(ns rdd.grand-central.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [rdd.grand-central.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[grand-central started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[grand-central has shut down successfully]=-"))
   :middleware wrap-dev})
