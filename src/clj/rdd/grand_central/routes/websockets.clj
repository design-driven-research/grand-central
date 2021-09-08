(ns rdd.grand-central.routes.websockets
  (:require [ring.util.response]
            [taoensso.sente :as sente]
            [postmortem.core :as pm]
            [rdd.grand-central.config :refer [env]]
            [mount.core :refer [defstate]]
            [taoensso.sente.server-adapters.aleph :refer [get-sch-adapter]]))

env


(defstate socket-connection
  :start (let [{:keys [ch-recv send-fn connected-uids
                       ajax-post-fn ajax-get-or-ws-handshake-fn]}
               (sente/make-channel-socket! (get-sch-adapter) {:csrf-token-fn nil ;; Add temporarily during dev
                                                              })]
           {:ch-chsk                       ch-recv
            :chsk-send!                    send-fn
            :connected-uids                connected-uids
            :routes ["/chsk"
                     {:get {:summary "Receive on websocket"
                            :handler #'ajax-get-or-ws-handshake-fn}
                      :post {:summary "Send on websocket"
                             :handler #'ajax-post-fn}}]}))

(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket! (get-sch-adapter) {:csrf-token-fn nil ;; Add temporarily during dev
                                                     })]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

(defn websocket-routes []
  ["/chsk"
   {:get {:summary "Receive on websocket"
          :handler ring-ajax-get-or-ws-handshake}
    :post {:summary "Send on websocket"
           :handler ring-ajax-post}}])

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id)

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (prn (str "Called - " id))
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler
  :default
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-server event}))))

(defmethod -event-msg-handler :data/update!
  [{:as ev-msg :keys [?reply-fn]}]
  (tap> (:?data ev-msg)))

(defmethod -event-msg-handler :chsk/ws-ping
  [{:as ev-msg :keys [?reply-fn]}])

(defonce router_ (atom nil))
(defn  stop-router! [] (when-let [stop-fn @router_] (stop-fn)))
(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-server-chsk-router!
           ch-chsk event-msg-handler)))


#_(stop-router!)

(start-router!)