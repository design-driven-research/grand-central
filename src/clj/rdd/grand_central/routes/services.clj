(ns rdd.grand-central.routes.services
  (:require [rdd.grand-central.middleware.formats :as formats]
            [reitit.coercion.spec :as spec-coercion]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception]
            [postmortem.core :as pm]
            [rdd.grand-central.services.store]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [rdd.grand-central.services.store :as store]
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
                 ;; exceptions (not sure if needed)
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

   ["/transactor"
    {:post {:summary "The transactor endpoint. Handles datomic/datascript tx-data"

            :responses {200 {:body {}}
                        400 {:body {:error string?}}}
            :handler (fn [request]
                       (pm/spy>> :transactor {:request-data
                                              (-> request :body-params :tx-data)})
                       (let [tx-data (-> request :body-params :tx-data)
                             result (store/transact-from-remote! tx-data)]

                         {:status 200
                          :body {:result {:msg "Success"}}}))}}]

   ["/custom"
    ["/initial-data"
     {:get {:summary "Load initial data"

            :responses {200 {:body {:result map?}}
                        400 {:body {:error string?}}}
            :handler (fn [request]
                       {:status 200
                        :body {:result {:data (store/initial-data)}}})}}]]

   ["/composites"
    ["/recipe-line-items"
     {:post {:summary "Update recipe line item"
             :parameters {:body {:uuid string?}}
             :responses {200 {:body {:result map?}}
                         400 {:body {:error string?}}}
             :handler (fn [{:keys [body-params]}]
                        (store/update-recipe-line-item body-params)
                        {:status 200
                         :body {:result {:msg "Success"}}})}}]]
   ["/items"
    ["/"
     {:post {:summary "Update the quantity"
             :parameters {:body {:uuid string?
                                 :quantity number?}}
             :responses {200 {:body {:result map?}}
                         400 {:body {:error string?}}}
             :handler (fn [{:keys [body-params]}]
                        (let [{:keys [uuid quantity]} body-params]
                          (store/update-recipe-line-item-quantity uuid quantity)
                          {:status 200
                           :body {:result {:msg "Success"}}}))}}]

    ["/:item-name"
     {:get {:parameters {:path {:item-name string?}}
            :handler (fn [request]
                       (let [item-name (-> request :parameters :path :item-name)]
                         {:status 200
                          :body {:item {} #_(store/item->tree item-name)}}))}}]]

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


#_(pm/reset!)
#_(pm/log-for :transactor)
  ;; => [{:request-data [[:db/add [:recipe-line-item/uuid "N0s9TOLaGm-nxrmoUUsh8"] :measurement/quantity 4]]}
  ;;     {:request-data [[:db/add [:recipe-line-item/uuid "jno0WN4RToRou6LDjdNlE"] :measurement/quantity 5]]}]

  ;; => {:transactor
  ;;     [{:request-data [[:db/add [:recipe-line-item/uuid "N0s9TOLaGm-nxrmoUUsh8"] :measurement/quantity 4]]}
  ;;      {:request-data [[:db/add [:recipe-line-item/uuid "jno0WN4RToRou6LDjdNlE"] :measurement/quantity 5]]}]}

  ;; => {}

  ;; => {:transactor
  ;;     [{:request-data
  ;;       [[:db/add
  ;;         [:recipe-line-item/uuid "nNN6y6ImyIHXPEcIOcHvZ"]
  ;;         :recipe-line-item/company-item
  ;;         [:company-item/uuid "e4OcffmS9HU-9FkLF0L6i"]]]}],
  ;;     :from-remote
  ;;     [[[:db/add
  ;;        [:recipe-line-item/uuid "nNN6y6ImyIHXPEcIOcHvZ"]
  ;;        :recipe-line-item/company-item
  ;;        [:company-item/uuid "e4OcffmS9HU-9FkLF0L6i"]]]]}

  ;; => {:from-remote
  ;;     [[[:db/add
  ;;        [:recipe-line-item/uuid "YzZhgyPiLPCvI0TXclr9z"]
  ;;        :recipe-line-item/company-item
  ;;        [:company-item/uuid "7k_ebFF2lQLyj6vw9N0qd"]]]],
  ;;     :transactor
  ;;     [{:request-data
  ;;       [[:db/add
  ;;         [:recipe-line-item/uuid "F7xaCMScCYL_mi-vYcFUx"]
  ;;         :recipe-line-item/company-item
  ;;         [:company-item/uuid "z5pIUOgcJK5LLGW5YUTXa"]]]}]}

#_(pm/log-for :transactor)
;; => [{:request-data [[:db/add [:recipe-line-item/uuid "N0s9TOLaGm-nxrmoUUsh8"] :measurement/quantity 4]]}
;;     {:request-data [[:db/add [:recipe-line-item/uuid "jno0WN4RToRou6LDjdNlE"] :measurement/quantity 5]]}]
