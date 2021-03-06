(ns rdd.grand-central.db.core
  (:require [clojure.edn :as edn]
            [datomic.client.api :as d]
            [mount.core :refer [defstate]]
            [nano-id.core :refer [nano-id]]
            [rdd.grand-central.utils.utils :refer [for-indexed
                                                   spread-across-space]]
            [tick.core :as t]
            [tick.locale-en-us]))

(declare install-schema reset-db! *reset-db!)

(defonce db-conn (atom nil))

(defstate ^{:on-reload :noop} client
  :start (let [new-client (d/client {:server-type :dev-local
                                     :storage-dir :mem
                                     :system "dev"})]
           (*reset-db! new-client :seed-path "resources/seeds/seed.edn")
           new-client))

(defn db
  [& {:keys [conn]
      :or {conn @db-conn}}]
  (d/db conn))

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
  [{:keys [child position company-item-sku]}]
  (let [{:keys [name quantity uom temp-id]} child]
    (cond-> {:db/id temp-id
             :recipe-line-item/uuid (nano-id)
             :meta/position position
             :recipe-line-item/item [:item/name name]
             :measurement/quantity quantity
             :measurement/uom [:uom/code uom]}
      company-item-sku (assoc :recipe-line-item/company-item [:company-item/sku company-item-sku]))))

(defn create-items
  [data]
  (for [{:as item
         :keys [name yield uom]} (:items data)]
    (let [has-children? (:line-items item)
          production-type (if has-children? :production.type/COMPOSITE :production.type/ATOM)
          temp-db-id name]
      {:db/id temp-db-id
       :item/uuid (nano-id)
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

(defn create-role-seed-data
  [data]
  (for [{:keys [name cost duration duration-interval]} (:roles data)]
    {:role/uuid (nano-id)
     :role/name name
     :currency.usd/cost cost
     :time/duration duration
     :time/duration-interval duration-interval}))

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


                  (let [conversion-payloads (for [{:keys [from to quantity]} conversions]
                                              (let [conversion-temp-id (nano-id)]
                                                {:db/id conversion-temp-id
                                                 :conversion/uuid (nano-id)
                                                 :conversion/from [:uom/code from]
                                                 :conversion/to [:uom/code to]
                                                 :measurement/quantity quantity}))
                        conversion-ids (map :db/id conversion-payloads)

                        quote-payloads (for [{:keys [uom quantity cost valid-from valid-to]} quotes]
                                         (let [quote-temp-id (nano-id)
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
                        company-item-payload {:db/id sku
                                              :company-item/uuid (nano-id)
                                              :company-item/sku sku
                                              :company-item/name name
                                              :info/description description
                                              :company-item/item [:item/name item]
                                              :company-item/quotes quote-ids
                                              :uom/conversions conversion-ids}
                        company-update-payload {:db/id [:company/name company]
                                                :company/company-items sku}]

                    [company-update-payload
                     company-item-payload
                     quote-payloads
                     conversion-payloads]))]

    (flatten payload)))

(defn find-company-item-by-item-name
  [company-items item-name]
  (let [matches (filter #(= item-name (:item %)) company-items)]
    (first matches)))

(defn create-recipes
  [data]

  (let [company-items (:company-items data)
        items (:items data)]
    (-> (for [{:keys [name line-items]} items]
          (let [;; Create and merge in temp ids into the children collection
                children-with-temp-ids (map #(assoc % :temp-id (nano-id)) line-items)

              ;; We need temp ids for the recipe line items so we can ref them in the parent item contains field
                temp-ids (map :temp-id children-with-temp-ids)

              ;; Create position spacing
                total-children (count children-with-temp-ids)
                child-positions (spread-across-space 9007199254740991 total-children)

              ;; Create the recipe line items recursively
                children (for-indexed [child idx children-with-temp-ids]
                                      (let [child-item-name (:name child)
                                            default-company-item (find-company-item-by-item-name company-items child-item-name)
                                            default-company-item-sku (:sku default-company-item)]
                                        (create-relationship {:child child
                                                              :position (nth child-positions idx)
                                                              :company-item-sku default-company-item-sku})))

              ;; Update the parent with the recipe line items
                item-updates {:db/id [:item/name name] :composite/contains (vec temp-ids)}]

          ;; Final results
            [item-updates children]))
        flatten)))

(defn create-labor-tx-data
  [{:keys [role description duration duration-interval]}]
  (let [uuid (nano-id)
        temp-db-id uuid]
    {:db/id temp-db-id
     :labor/uuid uuid
     :labor/role [:role/name role]
     :info/description description
     :time/duration duration
     :time/duration-interval duration-interval}))

(defn create-item-processes
  [data]

  (let [items (:items data)
        items-with-process (filter :process items)]
    (-> (for [{:keys [name process]} items-with-process]
          (let [process-uuid (nano-id)
                process-temp-db-id process-uuid
                quantity (:quantity process)
                uom (:uom process)

                labor-tx-data (map create-labor-tx-data (:labor process))

                ;; Temp db ids for use in process
                labor-ids (map :db/id labor-tx-data)

                process-tx-data {:db/id process-temp-db-id
                                 :process/uuid process-uuid
                                 :measurement/quantity quantity
                                 :measurement/uom [:uom/code uom]
                                 :process/labor labor-ids}

              ;; Update the parent with the recipe line items
                item-tx-data {:db/id [:item/name name] :item/process process-temp-db-id}]

          ;; Final results
            [process-tx-data labor-tx-data item-tx-data]))
        flatten)))

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

      (d/transact conn {:tx-data (create-role-seed-data seed-data)})

      (d/transact conn {:tx-data (create-items seed-data)})

      (d/transact conn {:tx-data (create-conversion-seed-data seed-data)})
      (d/transact conn {:tx-data (create-company-items-data seed-data)})
      (d/transact conn {:tx-data (create-recipes seed-data)})

      (d/transact conn {:tx-data (create-item-processes seed-data)}))
    (reset! db-conn conn)))


#_(tap> (create-item-processes (load-seed-data "resources/seeds/seed.edn")))

#_(d/transact @db-conn {:tx-data (create-item-processes (load-seed-data "resources/seeds/seed.edn"))})

(defn reset-db!
  [& {:keys [seed?]
      :or {seed? true}}]
  (if seed?
    (*reset-db! client :seed-path "resources/seeds/seed.edn")
    (*reset-db! client)))

#_(reset-db!)
#_(reset-db! :seed? false)

#_(tap> (d/datoms (d/db @db-conn) {:index :eavt}))

#_(d/delete-database client {:db-name "rdd"})




#_(d/transact @db-conn {:tx-data [[:db/add -1 :company-item/uuid "vIM73g-llu-K6gYOBYgtK"]
                                  [:db/add -1 :company-item/name "asdf"]
                                  [:db/add -1 :company-item/sku "asdf"]
                                  [:db/add -1 :company-item/item [:item/uuid "6ri3D10jZJgkBEGmqTrYr"]]
                                  [:db/add -1 :company-item/quotes -2]
                                  [:db/add
                                   [:company/uuid "CppigamwVXerGpk2iYctA"]
                                   :company/company-items
                                   -1]
                                  [:db/add -2 :quote/uuid "YudBHqXUvvcA6brhPb8dd"]
                                  [:db/add -2 :currency.usd/cost 3]
                                  [:db/add -2 :measurement/quantity 3]
                                  [:db/add -2 :measurement/uom [:uom/uuid "86ExwnpDjvmBP8yN4HCL7"]]]})

#_(d/transact
   @db-conn
   {:tx-data
    [[:db/add [:recipe-line-item/uuid "-dwpppysMp8UchURG_AP0"] :recipe-line-item/company-item [:company-item/uuid "IQ1_T1dH6Uj3lsc4glpa9"]]]})


