(ns rdd.grand-central.db.core
  (:require
   [mount.core :refer [defstate]]
   [clojure.edn :as edn]
   [datomic.client.api :as d]
   [nano-id.core :refer [nano-id]]
   [tick.core :as t]
   [tick.locale-en-us]
   [rdd.grand-central.utils.utils :refer [for-indexed spread-across-space]]
   [postmortem.core :as pm]
   [rdd.grand-central.config :refer [env]]))

(declare install-schema reset-db! *reset-db!)

(defonce db-conn (atom nil))

(defstate ^{:on-reload :noop} client
  :start (let [new-client (d/client {:server-type :dev-local
                                     :storage-dir :mem
                                     :system "dev"})]
           (*reset-db! new-client :seed-path "resources/seeds/simple.edn")
           new-client))

(defn load-schema-data
  [path]
  (-> (slurp path)
      read-string
      :schema))

(defn load-seed-data
  [path]
  (when path
    (try
      (-> (slurp path)
          edn/read-string)
      (catch Exception _
        (prn "Failed to load seed data")))))

(defn install-schema
  [conn path]
  (d/transact conn {:tx-data (load-schema-data path)})
  conn)

(defn show-schema
  "Show currently installed schema"
  [conn]
  (let [system-ns #{"db" "db.type" "db.install" "db.part"
                    "db.lang" "fressian" "db.unique" "db.excise"
                    "db.cardinality" "db.fn" "db.sys" "db.bootstrap"
                    "db.alter"}]
    (d/q '[:find ?ident
           :in $ ?system-ns
           :where
           [?e :db/ident ?ident]
           [(namespace ?ident) ?ns]
           [((comp not contains?) ?system-ns ?ns)]]
         (d/db conn) system-ns)))

(defn create-companies-seed-data
  [data]
  (for [{:keys [name]} (:companies data)]
    {:company/uuid (nano-id)
     :company/name name}))

(defn create-relationship
  [{:keys [name quantity uom temp-id]} position]
  {:db/id temp-id
   :recipe-line-item/uuid (nano-id)
   :meta/position position
   :recipe-line-item/item [:item/name name]
   :measurement/quantity quantity
   :measurement/uom [:uom/code uom]})

(defn create-items
  [data]
  (for [{:as item
         :keys [name yield uom]} (:items data)]
    (let [has-children? (:items item)
          production-type (if has-children? :production.type/COMPOSITE :production.type/ATOM)]
      {:item/uuid (nano-id)
       :item/production-type production-type
       :item/name name
       :measurement/uom [:uom/code uom]
       :measurement/yield yield})))

(defn create-uom-seed-data
  [data]
  (for [{:keys [name code type system]} (:uoms data)]
    {:uom/uuid (nano-id)
     :uom/type type
     :uom/system system
     :uom/name name
     :uom/code code}))

(defn create-conversion-seed-data
  [data]
  (for [{:keys [from to quantity]} (:conversions data)]
    {:conversion/uuid (nano-id)
     :conversion/from [:uom/code from]
     :conversion/to [:uom/code to]
     :measurement/quantity quantity}))

(defn date-str-to->inst
  [date]
  (t/inst (-> (t/date date)
              (t/at (t/midnight))
              (t/in "America/Los_Angeles"))))

(defn create-company-items-data
  [data]
  (let [payload (for [{:keys [item
                              company
                              sku
                              name
                              description
                              quotes
                              conversions]} (:company-items data)]


                  (let [company-item-temp-id (rand-int -10000000)
                        conversion-payloads (for [{:keys [from to quantity]} conversions]
                                              (let [conversion-temp-id (rand-int -10000000)]
                                                {:db/id conversion-temp-id
                                                 :conversion/uuid (nano-id)
                                                 :conversion/from [:uom/code from]
                                                 :conversion/to [:uom/code to]
                                                 :measurement/quantity quantity}))
                        conversion-ids (map :db/id conversion-payloads)

                        quote-payloads (for [{:keys [uom quantity cost valid-from valid-to]} quotes]
                                         (let [quote-temp-id (rand-int -10000000)
                                               valid-from (date-str-to->inst valid-from)
                                               valid-to (date-str-to->inst valid-to)]
                                           {:db/id quote-temp-id
                                            :quote/uuid (nano-id)
                                            :date/valid-from valid-from
                                            :date/valid-to valid-to
                                            :measurement/uom [:uom/code uom]
                                            :measurement/quantity quantity
                                            :currency.usd/cost cost}))
                        quote-ids (map :db/id quote-payloads)
                        company-item-payload {:db/id company-item-temp-id
                                              :company-item/uuid (nano-id)
                                              :company-item/sku sku
                                              :company-item/name name
                                              :info/description description
                                              :company-item/item [:item/name item]
                                              :company-item/quotes quote-ids
                                              :uom/conversions conversion-ids}
                        company-update-payload {:db/id [:company/name company]
                                                :company/company-items company-item-temp-id}]

                    [company-update-payload
                     company-item-payload
                     quote-payloads
                     conversion-payloads]))]

    (flatten payload)))

(defn create-recipes
  [data]

  (-> (for [{:keys [name items]} (:items data)]
        (let [;; Create and merge in temp ids into the children collection
              children-with-temp-ids (map #(assoc % :temp-id (rand-int -10000000)) items)

              ;; We need temp ids for the recipe line items so we can ref them in the parent item contains field
              temp-ids (map :temp-id children-with-temp-ids)

              ;; Create position spacing
              total-children (count children-with-temp-ids)
              child-positions (spread-across-space 9007199254740991 total-children)



              ;; Create the recipe line items recursively
              children (for-indexed [child idx children-with-temp-ids]
                                    (create-relationship child (nth child-positions idx)))

              ;; Update the parent with the recipe line items
              item-updates {:db/id [:item/name name] :composite/contains (vec temp-ids)}]

          ;; Final results
          [item-updates children]))
      flatten))

(defn- *reset-db!
  [client & {:keys [db-name schema-path seed-path]
             :or {db-name "rdd"
                  schema-path "resources/schema/core.edn"}}]
  (d/delete-database client {:db-name db-name})
  (d/create-database client {:db-name db-name})
  (tap> "Resetting")

  (let [conn (d/connect client {:db-name db-name})]
    (install-schema conn schema-path)
    (when-let [seed-data (load-seed-data seed-path)]
      (tap> "Installing seed data")
      (d/transact conn {:tx-data (create-companies-seed-data seed-data)})
      (d/transact conn {:tx-data (create-uom-seed-data seed-data)})
      (d/transact conn {:tx-data (create-items seed-data)})
      (d/transact conn {:tx-data (create-recipes seed-data)})
      (d/transact conn {:tx-data (create-conversion-seed-data seed-data)})
      (d/transact conn {:tx-data (create-company-items-data seed-data)}))
    (reset! db-conn conn)))

(defn reset-db!
  [& {:keys [seed?]
      :or {seed? true}}]
  (if seed?
    (*reset-db! client :seed-path "resources/seeds/simple.edn")
    (*reset-db! client)))



#_(reset-db!)
#_(reset-db! :seed? false)

#_(tap> (d/datoms (d/db @db-conn) {:index :eavt}))

#_(d/delete-database client {:db-name "rdd"})