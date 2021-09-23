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
            "resources/migrations/schema.edn"
            "resources/seeds/base.edn"
            c)))

#_(defstate conn
    :start (let [db (d/create-database client {:db-name "rdd"})
                 conn (d/connect client {:db-name "rdd"})]
             (install-schema conn  "resources/migrations/schema.edn")
             conn))

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

(defn add-uuid
  [m]
  (if (map? m)
    (assoc m :uuid (nano-id))
    m))

(defn- augment-with-uuids
  "Augment a map containing vectors with uuids for each key
   {:items [{:a 1}]} -> {:items [{:a 1 :uuid 'rGQA6dFAuLk-bP2XmxwIh'}]}
   "
  ([data] (augment-with-uuids data (keys data)))
  ([data keys]
   (if (empty? keys)
     data
     (let [key (first keys)
           augmented-key (or (map add-uuid (key data))
                             (key data))
           updated-data (assoc data key augmented-key)]
       (augment-with-uuids updated-data (rest keys))))))

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
  (for [{:keys [item price uom quantity]} (:costs data)]
    {:cost/uuid (nano-id)
     :currency.usd/cost price
     :for/item [:item/name item]
     :measurement/uom [:uom/code uom]
     :measurement/quantity quantity}))

#_(create-conversion-seed-data (load-seed-data "resources/seeds/base.edn"))

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
   "resources/migrations/schema.edn"
   "resources/seeds/base.edn"
   client))


(defn item->tree
  [name]
  (d/pull (d/db @db-conn) '[* {:measurement/uom [:uom/code] :composite/contains ...}] [:item/name name]))

#_(create-recipes (load-seed-data "resources/seeds/base.edn"))

#_(d/q '[:find ?name
         :where [_ :item/name ?name]]
       (d/db @db-conn))

#_(d/pull (d/db @db-conn) '[* {:composite/contains ...}] [:item/name "Chorizo Family Pack"])

#_(d/pull (d/db @db-conn) '[* {:measurement/uom [:uom/code] :composite/contains ...}] [:item/name "Chorizo Family Pack"])

#_(tap> (d/datoms (d/db @db-conn) {:index :eavt}))

#_(tap> (show-schema conn))

#_(defn show-transaction
    "Show all the transaction data
   e.g.
    (-> conn show-transaction count)
    => the number of transaction"
    [conn]
    (seq (d/tx-range (d/log conn) nil nil)))

#_(defn add-user
    "e.g.
    (add-user conn {:id \"aaa\"
                    :screen-name \"AAA\"
                    :status :user.status/active
                    :email \"aaa@example.com\" })"
    [conn {:keys [id screen-name status email]}]
    @(d/transact conn [{:user/id         id
                        :user/name       screen-name
                        :user/status     status
                        :user/email      email}]))

#_(defn find-one-by
    "Given db value and an (attr/val), return the user as EntityMap (datomic.query.EntityMap)
   If there is no result, return nil.

   e.g.
    (d/touch (find-one-by (d/db conn) :user/email \"user@example.com\"))
    => show all fields
    (:user/first-name (find-one-by (d/db conn) :user/email \"user@example.com\"))
    => show first-name field"
    [db attr val]
    (d/entity db
            ;;find Specifications using ':find ?a .' will return single scalar
              (d/q '[:find ?e .
                     :in $ ?attr ?val
                     :where [?e ?attr ?val]]
                   db attr val)))


#_(defn find-user [db id]
    (d/touch (find-one-by db :user/id id)))

#_(defn item->tree
    [product-name]
    (-> (neo/item->tree neo/session {:name product-name})
        first
        :value))
#_(defn node->tree
    [e]
    (let [has-child-nodes? (:node/children e)
          has-child-node? (:edge/child e)

          build-node-with-children (fn [node]
                                     (let [children (mapv node->tree (:node/children node))
                                           id (-> node :db/id)
                                           name (-> node :node/name)
                                           yield (-> node :node/yield)
                                           total-children-cost (->> children
                                                                    (map :total-cost)
                                                                    (reduce +))
                                           normalized-cost (/ total-children-cost yield)]
                                       {:id id
                                        :name name
                                        :yield yield
                                        :normalized-cost normalized-cost
                                        :children children}))

          build-edge (fn [edge]
                       (let [node (node->tree (:edge/child edge))
                             edge-id (:db/id edge)
                             quantity (:edge/quantity edge)
                             uom (-> edge :edge/uom :uom/code)
                             total-cost (* quantity (-> node :normalized-cost))]
                         (merge node {:quantity quantity
                                      :edge-id edge-id
                                      :total-cost total-cost
                                      :uom uom})))

          build-base-node (fn [node]
                            (let [id (-> node :db/id)
                                  name (-> node :node/name)
                                  yield (-> node :node/yield)
                                  uom (-> node :node/uom :uom/code)
                                  cost-per-yield 1
                                  normalized-cost (/ cost-per-yield yield)]
                              {:id id
                               :uom uom
                               :normalized-cost normalized-cost
                               :name name}))]

      (cond
        has-child-nodes? (build-node-with-children e)
        has-child-node? (build-edge e)
        :else (build-base-node e))))

#_(time (-> (d/entity (d/db conn) [:node/name "Chorizo Wrap"])
            node->tree))

#_(d/q '[:find ?e
         :keys id
         :where [?e :person/name "Bob"]]
       (d/db conn))
