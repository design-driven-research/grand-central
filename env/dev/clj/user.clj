(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require [clojure.pprint]
            [clojure.spec.alpha :as s]
            [datomic.api :as d]
            [expound.alpha :as expound]
            [mount.core :as mount]
            [rdd.grand-central.core]
            [rdd.grand-central.db.core :as db :refer [conn install-schema show-schema]]))

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

(defn start
  "Starts application.
  You'll usually want to run this on startup."
  []
  (mount/start-without #'rdd.grand-central.core/repl-server))

(defn stop
  "Stops application."
  []
  (mount/stop-except #'rdd.grand-central.core/repl-server))

(defn restart
  "Restarts application."
  []
  (stop)
  (start))

#_(install-schema conn)
#_(show-schema conn)
#_(tap> (show-schema conn))

;; Add UOMS
#_(d/transact db/conn [{:uom/name "Pound" :uom/code "lb" :uom/system :units.system/IMPERIAL :uom/type :units.type/WEIGHT :uom/factor 453.5920865}
                       {:uom/name "Gram" :uom/code "gram" :uom/system :units.system/METRIC :uom/type :units.type/WEIGHT :uom/factor 1.0}
                       {:uom/name "Ounce" :uom/code "oz" :uom/system :units.system/IMPERIAL :uom/type :units.type/WEIGHT :uom/factor 28.34949978}
                       {:uom/name "Kilogram" :uom/code "kg" :uom/system :units.system/METRIC :uom/type :units.type/WEIGHT :uom/factor 1000.0}

                       {:uom/name "Gallon" :uom/code "gallon" :uom/system :units.system/IMPERIAL :uom/type :units.type/VOLUME :uom/factor 768.0019661}
                       {:uom/name "Fluid Ounce" :uom/code "floz" :uom/system :units.system/IMPERIAL :uom/type :units.type/VOLUME :uom/factor 5.999988}
                       {:uom/name "Tablespoon" :uom/code "tbs" :uom/system :units.system/IMPERIAL :uom/type :units.type/VOLUME :uom/factor 3.000003}
                       {:uom/name "Cup" :uom/code "cup" :uom/system :units.system/IMPERIAL :uom/type :units.type/VOLUME :uom/factor 48.0000768}
                       {:uom/name "Teaspoon" :uom/code "tsp" :uom/system :units.system/IMPERIAL :uom/type :units.type/VOLUME :uom/factor 1.0}

                       {:uom/name "Each" :uom/code "ea"  :uom/type :units.type/CUSTOM}])

#_(d/transact db/conn [;;  Nodes
                       {:db/id "salt" :node/name "Salt" :node/uom [:uom/code "gram"] :node/yield 1.0}
                       {:db/id "pepper" :node/name "Pepper" :node/uom [:uom/code "gram"] :node/yield 1.0}
                       {:db/id "paprika" :node/name "Paprika" :node/uom [:uom/code "gram"] :node/yield 1.0}
                       {:db/id "garlicpowder" :node/name "Garlic powder" :node/uom [:uom/code "gram"] :node/yield 1.0}

                       {:db/id "oilmix" :node/name "Oil mix" :node/uom [:uom/code "gram"] :node/yield 50.0}
                       {:db/id "pestosauce" :node/name "Pesto Sauce" :node/uom [:uom/code "gram"] :node/yield 1.0}
                       {:db/id "mastersauce" :node/name "Master Sauce" :node/uom [:uom/code "gram"] :node/yield 100.0}

                       {:db/id "chorizowrap" :node/name "Chorizo Wrap" :node/uom [:uom/code "gram"] :node/yield 1.0}


                       {:edge/child "salt" :edge/parent "oilmix" :node/_parents "salt" :node/_children "oilmix" :edge/quantity 10.0 :edge/uom [:uom/code "gram"]}
                       {:edge/child "pepper" :edge/parent "oilmix" :node/_parents "pepper" :node/_children "oilmix" :edge/quantity 10.0 :edge/uom [:uom/code "gram"]}
                       {:edge/child "paprika" :edge/parent "oilmix" :node/_parents "paprika" :node/_children "oilmix" :edge/quantity 10.0 :edge/uom [:uom/code "gram"]}
                       {:edge/child "garlicpowder" :edge/parent "oilmix" :node/_parents "garlicpowder" :node/_children "oilmix" :edge/quantity 10.0 :edge/uom [:uom/code "gram"]}

                       {:edge/child "oilmix" :edge/parent "pestosauce" :node/_parents "oilmix" :node/_children "pestosauce" :edge/quantity 10.0 :edge/uom [:uom/code "gram"]}
                       {:edge/child "pestosauce" :edge/parent "mastersauce" :node/_parents "pestosauce" :node/_children "mastersauce" :edge/quantity 10.0 :edge/uom [:uom/code "gram"]}
                       {:edge/child "mastersauce" :edge/parent "chorizowrap" :node/_parents "mastersauce" :node/_children "chorizowrap" :edge/quantity 10.0 :edge/uom [:uom/code "gram"]}


                       {:db/id "food" :category/name "Food"}
                       {:db/id "dry" :category/name "Dry" :category/parents #{"food"}}
                       {:category/name "Spice" :category/parents #{"dry"}}

                       {:cost/quantity 1.0
                        :cost/uom [:uom/code "lb"]
                        :cost/node "salt"
                        :cost/cost 10.0}

                       {:cost/quantity 1.0
                        :cost/uom [:uom/code "lb"]
                        :cost/node "pepper"
                        :cost/cost 10.0}

                       {:db/id "case" :uom/name "Case" :uom/code "cs" :uom/type :units.type/CUSTOM}
                       {:db/id "pallet" :uom/name "Pallet" :uom/code "pallet" :uom/type :units.type/CUSTOM}
                       {:db/id "wrap" :uom/name "Wrap" :uom/code "wrap" :uom/type :units.type/CUSTOM}


                       {:conversion/from "case"
                        :conversion/to [:uom/code "lb"]
                        :conversion/node "salt"
                        :conversion/factor 25.0}

                       {:conversion/from "case"
                        :conversion/to [:uom/code "lb"]
                        :conversion/node "pepper"
                        :conversion/factor 25.0}

                       {:conversion/from "pallet"
                        :conversion/to "case"
                        :conversion/node "pepper"
                        :conversion/factor 100.0}
  ;;  
                       ])


;; Fiddle

#_(d/q '[:find ?e ?name
         :where [?e :node/name ?name]]
       (d/db conn))

#_(d/q '[:find ?e ?name
         :where [?e :uom/name ?name]]
       (d/db conn))

#_(tap> (d/datoms (d/db conn) :aevt))