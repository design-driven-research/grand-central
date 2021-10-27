(ns rdd.grand-central.models.process
  (:require [datomic.client.api :as d]
            [rdd.grand-central.db.core :as db-core :refer [db]]
            [rdd.grand-central.validation.db-spec]
            [clojure.spec.alpha :as s]
            [spec-coerce.core :as sc]))

(defn processes
  []
  (flatten (d/q '[:find (pull ?e [*
                                  {:measurement/uom [:uom/uuid]
                                   :process/labor [:labor/uuid]}])
                  :where [?e :process/uuid _]]
                (db))))

#_(tap> (processes))