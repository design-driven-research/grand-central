(ns rdd.grand-central.routes.services
  (:require [rdd.grand-central.middleware.formats :as formats]
            [reitit.coercion.spec :as spec-coercion]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [postmortem.core :as pm]
            [rdd.grand-central.db.core :as db]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [ring.middleware.anti-forgery :as af]

            [ring.util.http-response :refer :all]))

(defn service-routes []
  ["/api"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 coercion/coerce-exceptions-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware

                ;;  
                 exception/exception-middleware]}

   ;; swagger documentation
   ["" {:no-doc true
        :swagger {:info {:title "RDD"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
            {:url "/api/swagger.json"
             :config {:validator-url nil}})}]]

   ["/items"
    ["/"
     {:post {:summary "Update the quantity"
             :parameters {:body {:uuid string?
                                 :quantity float?}}
             :responses {200 {:body {:result map?}}
                         400 {:body {:error string?}}}
             :handler (fn [{:keys [body-params]}]
                      ;;  (tap> body-params)
                        (pm/spy>> :req body-params)
                        {:status 200
                         :body {:result {:msg "Success"}}})}}]
    ["/:item-name"
     {:parameters {:path {:item-name string?}}
      :get {:handler (fn [request]
                       (let [item-name (-> request :parameters :path :item-name)]
                         {:status 200
                          :body {:item (db/item->tree item-name)}}))}}]]

  ;;  
   ["/test/:val"
    {:parameters {:path {:val int?}}
     :get {:handler (fn [request]
                      (tap> (-> request :parameters :path :val))
                      {:status 200
                       :body {:item {}}})}}]

   ["/ping"
    {:get (constantly (ok {:message "pong"}))}]

   ["/init"
    {:get {:responses {200 {:body {:token string?}}}
           :handler (fn [request]
                      {:status 200
                       :body {:token af/*anti-forgery-token*}})}}]])

(comment
  (-> (pm/log-for :req)
      (first)
      :body
      (slurp))
  (pm/log-for :req)

  (pm/reset!)
  ;; 
  )