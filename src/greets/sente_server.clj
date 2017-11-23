(ns greets.sente-server
    "Official Sente reference example: server"
    {:author "Peter Taoussanis (@ptaoussanis)"}

    (:require
      [clojure.string :as str]
      [ring.util.response :as response]
      [ring.middleware.defaults]
      [compojure.core :as comp :refer (defroutes GET POST)]
      [compojure.route :as route]
      [clojure.core.async :as async :refer (<! <!! >! >!! put! chan go go-loop)]
      [taoensso.encore :as encore :refer (have have?)]
      [taoensso.timbre :as timbre :refer (tracef debugf infof warnf errorf)]
      [taoensso.sente :as sente]

      ;;; TODO Choose (uncomment) a supported web server + adapter -------------
      [org.httpkit.server :as http-kit]
      [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
      ;;
      ;; [immutant.web :as immutant]
      ;; [taoensso.sente.server-adapters.immutant :refer (get-sch-adapter)]
      ;;
      ;; [nginx.clojure.embed :as nginx-clojure]
      ;; [taoensso.sente.server-adapters.nginx-clojure :refer (get-sch-adapter)]
      ;;
      ;; [aleph.http :as aleph]
      ;; [taoensso.sente.server-adapters.aleph :refer (get-sch-adapter)]
      ;; -----------------------------------------------------------------------

      ;; Optional, for Transit encoding:
      [taoensso.sente.packers.transit :as sente-transit]))

;; (timbre/set-level! :trace) ; Uncomment for more logging
(reset! sente/debug-mode?_ false)                            ; Uncomment for extra debug info

;;;; Define our Sente channel socket (chsk) server

(let [;; Seriaztion format, must use same val for client + server:
      packer :edn                                           ; Default packer, a good choice in most cases
      ;; (sente-transit/get-transit-packer) ; Needs Transit dep

      chsk-server
      (sente/make-channel-socket-server!
        (get-sch-adapter) {:packer packer})

      {:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      chsk-server]

     (def ring-ajax-post ajax-post-fn)
     (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
     (def ch-chsk ch-recv)                                  ; ChannelSocket's receive channel
     (def chsk-send! send-fn)                               ; ChannelSocket's send API fn
     (def connected-uids connected-uids)                    ; Watchable, read-only atom
     )

;; We can watch this atom for changes if we like
(add-watch connected-uids :connected-uids
           (fn [_ _ old new]
               (when (not= old new)
                     (infof "Connected uids change: %s" new))))

;;;; Ring handlers

(defn login-handler
      "Here's where you'll add your server-side login/auth procedure (Friend, etc.).
      In our simplified example we'll just always successfully authenticate the user
      with whatever user-id they provided in the auth request."
      [ring-req]
      (let [{:keys [session params]} ring-req
            {:keys [user-id]} params]
           (debugf "Login request: %s" params)
           {:status 200 :session (assoc session :uid user-id)}))

(defroutes ring-routes
           (GET "/" ring-req (response/content-type (response/resource-response "index.html") "text/html"))
           (GET "/chsk" ring-req (ring-ajax-get-or-ws-handshake ring-req))
           (POST "/chsk" ring-req (ring-ajax-post ring-req))
           (route/resources "/")                            ; Static files, notably public/main.js (our cljs target)
           (route/not-found "<h1>Page not found</h1>"))

(def main-ring-handler
  "**NB**: Sente requires the Ring `wrap-params` + `wrap-keyword-params`
  middleware to work. These are included with
  `ring.middleware.defaults/wrap-defaults` - but you'll need to ensure
  that they're included yourself if you're not using `wrap-defaults`."
  (ring.middleware.defaults/wrap-defaults
    ring-routes ring.middleware.defaults/site-defaults))

;;;; Some server>user async push examples

(defn test-fast-server>user-pushes
      "Quickly pushes 100 events to all connected users. Note that this'll be
      fast+reliable even over Ajax!"
      []
      (doseq [uid (:any @connected-uids)]
             (doseq [i (range 100)]
                    (chsk-send! uid [:fast-push/is-fast (str "hello " i "!!")]))))

;;;; Sente event handlers

(defmulti -event-msg-handler
          "Multimethod to handle Sente `event-msg`s"
          :id                                               ; Dispatch on event-id
          )

(defn event-msg-handler
      "Wraps `-event-msg-handler` with logging, error catching, etc."
      [{:as ev-msg :keys [id ?data event]}]
      (-event-msg-handler ev-msg)                           ; Handle event-msgs on a single thread
      ;; (future (-event-msg-handler ev-msg)) ; Handle event-msgs on a thread pool
      )

(defmethod -event-msg-handler
           :default                                         ; Default/fallback case (no other matching handler)
           [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
           (let [session (:session ring-req)
                 uid (:uid session)]
                (debugf "Unhandled event: %s" event)
                (when ?reply-fn
                      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defmethod -event-msg-handler :example/test-rapid-push
           [ev-msg] (test-fast-server>user-pushes))


;; TODO Add your (defmethod -event-msg-handler <event-id> [ev-msg] <body>)s here...

;;;; Sente event router (our `event-msg-handler` loop)

(defonce router_ (atom nil))
(defn stop-router! [] (when-let [stop-fn @router_] (stop-fn)))
(defn start-router! []
      (stop-router!)
      (reset! router_
              (sente/start-server-chsk-router!
                ch-chsk event-msg-handler)))

;;;; Init stuff

(defonce web-server_ (atom nil))                            ; (fn stop [])
(defn stop-web-server! [] (when-let [stop-fn @web-server_] (stop-fn)))
(defn start-web-server! [& [port]]
      (stop-web-server!)
      (let [port (or port 3000)                                ; 0 => Choose any available port
            ring-handler (var main-ring-handler)

            [port stop-fn]
            (let [stop-fn (http-kit/run-server ring-handler {:port port})]
                 [(:local-port (meta stop-fn)) (fn [] (stop-fn :timeout 100))])

            uri (format "http://localhost:%s/" port)]

           (infof "Web server is running at `%s`" uri)

           (reset! web-server_ stop-fn)))

(defn stop! [] (stop-router!) (stop-web-server!))
(defn start! [port] (start-router!) (start-web-server! port))
