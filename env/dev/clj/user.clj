(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require [clojure.pprint]
            [clojure.spec.alpha :as s]
            [neo4j-clj.core :refer [execute]]
            [neo4j-clj.core :as neo4j]
            [expound.alpha :as expound]
            [mount.core :as mount]
            [rdd.grand-central.db.neo4j :as db]
            [rdd.grand-central.converters.node-converter :refer [node->tree]]
            [clojure.edn :refer [read-string]]
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

;; Fiddle
#_(time (node->tree
         (-> (db/get-all-items db/session {:name "Chorizo Family Pack"})
             first
             :value)))

#_(time (db/get-all-items db/session {:name "Chorizo Family Pack"}))

(comment
  (db/clear-db db/session)

  (db/install-contraints db/session)

  ;; Create measurement systems
  (do
    (db/create-measurement-system db/session {:name "Imperial"})
    (db/create-measurement-system db/session {:name "Metric"})
    (db/create-measurement-system db/session {:name "Custom"})

  ;; Create UOMs
    (db/create-uom db/session {:name "Pound" :code "lb" :type "WEIGHT" :system "Imperial"})
    (db/create-uom db/session {:name "Gram" :code "gr" :type "WEIGHT" :system "Metric"})
    (db/create-uom db/session {:name "Kilogram" :code "kg" :type "WEIGHT" :system "Metric"})

    (db/create-uom db/session {:name "Each" :code "ea" :type "COUNT" :system "Custom"})


  ;; Create standard unit conversions
    (db/create-conversion db/session {:from "lb"
                                      :to "gr"
                                      :quantity 453.1})

    (db/create-conversion db/session {:from "kg"
                                      :to "gr"
                                      :quantity 1000})

    (db/create-conversion db/session {:from "gr"
                                      :to "gr"
                                      :quantity 1})

  ;; Create item
    (db/create-item db/session {:name "Salt" :yield 1.0 :uomcode "gr"})
    (db/create-item db/session {:name "Pepper" :yield 1.0 :uomcode "gr"})
    (db/create-item db/session {:name "Spicy Sauce" :yield 20.0 :uomcode "gr"})
    (db/create-item db/session {:name "Master Mix"  :yield 30.0 :uomcode "gr"})
    (db/create-item db/session {:name "Chorizo Wrap" :yield 1.0 :uomcode "ea"})
    (db/create-item db/session {:name "Chorizo Family Pack" :yield 1.0 :uomcode "ea"})

    (db/create-price db/session {:itemname "Pepper"
                                 :companyname "Nut Site"
                                 :sku "P001"
                                 :price 25.00
                                 :uomcode "lb"
                                 :quantity 5})

  ;; Create recipe connections
    (db/add-recipe-component db/session {:parent "Spicy Sauce"
                                         :child "Salt"
                                         :quantity 20
                                         :uom "lb"})

    (db/add-recipe-component db/session {:parent "Spicy Sauce"
                                         :child "Pepper"
                                         :quantity 10
                                         :uom "lb"})

    (db/add-recipe-component db/session {:parent "Master Mix"
                                         :child "Spicy Sauce"
                                         :quantity 20
                                         :uom "gr"})

    (db/add-recipe-component db/session {:parent "Chorizo Wrap"
                                         :child "Master Mix"
                                         :quantity 30
                                         :uom "gr"})

    (db/add-recipe-component db/session {:parent "Chorizo Family Pack"
                                         :child "Chorizo Wrap"
                                         :quantity 50
                                         :uom "ea"}))

  (db/item->tree db/session {:name "Chorizo Family Pack"})
  ;; => ({:value
  ;;      {:measured_in ({:_type "UOM", :name "Each", :_id 16, :code "ea", :type "COUNT"}),
  ;;       :yield 1.0,
  ;;       :_type "Item",
  ;;       :name "Chorizo Family Pack",
  ;;       :_id 23,
  ;;       :made_of
  ;;       ({:_type "RecipeLineItem",
  ;;         :measured_in ({:code "ea", :_type "UOM", :name "Each", :measured_in.quantity 50, :_id 16, :type "COUNT"}),
  ;;         :_id 31,
  ;;         :made_of
  ;;         ({:measured_in ({:_type "UOM", :name "Each", :_id 16, :code "ea", :type "COUNT"}),
  ;;           :yield 1.0,
  ;;           :_type "Item",
  ;;           :name "Chorizo Wrap",
  ;;           :_id 22,
  ;;           :made_of
  ;;           ({:_type "RecipeLineItem",
  ;;             :measured_in
  ;;             ({:code "gr",
  ;;               :divides_into ({:divides_into.quantity 1, :code "gr", :_type "UOM", :name "Gram", :_id 14, :type "WEIGHT"}),
  ;;               :_type "UOM",
  ;;               :name "Gram",
  ;;               :measured_in.quantity 30,
  ;;               :_id 14,
  ;;               :type "WEIGHT"}),
  ;;             :_id 30,
  ;;             :made_of
  ;;             ({:measured_in
  ;;               ({:code "gr",
  ;;                 :divides_into
  ;;                 ({:divides_into.quantity 1, :code "gr", :_type "UOM", :name "Gram", :_id 14, :type "WEIGHT"}),
  ;;                 :_type "UOM",
  ;;                 :name "Gram",
  ;;                 :_id 14,
  ;;                 :type "WEIGHT"}),
  ;;               :yield 30.0,
  ;;               :_type "Item",
  ;;               :name "Master Mix",
  ;;               :_id 21,
  ;;               :made_of
  ;;               ({:_type "RecipeLineItem",
  ;;                 :measured_in
  ;;                 ({:code "gr",
  ;;                   :divides_into
  ;;                   ({:divides_into.quantity 1, :code "gr", :_type "UOM", :name "Gram", :_id 14, :type "WEIGHT"}),
  ;;                   :_type "UOM",
  ;;                   :name "Gram",
  ;;                   :measured_in.quantity 20,
  ;;                   :_id 14,
  ;;                   :type "WEIGHT"}),
  ;;                 :_id 29,
  ;;                 :made_of
  ;;                 ({:measured_in
  ;;                   ({:code "gr",
  ;;                     :divides_into
  ;;                     ({:divides_into.quantity 1, :code "gr", :_type "UOM", :name "Gram", :_id 14, :type "WEIGHT"}),
  ;;                     :_type "UOM",
  ;;                     :name "Gram",
  ;;                     :_id 14,
  ;;                     :type "WEIGHT"}),
  ;;                   :yield 20.0,
  ;;                   :_type "Item",
  ;;                   :name "Spicy Sauce",
  ;;                   :_id 19,
  ;;                   :made_of
  ;;                   ({:_type "RecipeLineItem",
  ;;                     :measured_in
  ;;                     ({:code "lb",
  ;;                       :divides_into
  ;;                       ({:divides_into.quantity 453.1,
  ;;                         :code "gr",
  ;;                         :divides_into
  ;;                         ({:divides_into.quantity 1, :code "gr", :_type "UOM", :name "Gram", :_id 14, :type "WEIGHT"}),
  ;;                         :_type "UOM",
  ;;                         :name "Gram",
  ;;                         :_id 14,
  ;;                         :type "WEIGHT"}),
  ;;                       :_type "UOM",
  ;;                       :name "Pound",
  ;;                       :measured_in.quantity 10,
  ;;                       :_id 13,
  ;;                       :type "WEIGHT"}),
  ;;                     :_id 28,
  ;;                     :made_of
  ;;                     ({:measured_in
  ;;                       ({:code "gr",
  ;;                         :divides_into
  ;;                         ({:divides_into.quantity 1, :code "gr", :_type "UOM", :name "Gram", :_id 14, :type "WEIGHT"}),
  ;;                         :_type "UOM",
  ;;                         :name "Gram",
  ;;                         :_id 14,
  ;;                         :type "WEIGHT"}),
  ;;                       :yield 1.0,
  ;;                       :variant_of
  ;;                       ({:_type "SKU",
  ;;                         :quoted
  ;;                         ({:_type "Price",
  ;;                           :measured_in
  ;;                           ({:code "lb",
  ;;                             :divides_into
  ;;                             ({:divides_into.quantity 453.1,
  ;;                               :code "gr",
  ;;                               :divides_into
  ;;                               ({:divides_into.quantity 1,
  ;;                                 :code "gr",
  ;;                                 :_type "UOM",
  ;;                                 :name "Gram",
  ;;                                 :_id 14,
  ;;                                 :type "WEIGHT"}),
  ;;                               :_type "UOM",
  ;;                               :name "Gram",
  ;;                               :_id 14,
  ;;                               :type "WEIGHT"}),
  ;;                             :_type "UOM",
  ;;                             :name "Pound",
  ;;                             :measured_in.quantity 5,
  ;;                             :_id 13,
  ;;                             :type "WEIGHT"}),
  ;;                           :_id 26,
  ;;                           :price 25.0}),
  ;;                         :_id 25,
  ;;                         :sku "P001"}),
  ;;                       :_type "Item",
  ;;                       :name "Pepper",
  ;;                       :_id 18})}
  ;;                    {:_type "RecipeLineItem",
  ;;                     :measured_in
  ;;                     ({:code "lb",
  ;;                       :divides_into
  ;;                       ({:divides_into.quantity 453.1,
  ;;                         :code "gr",
  ;;                         :divides_into
  ;;                         ({:divides_into.quantity 1, :code "gr", :_type "UOM", :name "Gram", :_id 14, :type "WEIGHT"}),
  ;;                         :_type "UOM",
  ;;                         :name "Gram",
  ;;                         :_id 14,
  ;;                         :type "WEIGHT"}),
  ;;                       :_type "UOM",
  ;;                       :name "Pound",
  ;;                       :measured_in.quantity 20,
  ;;                       :_id 13,
  ;;                       :type "WEIGHT"}),
  ;;                     :_id 27,
  ;;                     :made_of
  ;;                     ({:_type "Item",
  ;;                       :name "Salt",
  ;;                       :measured_in
  ;;                       ({:code "gr",
  ;;                         :divides_into
  ;;                         ({:divides_into.quantity 1, :code "gr", :_type "UOM", :name "Gram", :_id 14, :type "WEIGHT"}),
  ;;                         :_type "UOM",
  ;;                         :name "Gram",
  ;;                         :_id 14,
  ;;                         :type "WEIGHT"}),
  ;;                       :_id 17,
  ;;                       :yield 1.0})})})})})})})})}})

  ;; => ({:value
  ;;      {:_type "Item",
  ;;       :name "Chorizo Family Pack",
  ;;       :_id 23,
  ;;       :made_of
  ;;       ({:_type "RecipeLineItem",
  ;;         :measured_in ({:code "ea", :_type "UOM", :name "Each", :measured_in.quantity 50, :_id 16, :type "COUNT"}),
  ;;         :_id 31,
  ;;         :made_of
  ;;         ({:_type "Item",
  ;;           :name "Chorizo Wrap",
  ;;           :_id 22,
  ;;           :made_of
  ;;           ({:_type "RecipeLineItem",
  ;;             :measured_in
  ;;             ({:code "gr",
  ;;               :divides_into ({:divides_into.quantity 1, :code "gr", :_type "UOM", :name "Gram", :_id 14, :type "WEIGHT"}),
  ;;               :_type "UOM",
  ;;               :name "Gram",
  ;;               :measured_in.quantity 30,
  ;;               :_id 14,
  ;;               :type "WEIGHT"}),
  ;;             :_id 30,
  ;;             :made_of
  ;;             ({:_type "Item",
  ;;               :name "Master Mix",
  ;;               :_id 21,
  ;;               :made_of
  ;;               ({:_type "RecipeLineItem",
  ;;                 :measured_in
  ;;                 ({:code "gr",
  ;;                   :divides_into
  ;;                   ({:divides_into.quantity 1, :code "gr", :_type "UOM", :name "Gram", :_id 14, :type "WEIGHT"}),
  ;;                   :_type "UOM",
  ;;                   :name "Gram",
  ;;                   :measured_in.quantity 20,
  ;;                   :_id 14,
  ;;                   :type "WEIGHT"}),
  ;;                 :_id 29,
  ;;                 :made_of
  ;;                 ({:_type "Item",
  ;;                   :name "Spicy Sauce",
  ;;                   :_id 19,
  ;;                   :made_of
  ;;                   ({:_type "RecipeLineItem",
  ;;                     :measured_in
  ;;                     ({:code "lb",
  ;;                       :divides_into
  ;;                       ({:divides_into.quantity 453.1,
  ;;                         :code "gr",
  ;;                         :divides_into
  ;;                         ({:divides_into.quantity 1, :code "gr", :_type "UOM", :name "Gram", :_id 14, :type "WEIGHT"}),
  ;;                         :_type "UOM",
  ;;                         :name "Gram",
  ;;                         :_id 14,
  ;;                         :type "WEIGHT"}),
  ;;                       :_type "UOM",
  ;;                       :name "Pound",
  ;;                       :measured_in.quantity 10,
  ;;                       :_id 13,
  ;;                       :type "WEIGHT"}),
  ;;                     :_id 28,
  ;;                     :made_of
  ;;                     ({:_type "Item",
  ;;                       :name "Pepper",
  ;;                       :_id 18,
  ;;                       :variant_of
  ;;                       ({:_type "SKU",
  ;;                         :quoted
  ;;                         ({:_type "Price",
  ;;                           :measured_in
  ;;                           ({:code "lb",
  ;;                             :divides_into
  ;;                             ({:divides_into.quantity 453.1,
  ;;                               :code "gr",
  ;;                               :divides_into
  ;;                               ({:divides_into.quantity 1,
  ;;                                 :code "gr",
  ;;                                 :_type "UOM",
  ;;                                 :name "Gram",
  ;;                                 :_id 14,
  ;;                                 :type "WEIGHT"}),
  ;;                               :_type "UOM",
  ;;                               :name "Gram",
  ;;                               :_id 14,
  ;;                               :type "WEIGHT"}),
  ;;                             :_type "UOM",
  ;;                             :name "Pound",
  ;;                             :measured_in.quantity 5,
  ;;                             :_id 13,
  ;;                             :type "WEIGHT"}),
  ;;                           :_id 26,
  ;;                           :price 25.0}),
  ;;                         :_id 25,
  ;;                         :sku "P001"})})}
  ;;                    {:_type "RecipeLineItem",
  ;;                     :measured_in
  ;;                     ({:code "lb",
  ;;                       :divides_into
  ;;                       ({:divides_into.quantity 453.1,
  ;;                         :code "gr",
  ;;                         :divides_into
  ;;                         ({:divides_into.quantity 1, :code "gr", :_type "UOM", :name "Gram", :_id 14, :type "WEIGHT"}),
  ;;                         :_type "UOM",
  ;;                         :name "Gram",
  ;;                         :_id 14,
  ;;                         :type "WEIGHT"}),
  ;;                       :_type "UOM",
  ;;                       :name "Pound",
  ;;                       :measured_in.quantity 20,
  ;;                       :_id 13,
  ;;                       :type "WEIGHT"}),
  ;;                     :_id 27,
  ;;                     :made_of ({:_type "Item", :name "Salt", :_id 17})})})})})})})})}})



  (neo4j/execute db/session "MATCH (n:Item {name: 'Chorizo Wrap'})-[r1:MADE_OF *1..]->(rli:RecipeLineItem),
    (rli)-[r2:MADE_OF]->(child:Item)
MATCH (rli)-[r3:MEASURED_IN]->(uom:UOM)
RETURN n, rli, child, uom, r1, r2, r3")
  ;; => ({:n {:name "Chorizo Wrap"},
  ;;      :rli {},
  ;;      :child {:name "Master Mix"},
  ;;      :uom {:name "Gram", :code "gr", :type "WEIGHT"},
  ;;      :r1 ({}),
  ;;      :r2 {},
  ;;      :r3 {:quantity 30}}
  ;;     {:n {:name "Chorizo Wrap"},
  ;;      :rli {},
  ;;      :child {:name "Spicy Sauce"},
  ;;      :uom {:name "Gram", :code "gr", :type "WEIGHT"},
  ;;      :r1 ({} {} {}),
  ;;      :r2 {},
  ;;      :r3 {:quantity 20}}
  ;;     {:n {:name "Chorizo Wrap"},
  ;;      :rli {},
  ;;      :child {:name "Salt"},
  ;;      :uom {:name "Pound", :code "lb", :type "WEIGHT"},
  ;;      :r1 ({} {} {} {} {}),
  ;;      :r2 {},
  ;;      :r3 {:quantity 20}}
  ;;     {:n {:name "Chorizo Wrap"},
  ;;      :rli {},
  ;;      :child {:name "Pepper"},
  ;;      :uom {:name "Pound", :code "lb", :type "WEIGHT"},
  ;;      :r1 ({} {} {} {} {}),
  ;;      :r2 {},
  ;;      :r3 {:quantity 10}})



  ;; 
  )

