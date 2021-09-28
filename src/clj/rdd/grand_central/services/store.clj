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
  (d/q '[:find ?name ?uuid ?uom-code ?yield
         :keys :name :uuid :uom :yield
         :where [?eid :item/uuid ?uuid]
         [?eid :item/name ?name]
         [?eid :measurement/uom ?uom]
         [?uom :uom/code ?uom-code]

         [?eid :measurement/yield ?yield]]
       (db)))

(defn get-recipe-line-items
  []
  (d/q '[:find ?uuid ?uom-code ?quantity ?child-name ?child-uuid ?parent-name ?parent-uuid
         :keys :uuid :uom :quantity :child-name :child-uuid :parent-name :parent-uuid
         :where [?eid :recipe-line-item/uuid ?uuid]

         [?eid :measurement/uom ?uom]
         [?uom :uom/code ?uom-code]

         [?eid :composite/contains ?children]
         [?children :item/uuid ?child-uuid]
         [?children :item/name ?child-name]

         [?parent-id :composite/contains ?eid]
         [?parent-id :item/uuid ?parent-uuid]
         [?parent-id :item/name ?parent-name]

         [?eid :measurement/quantity ?quantity]]
       (db)))

(defn get-uoms
  []
  (d/q '[:find ?uuid ?name ?code ?type-ident ?system-ident
         :keys :uuid :name :code :type :system
         :where
         [?eid :uom/uuid ?uuid]
         [?eid :uom/name ?name]
         [?eid :uom/code ?code]
         [?eid :uom/type ?type]
         [?type :db/ident ?type-ident]

         [?eid :uom/system ?system]
         [?system :db/ident ?system-ident]]
       (db)))

(defn get-companies
  []
  (d/q '[:find ?uuid ?name
         :keys :uuid :name
         :where
         [?eid :company/uuid ?uuid]
         [?eid :company/name ?name]]
       (db)))

(defn get-conversions
  []
  (d/q '[:find ?uuid ?from ?to ?quantity
         :keys :uuid :from :to :quantity
         :where
         [?eid :conversion/uuid ?uuid]
         [?eid :conversion/from ?from-uom]
         [?eid :conversion/to ?to-uom]
         [?eid :measurement/quantity ?quantity]

         [?from-uom :uom/code ?from]
         [?to-uom :uom/code ?to]]
       (db)))

(defn get-costs
  []
  (d/q '[:find ?uuid ?item-uuid ?item-name ?company-uuid ?company-name ?sku ?cost ?quantity ?uom-code
         :keys :uuid :item-uuid :item-name :company-uuid :company-name :sku :cost :quantity :uom
         :where
         [?eid :cost/uuid ?uuid]
         [?eid :cost/sku ?sku]
         [?eid :currency.usd/cost ?cost]

         ;;  Item
         [?eid :cost/item ?item]
         [?item :item/uuid ?item-uuid]
         [?item :item/name ?item-name]

        ;;  Company
         [?eid :cost/company ?company]
         [?company :company/uuid ?company-uuid]
         [?company :company/name ?company-name]

        ;;  UOM
         [?eid :measurement/quantity ?quantity]
         [?eid :measurement/uom ?uom]
         [?uom :uom/code ?uom-code]]
       (db)))

(defn initial-data
  "Load initial data"
  []
  {:items (get-items)
   :recipe-line-items (get-recipe-line-items)
   :uoms (get-uoms)
   :companies (get-companies)
   :conversions (get-conversions)
   :costs (get-costs)})

(comment

  (get-items)
  (get-recipe-line-items)
  (get-uoms)
  (get-companies)
  (get-conversions)
  (get-costs)

  (initial-data)

  (get-uoms)
  ;; => [{:uuid "F8N7JpdLYpkuw5HnqYX9p", :name "Gram", :code "gr", :type :units.type/WEIGHT, :system :units.system/METRIC}
  ;;     {:uuid "OorDrZiU72I-AtkEaXImM", :name "Kilogram", :code "kg", :type :units.type/WEIGHT, :system :units.system/METRIC}
  ;;     {:uuid "qGVcC7Ex1eBfDlCSqy482", :name "Each", :code "ea", :type :units.type/CUSTOM, :system :units.system/CUSTOM}
  ;;     {:uuid "MYRxaMZcx_DSWkilLvXCd", :name "Pound", :code "lb", :type :units.type/WEIGHT, :system :units.system/IMPERIAL}]

;; => [{:uuid "qGVcC7Ex1eBfDlCSqy482", :name "Each", :code "ea", :type 101155069755470, :system 101155069755467}
;;     {:uuid "OorDrZiU72I-AtkEaXImM", :name "Kilogram", :code "kg", :type 101155069755468, :system 101155069755466}
;;     {:uuid "MYRxaMZcx_DSWkilLvXCd", :name "Pound", :code "lb", :type 101155069755468, :system 101155069755465}
;;     {:uuid "F8N7JpdLYpkuw5HnqYX9p", :name "Gram", :code "gr", :type 101155069755468, :system 101155069755466}]

  ;; 
  )