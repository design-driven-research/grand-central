(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require [clojure.pprint]
            [clojure.spec.alpha :as s]
            [datomic.client.api :as d]
            [rdd.grand-central.db.core :as db]
            [expound.alpha :as expound]
            [mount.core :as mount]
            [rdd.grand-central.core]))


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

(defn reset-db!
  "Reset the db and install seed data."
  []
  (db/reset-db!))

#_(tap> (show-schema conn))

;; Add UOMS
#_(d/transact db/conn {:tx-data [{:uom/name "Pound" :uom/code "lb" :uom/system :units.system/IMPERIAL :uom/type :units.type/WEIGHT :uom/factor 453.5920865}
                                 {:uom/name "Gram" :uom/code "gram" :uom/system :units.system/METRIC :uom/type :units.type/WEIGHT :uom/factor 1.0}
                                 {:uom/name "Ounce" :uom/code "oz" :uom/system :units.system/IMPERIAL :uom/type :units.type/WEIGHT :uom/factor 28.34949978}
                                 {:uom/name "Kilogram" :uom/code "kg" :uom/system :units.system/METRIC :uom/type :units.type/WEIGHT :uom/factor 1000.0}

                                 {:uom/name "Gallon" :uom/code "gallon" :uom/system :units.system/IMPERIAL :uom/type :units.type/VOLUME :uom/factor 768.0019661}
                                 {:uom/name "Fluid Ounce" :uom/code "floz" :uom/system :units.system/IMPERIAL :uom/type :units.type/VOLUME :uom/factor 5.999988}
                                 {:uom/name "Tablespoon" :uom/code "tbs" :uom/system :units.system/IMPERIAL :uom/type :units.type/VOLUME :uom/factor 3.000003}
                                 {:uom/name "Cup" :uom/code "cup" :uom/system :units.system/IMPERIAL :uom/type :units.type/VOLUME :uom/factor 48.0000768}
                                 {:uom/name "Teaspoon" :uom/code "tsp" :uom/system :units.system/IMPERIAL :uom/type :units.type/VOLUME :uom/factor 1.0}

                                 {:uom/name "Each" :uom/code "ea"  :uom/type :units.type/CUSTOM}]})

#_(d/transact db/conn {:tx-data [;;  Nodes
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
                                 ]})


;; Fiddle

#_(time (d/q '[:find ?e
               :in $ ?name
               :where
               [?e :node/name ?name]
               [?ident :db/ident]]
             (d/db conn) "Chorizo Wrap"))

#_(def rules '[[(values [?node] ?edge)
                [?node :node/children ?edge]
                (values ?edge ?node)]

               [(values [?edge] ?node)
                [?edge :edge/child ?subnode]
                (values ?subnode ?edge)]

               #_[(values [?node] ?edge)
                  [(ground "no edges") ?edge]
                  [(ground "no subnodes") ?node]]

               #_[(values ?node ?edge ?subnode)
                  [(ground "no edges") ?edge]
                  [(ground "no nodes") ?node]
                  [(ground "no subnodes") ?subnode]]

               #_[(edge ?edge ?node)
                  [?edge :edge/child ?node]
                  (values ?node _ _)


                  #_(values ?subnode ?ident ?attr ?val ?other)]

               #_[(values ?e ?ident ?attr ?val ?subnode)
                  [?attr :db/ident ?ident]
                  [?e ?ident ?val]
                  [(ground "NOTHING") ?subnode]
                  #_[?e :node/children ?subnode]]])

#_(->> (d/q '[:find ?node ?edge ?subnode
              :in $ %
              :where
              [?node :node/name "Chorizo Wrap"]
              (values ?node ?edge ?subnode)]

            (d/db conn) rules)
       first
       #_(map (fn [id] (d/pull (d/db conn) [:node/name :db/id] id))))
;; => [74766790688889 "no edges" "no subnodes"]

;; => ({:db/id 74766790688889, :node/name "Chorizo Wrap"}
;;     {:db/id 74766790688896,
;;      :edge/parent #:db{:id 74766790688889},
;;      :edge/child #:db{:id 74766790688888},
;;      :edge/uom #:db{:id 83562883711081},
;;      :edge/quantity 10.0,
;;      :node/_children ...,
;;      :node/_parents ...}
;;     {:db/id 74766790688888, :node/name "Master Sauce"})

;; => ({:db/id 74766790688889, :node/name "Chorizo Wrap"}
;;     {:db/id 74766790688896,
;;      :edge/parent #:db{:id 74766790688889},
;;      :edge/child #:db{:id 74766790688888},
;;      :edge/uom #:db{:id 83562883711081},
;;      :edge/quantity 10.0,
;;      :node/_children ...,
;;      :node/_parents ...}
;;     {:db/id 74766790688888, :node/name "Master Sauce"})

;; => (#:node{:name "Chorizo Wrap"} nil #:node{:name "Master Sauce"})

;; => ({:db/id 74766790688889,
;;      :node/name "Chorizo Wrap",
;;      :node/yield 1.0,
;;      :node/children
;;      [{:db/id 74766790688896,
;;        :edge/parent #:db{:id 74766790688889},
;;        :edge/child #:db{:id 74766790688888},
;;        :edge/uom #:db{:id 83562883711081},
;;        :edge/quantity 10.0}],
;;      :node/uom #:db{:id 83562883711081},
;;      :edge/_parent ...}
;;     {:db/id 74766790688896,
;;      :edge/parent #:db{:id 74766790688889},
;;      :edge/child #:db{:id 74766790688888},
;;      :edge/uom #:db{:id 83562883711081},
;;      :edge/quantity 10.0,
;;      :node/_children ...,
;;      :node/_parents ...}
;;     {:db/id 74766790688888,
;;      :node/name "Master Sauce",
;;      :node/yield 100.0,
;;      :node/children
;;      [{:db/id 74766790688895,
;;        :edge/parent #:db{:id 74766790688888},
;;        :edge/child #:db{:id 74766790688887},
;;        :edge/uom #:db{:id 83562883711081},
;;        :edge/quantity 10.0}],
;;      :node/parents
;;      [{:db/id 74766790688896,
;;        :edge/parent #:db{:id 74766790688889},
;;        :edge/child #:db{:id 74766790688888},
;;        :edge/uom #:db{:id 83562883711081},
;;        :edge/quantity 10.0}],
;;      :node/uom #:db{:id 83562883711081},
;;      :edge/_parent ...,
;;      :edge/_child ...})

;; => (#:node{:name "Chorizo Wrap"} nil #:node{:name "Master Sauce"})

;; => [74766790688889 "NOTHING" "NOTHING"]

;; => (#:node{:name "Chorizo Wrap"} nil nil)

;; => ({:db/id 74766790688889,
;;      :node/name "Chorizo Wrap",
;;      :node/yield 1.0,
;;      :node/children
;;      [{:db/id 74766790688896,
;;        :edge/parent #:db{:id 74766790688889},
;;        :edge/child #:db{:id 74766790688888},
;;        :edge/uom #:db{:id 83562883711081},
;;        :edge/quantity 10.0}],
;;      :node/uom #:db{:id 83562883711081},
;;      :edge/_parent ...}
;;     #:db{:id nil}
;;     #:db{:id nil})

;; => ({:db/id 74766790688889,
;;      :node/name "Chorizo Wrap",
;;      :node/yield 1.0,
;;      :node/children
;;      [{:db/id 74766790688896,
;;        :edge/parent #:db{:id 74766790688889},
;;        :edge/child #:db{:id 74766790688888},
;;        :edge/uom #:db{:id 83562883711081},
;;        :edge/quantity 10.0}],
;;      :node/uom #:db{:id 83562883711081},
;;      :edge/_parent ...}
;;     {:db/id 74766790688896,
;;      :edge/parent #:db{:id 74766790688889},
;;      :edge/child #:db{:id 74766790688888},
;;      :edge/uom #:db{:id 83562883711081},
;;      :edge/quantity 10.0,
;;      :node/_children ...,
;;      :node/_parents ...}
;;     {:db/id 74766790688888,
;;      :node/name "Master Sauce",
;;      :node/yield 100.0,
;;      :node/children
;;      [{:db/id 74766790688895,
;;        :edge/parent #:db{:id 74766790688888},
;;        :edge/child #:db{:id 74766790688887},
;;        :edge/uom #:db{:id 83562883711081},
;;        :edge/quantity 10.0}],
;;      :node/parents
;;      [{:db/id 74766790688896,
;;        :edge/parent #:db{:id 74766790688889},
;;        :edge/child #:db{:id 74766790688888},
;;        :edge/uom #:db{:id 83562883711081},
;;        :edge/quantity 10.0}],
;;      :node/uom #:db{:id 83562883711081},
;;      :edge/_parent ...,
;;      :edge/_child ...})

;; => Execution error (IndexOutOfBoundsException) at datomic.core.datalog/join-project-coll-with$project (datalog.clj:226).
;;    null

;; => ({:db/id 74766790688889,
;;      :node/name "Chorizo Wrap",
;;      :node/yield 1.0,
;;      :node/children
;;      [{:db/id 74766790688896,
;;        :edge/parent #:db{:id 74766790688889},
;;        :edge/child #:db{:id 74766790688888},
;;        :edge/uom #:db{:id 83562883711081},
;;        :edge/quantity 10.0}],
;;      :node/uom #:db{:id 83562883711081},
;;      :edge/_parent ...}
;;     {:db/id 74766790688888,
;;      :node/name "Master Sauce",
;;      :node/yield 100.0,
;;      :node/children
;;      [{:db/id 74766790688895,
;;        :edge/parent #:db{:id 74766790688888},
;;        :edge/child #:db{:id 74766790688887},
;;        :edge/uom #:db{:id 83562883711081},
;;        :edge/quantity 10.0}],
;;      :node/parents
;;      [{:db/id 74766790688896,
;;        :edge/parent #:db{:id 74766790688889},
;;        :edge/child #:db{:id 74766790688888},
;;        :edge/uom #:db{:id 83562883711081},
;;        :edge/quantity 10.0}],
;;      :node/uom #:db{:id 83562883711081},
;;      :edge/_parent ...,
;;      :edge/_child ...})


#_(d/pull (d/db conn) [:db/id] 74766790688889)
;; => [{:id 74766790688889, :key :node/name, :attr 76, :val "Chorizo Wrap", :sub "NOTHING"}
;;     {:id 74766790688889, :key :node/children, :attr 79, :val 74766790688896, :sub "NOTHING"}
;;     {:id 74766790688889, :key :node/yield, :attr 78, :val 1.0, :sub "NOTHING"}
;;     {:id 74766790688889, :key :node/yield, :attr 78, :val 1.0, :sub 74766790688888}
;;     {:id 74766790688889, :key :node/uom, :attr 81, :val 83562883711081, :sub 74766790688888}
;;     {:id 74766790688889, :key :node/uom, :attr 81, :val 83562883711081, :sub "NOTHING"}
;;     {:id 74766790688889, :key :node/children, :attr 79, :val 74766790688896, :sub 74766790688888}
;;     {:id 74766790688889, :key :node/name, :attr 76, :val "Chorizo Wrap", :sub 74766790688888}]

;; => [{:id 74766790688889, :key :node/name, :attr 76, :val "Chorizo Wrap", :sub "NOTHING"}
;;     {:id 74766790688889, :key :node/children, :attr 79, :val 74766790688896, :sub "NOTHING"}
;;     {:id 74766790688889, :key :node/yield, :attr 78, :val 1.0, :sub "NOTHING"}
;;     {:id 74766790688889, :key :node/yield, :attr 78, :val 1.0, :sub 74766790688888}
;;     {:id 74766790688889, :key :node/uom, :attr 81, :val 83562883711081, :sub 74766790688888}
;;     {:id 74766790688889, :key :node/uom, :attr 81, :val 83562883711081, :sub "NOTHING"}
;;     {:id 74766790688889, :key :node/children, :attr 79, :val 74766790688896, :sub 74766790688888}
;;     {:id 74766790688889, :key :node/name, :attr 76, :val "Chorizo Wrap", :sub 74766790688888}]

;; => [{:id 74766790688889, :key :node/children, :attr 79, :val 74766790688896, :sub "NOTHING"}
;;     {:id 74766790688889, :key :node/name, :attr 76, :val "Chorizo Wrap", :sub "NOTHING"}
;;     {:id 74766790688889, :key :node/yield, :attr 78, :val 1.0, :sub "NOTHING"}
;;     {:id 74766790688889, :key :node/yield, :attr 78, :val 1.0, :sub 74766790688888}
;;     {:id 74766790688889, :key :node/uom, :attr 81, :val 83562883711081, :sub 74766790688888}
;;     {:id 74766790688889, :key :node/uom, :attr 81, :val 83562883711081, :sub "NOTHING"}
;;     {:id 74766790688889, :key :node/children, :attr 79, :val 74766790688896, :sub 74766790688888}
;;     {:id 74766790688889, :key :node/name, :attr 76, :val "Chorizo Wrap", :sub 74766790688888}]

;; => [{:id 74766790688889, :key :node/yield, :attr 78, :val 1.0, :sub 74766790688888}
;;     {:id 74766790688889, :key :node/uom, :attr 81, :val 83562883711081, :sub 74766790688888}
;;     {:id 74766790688889, :key :node/children, :attr 79, :val 74766790688896, :sub 74766790688888}
;;     {:id 74766790688889, :key :node/name, :attr 76, :val "Chorizo Wrap", :sub 74766790688888}]

;; => [{:id 74766790688889, :key :node/uom, :attr 81, :val 83562883711081, :sub 74766790688896}
;;     {:id 74766790688889, :key :node/children, :attr 79, :val 74766790688896, :sub 74766790688896}
;;     {:id 74766790688889, :key :node/name, :attr 76, :val "Chorizo Wrap", :sub 74766790688896}
;;     {:id 74766790688889, :key :node/yield, :attr 78, :val 1.0, :sub 74766790688896}]




;; => [{:id 74766790688889, :key :node/uom, :attr 81, :val 83562883711081, :edge 74766790688896}
;;     {:id 74766790688889, :key :node/children, :attr 79, :val 74766790688896, :edge 74766790688896}
;;     {:id 74766790688889, :key :node/name, :attr 76, :val "Chorizo Wrap", :edge 74766790688896}
;;     {:id 74766790688889, :key :node/yield, :attr 78, :val 1.0, :edge 74766790688896}]

;; => [{:id 74766790688889, :key :node/name, :attr 76, :val "Chorizo Wrap"}
;;     {:id 74766790688889, :key :node/uom, :attr 81, :val 83562883711081}
;;     {:id 74766790688889, :key :node/children, :attr 79, :val 74766790688896}
;;     {:id 74766790688889, :key :node/yield, :attr 78, :val 1.0}]

;; => [{:id 74766790688889, :key :node/name, :attr 76, :val "Chorizo Wrap"}
;;     {:id 74766790688889, :key :node/uom, :attr 81, :val 83562883711081}
;;     {:id 74766790688889, :key :node/children, :attr 79, :val 74766790688896}
;;     {:id 74766790688889, :key :node/yield, :attr 78, :val 1.0}]

;; => [{:id 74766790688889, :key :node/name, :attr 76, :val "Chorizo Wrap"}
;;     {:id 74766790688889, :key :node/uom, :attr 81, :val 83562883711081}
;;     {:id 74766790688889, :key :node/children, :attr 79, :val 74766790688896}
;;     {:id 74766790688889, :key :node/yield, :attr 78, :val 1.0}]

;; => [{:key :node/children, :attr 79, :val 74766790688896}
;;     {:key :node/uom, :attr 81, :val 83562883711081}
;;     {:key :node/name, :attr 76, :val "Chorizo Wrap"}
;;     {:key :node/yield, :attr 78, :val 1.0}]

;; => [{:key :node/yield, :val 1.0}
;;     {:key :node/name, :val "Chorizo Wrap"}
;;     {:key :node/uom, :val 83562883711081}
;;     {:key :node/children, :val 74766790688896}]

;; => [{:key :node/yield, :val 1.0}
;;     {:key :node/name, :val "Chorizo Wrap"}
;;     {:key :node/uom, :val 83562883711081}
;;     {:key :node/children, :val 74766790688896}]

;; => [[:node/yield] [:node/uom] [:node/children] [:node/name]]

;; => [[:node/name]]


#_(tap> (d/datoms (d/db conn) {:index :eavt}))

#_(d/q '[:find ?e ?name
         :where [?e :uom/name ?name]]
       (d/db conn))

#_(tap> (d/datoms (d/db conn) :aevt))