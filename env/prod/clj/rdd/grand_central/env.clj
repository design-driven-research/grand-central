(ns rdd.grand-central.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[grand-central started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[grand-central has shut down successfully]=-"))
   :middleware identity})
