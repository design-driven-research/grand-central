(ns rdd.grand-central.models.time
  (:require [datomic.client.api :as d]
            [rdd.grand-central.db.core :as db-core :refer [db]]
            [rdd.grand-central.validation.db-spec]
            [clojure.spec.alpha :as s]
            [spec-coerce.core :as sc]))

(defn times
  []
  (flatten (d/q '[:find (pull ?e [*])
                  :where [?e :time/interval _]]
                (db))))