(ns rdd.grand-central.converters.node-converter)

(defn node->tree
  [node]
  (let [name (:name node)
        children (:contains node)
        has-children? (seq children)]
    (-> {}
        (cond-> true (assoc :name name))
        (cond-> has-children? (assoc :children (map node->tree children))))))

