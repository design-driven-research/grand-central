(ns rdd.grand-central.handler
  (:require [mount.core :as mount]
            [reitit.core]
            [reitit.coercion]
            [rdd.grand-central.env :refer [defaults]]
            [rdd.grand-central.middleware :as middleware]
            [rdd.grand-central.routes.services :refer [service-routes]]
            [reitit.ring :as ring]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.webjars :refer [wrap-webjars]]))

(mount/defstate init-app
  :start ((or (:init defaults) (fn [])))
  :stop  ((or (:stop defaults) (fn []))))

(mount/defstate app-routes
  :start (ring/ring-handler
          (ring/router
           [["/" {:get
                  {:handler (constantly {:status 301 :headers {"Location" "/api/api-docs/index.html"}})}}]
            (service-routes)])
          (ring/routes
           (ring/create-resource-handler
            {:path "/"})
           (wrap-content-type (wrap-webjars (constantly nil)))
           (ring/create-default-handler))))

#'app-routes
(defn app []
  (middleware/wrap-base #'app-routes))