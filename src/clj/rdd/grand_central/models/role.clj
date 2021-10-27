(ns rdd.grand-central.models.role
  (:require [datomic.client.api :as d]
            [rdd.grand-central.db.core :as db-core :refer [db]]
            [rdd.grand-central.validation.db-spec]
            [clojure.spec.alpha :as s]
            [spec-coerce.core :as sc]))

(defn roles
  []
  (flatten (d/q '[:find (pull ?e [*])
                  :where [?e :role/uuid _]]
                (db))))

