(ns greets.app-server
  (:require
    [clojure.edn :as edn]
    [hiccup.core :as hiccup]
    [postal.core :as postal]
    [greets.sente-server :as sente-server]
    [greets.atoms-server :as atoms]
    [greets.files :as files]
    [greets.login-tokens :as login-tokens]))

(defmethod sente-server/-event-msg-handler :chsk/uidport-close ;todo close session, if any
  [ev-msg]
  ())

(defn valid-token-request
  [client-id email-address]
  (let [account (get @atoms/email-addresses email-address)
        token (login-tokens/make-token account)]
    (println account token)
    (sente-server/chsk-send! client-id
                             [:login-token/token-status-message
                              {:value "Sending the login token. Please check your emails."}])))

(defmethod sente-server/-event-msg-handler :login-token/request
  [ev-msg]
  (let [client-id (:client-id ev-msg)
        ?data (:?data ev-msg)
        email-address (:email-address ?data)
        email-addresses @atoms/email-addresses
        valid-email-address (contains? email-addresses email-address)]
    (if valid-email-address
      (valid-token-request client-id email-address)
      (sente-server/chsk-send! client-id
                               [:login-token/token-status-message
                                {:value "Unknown email address."}]))))

(defn landing-pg-handler
  [ring-req]
  (let [params (:params ring-req)
        token (:token params)
        token-status-message (if (nil? token)
                               (str "Please enter your registered email address "
                                    "(above) to receive a one-time login token.")
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

(defn register-email-addresses
  []
  (reset! atoms/email-addresses
          (reduce
            (fn [email-addresses e]
              (let [account (key e)
                    email-address (:email (val e))]
                (assoc email-addresses email-address account)))
            {}
            (:accounts @atoms/accounts))))

(defn -main "For `lein run`, etc."
  []
  (reset! atoms/app-handler landing-pg-handler)
  (files/load-edn-file (files/resolve-file "data" "accounts" nil "edn") atoms/accounts)
  (register-email-addresses)
  (login-tokens/initialize 10)                              ;max token life is 10 min
  (sente-server/start! 3001))
