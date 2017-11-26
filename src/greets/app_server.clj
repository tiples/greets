(ns greets.app-server
  (:require
    [clojure.edn :as edn]
    [hiccup.core :as hiccup]
    [greets.sente-server :as sente-server]
    [greets.atoms-server :as atoms]
    [greets.files :as files]))

(defmethod sente-server/-event-msg-handler
  :chsk/uidport-close ;todo close session, if any
  [ev-msg]
  ())

(defmethod sente-server/-event-msg-handler
  :login-token/request
  [ev-msg]
  (let [client-id (:client-id ev-msg)
        ?data (:?data ev-msg)
        email-address (:email-address ?data)]
    (println client-id)
    (println email-address)))

(defn landing-pg-handler
  [ring-req]
  (let [params (:params ring-req)
        token (:token params)
        token-status-message (if (nil? token)
                               (str "Please enter your registered email address "
                                    "to receive a one-time login token.")
                               nil)]
    (hiccup/html
      [:head
       [:meta {:charset "utf-8"}]
       [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
       [:meta {:name "description" :content ""}]
       [:title "greets"]]
      [:body
       [:input {:type "hidden" :id "tokenStatusMessage" :value token-status-message}]
       [:input {:type "hidden" :id "token" :value token}]
       [:div {:id "container"}]
       [:script {:type "text/javascript" :src "app.js"}]])))

(defn -main "For `lein run`, etc."
  []
  (reset! atoms/app-handler landing-pg-handler)
  (files/load-edn-file (files/resolve-file "data" "accounts" nil "edn") atoms/accounts)
  (sente-server/start! 3001))
