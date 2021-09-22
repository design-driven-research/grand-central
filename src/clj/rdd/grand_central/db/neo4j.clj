(ns rdd.grand-central.db.neo4j
  (:require [neo4j-clj.core :as db :refer [defquery get-session]]
            [mount.core :refer [defstate]]
            [nano-id.core :refer [nano-id]]
            [postmortem.core :as pm]
            [clojure.edn :refer [read-string]]
            [rdd.grand-central.config :refer [env]])
  (:import (java.net URI)))

(defstate conn
  :start (db/connect (URI. (:neo4j-url env))
                     (:neo4j-username env)
                     (:neo4j-password env))
  :stop (:destroy-fn conn))

(defstate session
  :start (get-session conn))

(defquery
  install-contraints
  "CREATE CONSTRAINT unique_item_name ON (item:Item) ASSERT item.name IS UNIQUE;
   CREATE CONSTRAINT unique_uom_code ON (uom:UOM) ASSERT uom.code IS UNIQUE;
   CREATE CONSTRAINT unique_uom_name ON (uom:UOM) ASSERT uom.name IS UNIQUE;
   ")


(defquery
  create-company-query
  "MERGE(c:Company {name: $name})
   SET c.uuid = $uuid
   RETURN c")
#_(create-company-query session {:name "ABC Organics" :uuid "asdfasdf"})

(defquery
  create-price-query
  "MATCH (i:Item {name: $item})
   MATCH (c:Company {name: $company})
   MATCH (uom:UOM {code: $uom})

   MERGE (c)-[:SELLS]->(sku:SKU {sku: $sku})-[:VARIANT_OF]->(i)

   CREATE (sku)-[:QUOTED]->(price:Price {price: $price})
   CREATE (price)-[:MEASURED_IN {quantity: $quantity}]->(uom)
   SET price.uuid = $uuid

   RETURN price")

(defquery
  get-all-items
  "MATCH path = (startNode:Item {name: $name})-[:CONTAINS *1..]->(c:Connection)->[:CONTAINS *1..]->(child:Item) 
   WITH collect(path) AS paths
   CALL apoc.convert.toTree(paths)
   YIELD value
   RETURN value")

(defquery
  item->tree-query
  "MATCH (i:Item {name: $name})
   CALL apoc.path.expandConfig(i, {
   relationshipFilter: 'MADE_OF>|MEASURED_IN>|<VARIANT_OF|>QUOTED|>DIVIDES_INTO'})
   YIELD path
   WITH collect(path) as paths
   CALL apoc.convert.toTree(paths)
   YIELD value
   RETURN value")

(defquery
  clear-db!
  "MATCH (n) DETACH DELETE n")

(defquery
  create-item
  "MATCH (uom:UOM {code: $uom})
   MERGE (i:Item {name: $name})
   SET i.uuid = $uuid, i.yield = $yield
   CREATE (i)-[:MEASURED_IN]->(uom)
   RETURN i")

(defquery
  create-measurement-system-query
  "MERGE (system:MeasurementSystem {name: $name})
   SET system.uuid = $uuid
   RETURN system")

(defquery
  create-uom-query
  "MATCH (system:MeasurementSystem {name: $system})
   MERGE (uom:UOM {name: $name, code: $code, type: $type})-[:USES_MEASUREMENT_SYSTEM]->(system)
   SET uom.uuid = $uuid
   RETURN uom")

(defquery
  create-conversion-query
  "MATCH (from:UOM {code: $from})
   MATCH (to:UOM {code: $to})
   MERGE (from)-[rel:DIVIDES_INTO {quantity: $quantity}]->(to)
   SET rel.uuid = $uuid
   RETURN rel")

(defquery
  create-recipe-line-item-query
  "MATCH (parent:Item {name: $parent})
   MATCH (child:Item {name: $child})
   MATCH (uom:UOM {code: $uom})
   CREATE (parent)-[:MADE_OF]->(rli:RecipeLineItem)-[:MADE_OF]->(child)
   CREATE (rli)-[:MEASURED_IN {quantity: $quantity}]->(uom)
   SET rli.uuid = $uuid
   RETURN rli, parent")

(defquery
  update-recipe-line-item-quantity-query
  "MATCH (rli:RecipeLineItem {uuid: $uuid})-[rel:MEASURED_IN]->(uom:UOM)
   SET rel.quantity = $quantity
   RETURN rli, uom, rel")

(defn item->tree
  [product-name]
  (-> (item->tree-query session {:name product-name})
      first
      :value))

#_(item->tree-query session {:name "Chorizo Family Pack"})
#_(item->tree "Chorizo Family Pack")

(defn add-uuid
  [m]
  (if (map? m)
    (assoc m :uuid (nano-id))
    m))

(defn- augment-with-uuids
  "Augment a map containing vectors with uuids for each key
   {:items [{:a 1}]} -> {:items [{:a 1 :uuid 'rGQA6dFAuLk-bP2XmxwIh'}]}
   "
  ([data] (augment-with-uuids data (keys data)))
  ([data keys]
   (if (empty? keys)
     data
     (let [key (first keys)
           augmented-key (or (map add-uuid (key data))
                             (key data))
           updated-data (assoc data key augmented-key)]
       (augment-with-uuids updated-data (rest keys))))))

(defn create-nodes!
  [key query data]
  (doseq [node (key data)]
    (query session node))
  data)

(defn create-relationship
  [parent {:keys [name quantity uom]}]
  (let [query-args {:parent (:name parent)
                    :child name
                    :uuid (nano-id)
                    :uom uom
                    :quantity quantity}]

    (create-recipe-line-item-query
     session
     query-args)))


(defn create-recipes!
  [data]
  (let [items (:items data)]
    (doseq [parent items
            :when (seq (:items parent))]

      (doseq [child (:items parent)]
        (create-relationship parent child)))

    data))

(defn seed-db
  []
  (clear-db! session)
  (->>
   (slurp "resources/seeds/base.edn")
   read-string
   (augment-with-uuids)
   (create-nodes! :companies create-company-query)
   (create-nodes! :measurment-system create-measurement-system-query)
   (create-nodes! :uoms create-uom-query)
   (create-nodes! :items create-item)
   (create-recipes!)
   (create-nodes! :conversions create-conversion-query)
   (create-nodes! :prices create-price-query)))



(comment
  ;; Just clear db
  (clear-db! session)

  ;; Auto clears and seed db
  (seed-db)

  ;; Only works if brand new db
  (install-contraints session)

  ;; 
  )
