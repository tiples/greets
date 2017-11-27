(ns greets.app-client
  (:require [greets.sente-client :as client :refer [->output! chsk-send! chsk chsk-state]]
            [greets.atoms-client :as atoms]
            [clojure.string :as str]
            [reagent.core :as reagent :refer [atom]]
            [taoensso.sente :as sente :refer (cb-success?)]))

(defn get-login-token
  []
  [:div
   [:form
    {:on-submit
     (fn [e]
       (.preventDefault e)
       (reset! atoms/email-address (.. e -target -elements -message -value))
       (chsk-send! [:login-token/request {:email-address @atoms/email-address}]))}
    [:label "Email address: "]
    [:input {:name "message" :type "text" :autoFocus true}]
    [:input {:type "submit" :value "send"}]]
   [:p @atoms/token-status-message]])

(defmethod client/chsk-recv :login-token/token-status-message
  [id ?data]
  (reset! atoms/token-status-message (:value ?data)))

(defn get-account
  [token]
  [:div
   [:form
    {:on-submit
     (fn [e]
       (.preventDefault e)
       (reset! atoms/user-account (.. e -target -elements -message -value))
       (chsk-send! [:login-token/account {:token token
                                          :account @atoms/user-account}]))}
    [:label "Account name: "]
    [:input {:name "message" :type "text" :autoFocus true}]
    [:input {:type "submit" :value "send"}]]
   [:p @atoms/account-status-message]])

(defmethod client/chsk-recv :login-token/account-status-message
  [id ?data]
  (reset! atoms/account-status-message (:value ?data)))

(defn logged-in
  []
  [:p (str @atoms/user-account)])

(defn calling-component
  []
  (reagent/with-let
    [token-element (.getElementById js/document "token")
     token (.getAttribute token-element "value")
     _ (reset! atoms/token-status-message (str "Please enter your registered email address "
                                               "(above) to receive a one-time login token."))
     _ (reset! atoms/account-status-message "Please enter your account name to complete the login.")]
    (if (nil? token)
      [get-login-token]
      (if (some? @atoms/account-status-message)
        [get-account token]
        [logged-in]))))

(defn start!
  []
  (client/start!)
  (reagent/render-component [calling-component]
                            (.getElementById js/document "container")))
