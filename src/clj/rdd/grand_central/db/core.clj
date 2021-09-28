(ns rdd.grand-central.db.core
  (:require
   [mount.core :refer [defstate]]
   [clojure.edn :as edn]
   [datomic.client.api :as d]
   [nano-id.core :refer [nano-id]]
   [postmortem.core :as pm]
   [rdd.grand-central.config :refer [env]]))

(declare install-schema reset-db! *reset-db!)

(defonce db-conn (atom nil))

(defstate client
  :start (let [c (d/client {:server-type :dev-local
                            :storage-dir :mem
                            :system "dev"})]
           (*reset-db!
            "rdd"
            "resources/schema/core.edn"
            "resources/seeds/base.edn"
            c)
           c))

(defn load-schema-data
  [path]
  (-> (slurp path)
      read-string
      :schema))

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
  [{:keys [name quantity uom temp-id]}]
  {:db/id temp-id
   :recipe-line-item/uuid (nano-id)
   :composite/contains [[:item/name name]]
   :measurement/quantity quantity
   :measurement/uom [:uom/code uom]})

(defn create-items
  [data]
  (for [{:keys [name yield uom]} (:items data)]
    {:item/uuid (nano-id)
     :item/name name
     :measurement/uom [:uom/code uom]
     :measurement/yield yield}))

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

(defn create-cost-seed-data
  [data]
  (for [{:keys [item cost company sku uom quantity]} (:costs data)]
    {:cost/uuid (nano-id)
     :currency.usd/cost cost
     :cost/sku sku
     :cost/company [:company/name company]
     :cost/item [:item/name item]
     :measurement/uom [:uom/code uom]
     :measurement/quantity quantity}))

(defn create-recipes
  [data]
  (-> (for [{:keys [name items]} (:items data)]
        (let [;; Create and merge in temp ids into the children collection
              children-with-temp-ids (map #(assoc % :temp-id (rand-int -10000000)) items)

              ;; We need temp ids for the recipe line items so we can ref them in the parent item contains field
              temp-ids (map :temp-id children-with-temp-ids)

              ;; Create the recipe line items recursively
              children (for [child children-with-temp-ids] (create-relationship child))

              ;; Update the parent with the recipe line items
              item-updates {:db/id [:item/name name] :composite/contains (vec temp-ids)}]

          ;; Final results
          [item-updates children]))
      flatten))

(defn load-seed-data
  [path]
  (-> (slurp path)
      edn/read-string))

(defn- *reset-db!
  [db-name schema-path seed-path client]
  (d/delete-database client {:db-name db-name})
  (d/create-database client {:db-name db-name})

  (let [seed-data (load-seed-data seed-path)
        conn (d/connect client {:db-name db-name})]
    (install-schema conn schema-path)
    (d/transact conn {:tx-data (create-companies-seed-data seed-data)})
    (d/transact conn {:tx-data (create-uom-seed-data seed-data)})
    (d/transact conn {:tx-data (create-items seed-data)})
    (d/transact conn {:tx-data (create-recipes seed-data)})
    (d/transact conn {:tx-data (create-conversion-seed-data seed-data)})
    (d/transact conn {:tx-data (create-cost-seed-data seed-data)})
    (reset! db-conn conn)))

(defn reset-db!
  []
  (*reset-db!
   "rdd"
   "resources/schema/core.edn"
   "resources/seeds/base.edn"
   client))

(defn item->tree
  [name]
  (d/pull (d/db @db-conn) '[* {:measurement/uom [:uom/code]
                               :cost/_item [:cost/uuid
                                            :measurement/quantity
                                            {:cost/item [:item/uuid]}
                                            {:measurement/uom [:uom/code]}]
                               :composite/contains ...}] [:item/name name]))

#_(reset-db!)

(item->tree "Chorizo Family Pack")
