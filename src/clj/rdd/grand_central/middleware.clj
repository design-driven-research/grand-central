(ns rdd.grand-central.middleware
  (:require
   [rdd.grand-central.env :refer [defaults]]
   [rdd.grand-central.config :refer [env]]
   [ring-ttl-session.core :refer [ttl-memory-store]]
   [postmortem.core :as pm]
   [ring.middleware.defaults :refer [site-defaults api-defaults wrap-defaults]]))

(defn wrap-cors
  "Wrap the server response with new headers to allow Cross Origin."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (-> response
          (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
          (assoc-in [:headers "Access-Control-Allow-Headers"] "*")
          (assoc-in [:headers "Access-Control-Allow-Methods"] "*")))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      (wrap-cors)
      (wrap-defaults
       (-> site-defaults
           #_(assoc-in [:security :anti-forgery] false)
           (assoc-in  [:session :store] (ttl-memory-store (* 60 30)))))))

#_(pm/reset!)
#_(pm/log-for :middles)