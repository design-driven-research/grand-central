(ns rdd.grand-central.services.store
  (:require [datomic.client.api :as d]
            [rdd.grand-central.db.core :as db-core]))

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
                                     :composite/contains [:item/uuid]}])
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

(defn initial-data
  "Load initial data"
  []
  {:items (get-items)
   :recipe-line-items (get-recipe-line-items)
   :uoms (get-uoms)
   :companies (get-companies)
   :conversions (get-conversions)
   :company-items (get-company-items)
   :quotes (get-quotes)})

(defn item->tree
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

  (initial-data)

  (tap> (initial-data))

  ;; 
  )