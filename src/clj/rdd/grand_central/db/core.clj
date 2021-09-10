(ns rdd.grand-central.db.core
  (:require
   [datomic.api :as d]
   [io.rkn.conformity :as c]
   [mount.core :refer [defstate]]
   [rdd.grand-central.config :refer [env]]))

(defstate conn
  :start (do (-> env :database-url d/create-database) (-> env :database-url d/connect))
  :stop (-> conn .release))

(defn install-schema
  "This function expected to be called at system start up.

  Datomic schema migrations or db preinstalled data can be put into 'migrations/schema.edn'
  Every txes will be executed exactly once no matter how many times system restart."
  [conn]
  (let [norms-map (c/read-resource "migrations/schema.edn")]
    (c/ensure-conforms conn norms-map (keys norms-map))))

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

(defn show-transaction
  "Show all the transaction data
   e.g.
    (-> conn show-transaction count)
    => the number of transaction"
  [conn]
  (seq (d/tx-range (d/log conn) nil nil)))

(defn add-user
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

(defn find-one-by
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


(defn find-user [db id]
  (d/touch (find-one-by db :user/id id)))

(defn load-initial!
  []
  (d/q '[:find ?e
         :where [?e]]
       (d/db conn)))

(defn node->tree
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

;; => [[{:db/id 17592186045442,
;;       :node/name "Oil mix",
;;       :node/yield 50.0,
;;       :node/children
;;       [{:db/id 17592186045446,
;;         :edge/parent #:db{:id 17592186045442},
;;         :edge/child #:db{:id 17592186045438},
;;         :edge/uom #:db{:id 17592186045428},
;;         :edge/quantity 10.0}
;;        {:db/id 17592186045447,
;;         :edge/parent #:db{:id 17592186045442},
;;         :edge/child #:db{:id 17592186045439},
;;         :edge/uom #:db{:id 17592186045428},
;;         :edge/quantity 10.0}
;;        {:db/id 17592186045448,
;;         :edge/parent #:db{:id 17592186045442},
;;         :edge/child #:db{:id 17592186045440},
;;         :edge/uom #:db{:id 17592186045428},
;;         :edge/quantity 10.0}
;;        {:db/id 17592186045449,
;;         :edge/parent #:db{:id 17592186045442},
;;         :edge/child #:db{:id 17592186045441},
;;         :edge/uom #:db{:id 17592186045428},
;;         :edge/quantity 10.0}],
;;       :node/parents
;;       [{:db/id 17592186045450,
;;         :edge/parent #:db{:id 17592186045443},
;;         :edge/child #:db{:id 17592186045442},
;;         :edge/uom #:db{:id 17592186045428},
;;         :edge/quantity 10.0}],
;;       :node/uom #:db{:id 17592186045428}}]]


