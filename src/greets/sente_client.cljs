(ns greets.sente-client
  "Official Sente reference example: client"
  {:author "Peter Taoussanis (@ptaoussanis)"}

  (:require
    [clojure.string :as str]
    [cljs.core.async :as async :refer (<! >! put! chan)]
    [taoensso.encore :as encore :refer-macros (have have?)]
    [taoensso.timbre :as timbre :refer-macros (tracef debugf infof warnf errorf)]
    [taoensso.sente :as sente :refer (cb-success?)]

    ;; Optional, for Transit encoding:
    [taoensso.sente.packers.transit :as sente-transit])

  (:require-macros
    [cljs.core.async.macros :as asyncm :refer (go go-loop)]))

;(timbre/set-level! :debug)

(defn ->output! [fmt & args]
      (let [msg (apply encore/format fmt args)]
           (timbre/debug msg)
           (.log js/console msg)))

(->output! "ClojureScript appears to have loaded correctly.")

;;;; Define our Sente channel socket (chsk) client

(let [;; For this example, select a random protocol:
      ;; Serializtion format, must use same val for client + server:
      packer
      :edn                                           ; Default packer, a good choice in most cases
      ;; (sente-transit/get-transit-packer) ; Needs Transit dep

      {:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client!
        "/chsk"                                             ; Must match server Ring routing URL
        {:type :auto
         :packer packer})]

     (def chsk chsk)
     (def ch-chsk ch-recv)                                  ; ChannelSocket's receive channel
     (def chsk-send! send-fn)                               ; ChannelSocket's send API fn
     (def chsk-state state)                                 ; Watchable, read-only atom
     )

;;;; Sente event handlers

(defmulti -event-msg-handler
          "Multimethod to handle Sente `event-msg`s"
          :id                                               ; Dispatch on event-id
          )

(defn event-msg-handler
      "Wraps `-event-msg-handler` with logging, error catching, etc."
      [{:as ev-msg :keys [id ?data event]}]
      (-event-msg-handler ev-msg))

(defmethod -event-msg-handler
           :default                                         ; Default/fallback case (no other matching handler)
           [{:as ev-msg :keys [event]}]
           (->output! "Unhandled event: %s" event))

(defmethod -event-msg-handler :chsk/state
           [{:as ev-msg :keys [?data]}]
           (let [[old-state-map new-state-map] (have vector? ?data)]
                (if (:first-open? new-state-map)
                  (->output! "Channel socket successfully established!: %s" new-state-map)
                  (->output! "Channel socket state change: %s" new-state-map))))

(defmethod -event-msg-handler :chsk/recv
           [{:as ev-msg :keys [?data]}]
           (->output! "Push event from server: %s" ?data))

(defmethod -event-msg-handler :chsk/handshake
           [{:as ev-msg :keys [?data]}]
           (let [[?uid ?csrf-token ?handshake-data] ?data]
                (->output! "Handshake: %s" ?data)))

;; TODO Add your (defmethod -event-msg-handler <event-id> [ev-msg] <body>)s here...

;;;; Sente event router (our `event-msg-handler` loop)

(defonce router_ (atom nil))
(defn stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
      (stop-router!)
      (reset! router_
              (sente/start-client-chsk-router!
                ch-chsk event-msg-handler)))

;;;; Init stuff

(defn start!
      []
      (start-router!)
      )

(defonce _start-once (start!))
