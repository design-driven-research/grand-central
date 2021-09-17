(ns rdd.grand-central.db.neo4j
  (:require [neo4j-clj.core :as db :refer [defquery get-session]]
            [mount.core :refer [defstate]]
            [rdd.grand-central.config :refer [env]])
  (:import (java.net URI)))

(defstate conn
  :start (db/connect (URI. (:neo4j-url env))
                     (:neo4j-username env)
                     (:neo4j-password env))
  :stop (:destroy-fn conn))

(defstate session
  :start (get-session conn))

(defquery install-contraints
  "CREATE CONSTRAINT unique_item_name ON (item:Item) ASSERT item.name IS UNIQUE;
   CREATE CONSTRAINT unique_uom_code ON (uom:UOM) ASSERT uom.code IS UNIQUE;
   CREATE CONSTRAINT unique_uom_name ON (uom:UOM) ASSERT uom.name IS UNIQUE;
   ")

(defquery create-company
  "MERGE (c:Company {name: $name})")

(defquery create-price
  "MERGE (i:Item {name: $itemname})
   MERGE (c:Company {name: $companyname})
   MERGE (c)-[:SELLS]->(sku:SKU {sku: $sku})-[:VARIANT_OF]->(i)
   MERGE (sku)-[:QUOTED]->(price:Price {price: $price})
   MERGE (uom:UOM {code: $uomcode})
   MERGE (price)-[:MEASURED_IN {quantity: $quantity}]->(uom)
   ")

(defquery get-all-items
  "MATCH path = (startNode:Item {name: $name})-[:CONTAINS *1..]->(c:Connection)->[:CONTAINS *1..]->(child:Item) 
   WITH collect(path) AS paths
   CALL apoc.convert.toTree(paths)
   YIELD value
   RETURN value;
   ")

(defquery item->tree
  "MATCH (i:Item {name: $name})
   CALL apoc.path.expandConfig(i, {
   relationshipFilter: 'MADE_OF>|MEASURED_IN>|<VARIANT_OF|>QUOTED|>DIVIDES_INTO'})
   YIELD path
   WITH collect(path) as paths
   CALL apoc.convert.toTree(paths)
   YIELD value
   RETURN value;")

(defquery clear-db "MATCH (n) DETACH DELETE n")

(defquery create-item "MATCH (uom:UOM {code: $uomcode})
                       CREATE (i:Item {name: $name, yield: $yield})-[:MEASURED_IN]->(uom)
                       RETURN i")

(defquery create-measurement-system "MERGE (system:MeasurementSystem {name: $name})
                   RETURN system")

(defquery create-uom "MATCH (system:MeasurementSystem {name: $system})
                      MERGE (uom:UOM {name: $name, code: $code, type: $type})-[:USES_MEASUREMENT_SYSTEM]->(system)
                   RETURN uom")

(defquery create-conversion "MATCH (from:UOM {code: $from})
                             MATCH (to:UOM {code: $to})
                      MERGE (from)-[rel:DIVIDES_INTO {quantity: $quantity}]->(to)
                             RETURN rel")

(defquery add-recipe-component "
                     MATCH (root:Item {name: $parent})
                     MATCH (child:Item {name: $child})
                     MATCH (uom:UOM {code: $uom})
                     CREATE (root)-[:MADE_OF]->(rli:RecipeLineItem)-[:MADE_OF]->(child)
                     CREATE (rli)-[:MEASURED_IN {quantity: $quantity}]->(uom)")


#_(install-contraints (session conn))