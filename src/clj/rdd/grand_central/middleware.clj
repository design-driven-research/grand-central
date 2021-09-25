(ns rdd.grand-central.middleware
  (:require [rdd.grand-central.env :refer [defaults]]
            [ring-ttl-session.core :refer [ttl-memory-store]]
            [ring.middleware.cors :as cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      (wrap-cors :access-control-allow-origin [#"http://localhost:4200"]
                 :access-control-allow-headers #{"accept"
                                                 "accept-encoding"
                                                 "accept-language"
                                                 "authorization"
                                                 "content-type"
                                                 "origin"}
                 :access-control-allow-methods [:get :put :post :delete :options])
      (wrap-defaults
       (-> site-defaults
           (assoc-in [:security :anti-forgery] false)
           (assoc-in  [:session :store] (ttl-memory-store (* 60 30)))))))