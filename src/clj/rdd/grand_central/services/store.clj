(ns rdd.grand-central.services.store
  (:require [datomic.client.api :as d]
            [rdd.grand-central.db.core :as db-core]
            [rdd.grand-central.models.item :as item]
            [rdd.grand-central.models.labor :as labor]
            [rdd.grand-central.models.process :as process]
            [rdd.grand-central.models.role :as role]
            [rdd.grand-central.validation.db-spec]
            [spec-coerce.core :as sc]))

(defn conn
  []
  @db-core/db-conn)

(defn db
  []
  (d/db (conn)))

(defn transact
  [tx-data]
  (d/transact (conn) {:tx-data tx-data}))

(defn update-recipe-line-item-quantity
  [uuid quantity]
  (transact [[:db/add [:recipe-line-item/uuid uuid] :measurement/quantity (double quantity)]]))

(defn update-recipe-line-item
  [{:keys [uuid quantity uom]}]
  (let [tx-data (cond-> []
                  quantity (conj [:db/add [:recipe-line-item/uuid uuid] :measurement/quantity (double quantity)])
                  uom (conj [:db/add [:recipe-line-item/uuid uuid] :measurement/uom [:uom/code uom]]))]
    (transact tx-data)))

(defn get-items
  []
  (flatten (d/q '[:find (pull ?eid [*
                                    {:measurement/uom [:uom/uuid]
                                     :composite/contains [:recipe-line-item/uuid]}])
                  :where
                  [?eid :item/uuid ?uuid]]
                (db))))

(defn get-recipe-line-items
  []
  (flatten (d/q '[:find (pull ?eid [*
                                    {:measurement/uom [:uom/uuid]
                                     :recipe-line-item/item [:item/uuid]
                                     :recipe-line-item/company-item [:company-item/uuid]}])
                  :where
                  [?eid :recipe-line-item/uuid ?uuid]]
                (db))))

(defn get-uoms
  []
  (flatten (d/q '[:find (pull ?eid [*])
                  :where
                  [?eid :uom/uuid ?uuid]]
                (db))))

(defn get-companies
  []
  (flatten (d/q '[:find (pull ?eid [* {:company/company-items [:company-item/uuid]}])
                  :where
                  [?eid :company/uuid ?uuid]]
                (db))))

(defn get-conversions
  []
  (flatten (d/q '[:find (pull ?eid [* {:conversion/from [:uom/uuid]} {:conversion/to [:uom/uuid]}])
                  :where
                  [?eid :conversion/uuid ?uuid]]
                (db))))

(defn get-company-items
  []
  (flatten (d/q '[:find (pull ?eid [*
                                    {:company-item/item [:item/uuid]}
                                    {:company-item/quotes [:quote/uuid]}
                                    {:uom/conversions [:conversion/uuid]}])
                  :where
                  [?eid :company-item/uuid ?uuid]]
                (db))))

(defn get-quotes
  []
  (flatten (d/q '[:find (pull ?eid [* {:measurement/uom [:uom/uuid]}])
                  :where
                  [?eid :quote/uuid ?uuid]]
                (db))))

(defn coerce
  "Coerce a datom based on a matching global spec."
  [datom]
  (let [[db-fn eid attr value] datom
        coerced (sc/coerce attr value)]

    (case db-fn
      :db/add [db-fn eid attr coerced]
      :db/retract (if coerced
                    [db-fn eid attr coerced]
                    [db-fn eid attr])
      :db/retractEntity [db-fn eid])))

(defn transact-from-remote!
  "Process a remote transaction"
  [tx-data]
  (tap> tx-data)
  (let [coerced-tx-data (map coerce tx-data)]
    (d/transact (conn) {:tx-data coerced-tx-data})))

#_(d/transact (conn) {:tx-data [[:db/add [:process/uuid "L4dsfDL6CXnKixTMMmER6"] :measurement/quantity 2.0]]})
#_(d/transact (conn) {:tx-data [[:db/add [:labor/uuid "BDi25fzfS1Fo5tCiAJNzJ"] :time/duration 20.2]]})

#_(coerce [:db/add [:process/uuid "L4dsfDL6CXnKixTMMmER6"] :measurement/quantity 2])

#_(transact-from-remote! [[:db/add [:process/uuid "L4dsfDL6CXnKixTMMmER6"] :measurement/uom [:uom/uuid "aeMR7eBO18F8olSmCJGsW"]]])

(defn initial-data
  "Load initial data"
  []
  {:items (item/items)
   :recipe-line-items (get-recipe-line-items)
   :uoms (get-uoms)
   :roles (role/roles)
   :companies (get-companies)
   :conversions (get-conversions)
   :company-items (get-company-items)
   :quotes (get-quotes)
   :processes (process/processes)
   :labor (labor/labor)})

#_(defn item->tree
    [name]
    (d/pull (db) '[* {:measurement/uom [:uom/code]
                      :cost/_item [:cost/uuid
                                   :measurement/quantity
                                   {:cost/item [:item/uuid]}
                                   {:measurement/uom [:uom/code]}]
                      :composite/contains ...}] [:item/name name]))

(comment
  (get-items)
  (get-recipe-line-items)


  (get-uoms)
  (get-companies)
  (get-conversions)
  (get-quotes)
  (get-company-items)

  (initial-data)

  (tap> (initial-data))

  ;; 
  )