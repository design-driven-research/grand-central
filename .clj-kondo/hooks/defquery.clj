(ns hooks.defquery
  (:require [clj-kondo.hooks-api :as api]))

(defn defquery [{:keys [:node]}]
  (let [args (-> node :children rest)
        name (-> args first)
        new-node (api/list-node
                  (list
                   (api/token-node 'defn)
                   name
                   (api/vector-node '[sess & params])))]
    {:node new-node}))

;; (defmacro defquery
;;   "Shortcut macro to define a named query."
;;   [name ^String query]
;;   `(def ~name (create-query ~query)))


;; (defn create-query
;;   "Convenience function. Takes a cypher query as input, returns a function that
;;   takes a session (and parameter as a map, optionally) and return the query
;;   result as a map."
;;   [cypher]
;;   (fn
;;     ([sess] (execute sess cypher))
;;     ([sess params] (execute sess cypher params))))

;; (ns hooks.defquery
;;   (:require [clj-kondo.hooks-api :as api]))

;; (defn defquery [{:keys [:node]}]
;;   (let [args (-> node :children rest)
;;         name (-> args first)
;;         query (-> args second)
;;         query-is-string? (= (type query) "class clj_kondo.impl.rewrite_clj.node.string.StringNode")
;;         is-type (type query)
;;         new-node (api/list-node
;;                   (list
;;                    (api/token-node 'defn)
;;                    name
;;                    (api/vector-node '[sess & params])))]
;;     (let [{:keys [:row :col]} (meta node)]
;;       (when-not query-is-string?
;;         (api/reg-finding! {:message (format "defquery query of type '%s' should be a string. " is-type)
;;                            :type    :clj-kondo.hooks.defquery/argument-type
;;                            :row row
;;                            :col col})))
;;     {:node new-node}))

;; (defmacro defquery
;;   "Shortcut macro to define a named query."
;;   [name ^String query]
;;   `(def ~name (create-query ~query)))


;; (defn create-query
;;   "Convenience function. Takes a cypher query as input, returns a function that
;;   takes a session (and parameter as a map, optionally) and return the query
;;   result as a map."
;;   [cypher]
;;   (fn
;;     ([sess] (execute sess cypher))
;;     ([sess params] (execute sess cypher params))))