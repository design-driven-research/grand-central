(ns rdd.grand-central.models.labor
  (:require [datomic.client.api :as d]
            [rdd.grand-central.db.core :as db-core :refer [db]]
            [rdd.grand-central.validation.db-spec]))

(defn labor
  []
  (flatten (d/q '[:find (pull ?e [*
                                  {:labor/role [:role/uuid]}])
                  :where [?e :labor/uuid _]]
                (db))))

(defn labor-by-uuid
  [uuid]
  (flatten (d/q '[:find (pull ?e [*
                                  {:labor/role [:role/uuid]}])
                  :in $ ?uuid
                  :where [?e :labor/uuid ?uuid]]
                (db) uuid)))

(labor-by-uuid "BDi25fzfS1Fo5tCiAJNzJ")
;; => ({:db/id 92358976733372, :time/duration 25.0, :time/duration-interval #:db{:id 101155069755468, :ident :time.interval/MINUTE}, :labor/uuid "BDi25fzfS1Fo5tCiAJNzJ", :labor/role #:role{:uuid "Wg4BFXANzWwJP8OsyEIv_"}, :info/description "Slice onions"})

[:labor/uuid "BDi25fzfS1Fo5tCiAJNzJ"]


