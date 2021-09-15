(ns rdd.grand-central.db.core
  (:require
   [datomic.client.api :as d]
   [rdd.grand-central.db.neo4j :as neo]
   #_[io.rkn.conformity :as c]
   [mount.core :refer [defstate]]
   [rdd.grand-central.config :refer [env]]))

(declare install-schema)

(defstate client
  :start (d/client {:server-type :dev-local
                    :storage-dir :mem
                    :system "dev"}))

(defstate conn
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
  (d/transact conn {:tx-data (load-schema-data path)}))

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

(defn load-initial!
  []
  (neo/item->tree neo/session {:name "Chorizo Family Pack"}))
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
