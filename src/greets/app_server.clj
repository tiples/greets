(ns greets.app-server
  (:require
    [clojure.edn :as edn]
    [hiccup.core :as hiccup]
    [postal.core :as postal]
    [greets.sente-server :as sente-server]
    [greets.atoms-server :as atoms]
    [greets.files :as files]
    [greets.login-tokens :as login-tokens]
    [greets.db :as db]))

(defn send-email
  [email-address content]
  (try
    (postal/send-message (:connection @atoms/postal)
                         {:from    (get-in @atoms/postal [:connection :user])
                          :to      email-address
                          :subject "login token"
                          :body    [{:type    "text/html"
                                     :content content}]})
    (catch Exception e
      (println (.getMessage e))
      {:error :Unknown-host})))

(defn valid-token-request
  [client-id email-address origin]
  (let [account-kw (get @atoms/email-addresses email-address)
        token (login-tokens/make-token account-kw)
        html-content [:html
                      [:body
                       [:a {:href (str origin "/?token=" token)} "Click here to login."]
                       ]]
        content (hiccup/html html-content)
        postal-response (send-email email-address content)
        success (= (:error postal-response) :SUCCESS)]
    (if (not success) (println postal-response))
    (sente-server/chsk-send! client-id
                             [:login-token/token-status-message
                              {:value (if success
                                        "Sending the login token. Please check your emails."
                                        "There was an error encountered when sending the login token")}])))

(defmethod sente-server/-event-msg-handler :login-token/request
  [ev-msg]
  (let [client-id (:client-id ev-msg)
        ?data (:?data ev-msg)
        email-address (:email-address ?data)
        email-addresses @atoms/email-addresses
        valid-email-address (contains? email-addresses email-address)
        ring-req (:ring-req ev-msg)
        headers (:headers ring-req)
        origin (get headers "origin")]
    (if valid-email-address
      (valid-token-request client-id email-address origin)
      (sente-server/chsk-send! client-id
                               [:login-token/token-status-message
                                {:value "Unknown email address."}]))))

(defn login-failure
  [client-id token]
  (sente-server/chsk-send! client-id
                           [:login-token/reset-status-messages
                            {:value "Login failure. Token invalidated."}]))

(defn login
  [client-id account-kw]
  (swap! atoms/sessions assoc client-id {:account account-kw})
  (sente-server/chsk-send! client-id
                           [:login-token/account-status-message
                            {:value nil}]))

(defmethod sente-server/-event-msg-handler :chsk/uidport-close
  [ev-msg]
  (let [client-id (:client-id ev-msg)]
    (swap! atoms/sessions dissoc client-id)))

(defmethod sente-server/-event-msg-handler :login-token/account
  [ev-msg]
  (let [client-id (:client-id ev-msg)
        ?data (:?data ev-msg)
        token (:token ?data)
        account (:account ?data)
        account-kw (keyword account)
        aacount-kw (login-tokens/remove-token token)]
    (if (= account-kw aacount-kw)
      (login client-id account-kw)
      (login-failure client-id token))))

(defn landing-pg-handler
  [ring-req]
  (let [params (:params ring-req)
        token (:token params)]
    (hiccup/html
      [:head
       [:meta {:charset "utf-8"}]
       [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
       [:meta {:name "description" :content ""}]
       [:title "greets"]]
      [:body
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
            (get-in @atoms/accounts-db
                    [:value :App :has_application_rolon :Accounts :has_member]))))

(defn -main "For `lein run`, etc."
  []
  (reset! atoms/app-handler landing-pg-handler)
  (files/load-edn-file (files/resolve-file "private-data" "postal" nil "edn") atoms/postal)
  (db/initialize! atoms/accounts-db "dbs/accounts" "accounts" "initial" {})
  (db/load-db! atoms/accounts-db)
  (register-email-addresses)
  (login-tokens/initialize 10)                              ;max token life is 10 min
  (sente-server/start! 3001))
