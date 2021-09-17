(ns rdd.grand-central.services.socket-server
  (:require [mount.core :refer [defstate]]
            [ring.util.response]
            [taoensso.sente :as sente]
            [rdd.grand-central.db.core :as db]
            [taoensso.sente.server-adapters.aleph :refer [get-sch-adapter]]))

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
                            :handler ajax-get-or-ws-handshake-fn}
                      :post {:summary "Send on websocket"
                             :handler ajax-post-fn}}]}))

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

(defmethod -event-msg-handler
  :data/update!
  [{:as ev-msg :keys [?reply-fn]}]
  (tap> (:?data ev-msg)))

(defmethod -event-msg-handler
  :chsk/ws-ping
  [{:as ev-msg :keys [?reply-fn]}])

(defmethod -event-msg-handler
  :data/item-by-name
  [{:as ev-msg :keys [?reply-fn ?data]}]
  (when ?reply-fn
    (let [tree (db/item->tree (:product-name ?data))]
      (?reply-fn tree))))

(defstate socket-server
  :start (let [conn (:ch-chsk socket-connection)
               stop-fn (sente/start-server-chsk-router! conn event-msg-handler)]
           {:stop-fn! stop-fn})
  :stop ((:stop-fn socket-server)))