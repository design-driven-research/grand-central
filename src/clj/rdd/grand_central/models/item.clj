(ns rdd.grand-central.models.item
  (:require [datomic.client.api :as d]
            [rdd.grand-central.db.core :as db-core :refer [db]]
            [rdd.grand-central.validation.db-spec]
            [clojure.spec.alpha :as s]
            [spec-coerce.core :as sc]))


(defn get-item-by-name
  [name]
  (ffirst (d/q '[:find (pull ?item [*])
                 :in $ ?name
                 :where
                 [?item :item/name ?name]]
               (db) name)))

#_(tap> (get-item-by-name "Sauce"))