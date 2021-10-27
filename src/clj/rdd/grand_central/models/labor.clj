(ns rdd.grand-central.models.labor
  (:require [datomic.client.api :as d]
            [rdd.grand-central.db.core :as db-core :refer [db]]
            [rdd.grand-central.validation.db-spec]
            [clojure.spec.alpha :as s]
            [spec-coerce.core :as sc]))

(defn labor
  []
  (flatten (d/q '[:find (pull ?e [*
                                  {:time/duration-interval [:time/interval]
                                   :labor/role [:role/uuid]}])
                  :where [?e :labor/uuid _]]
                (db))))