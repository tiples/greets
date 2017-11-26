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

(defn got-login-token
  [token]
  [:p (str "token=" token)])

(defn calling-component
  []
  (reagent/with-let
    [token-element (.getElementById js/document "token")
     token (.getAttribute token-element "value")
     token-status-message-element (.getElementById js/document "tokenStatusMessage")
     token-status-message (.getAttribute token-status-message-element "value")]
    (reset! atoms/token-status-message token-status-message)
    (if (some? token)
      [got-login-token token]
      [get-login-token])))

(defn start!
  []
  (client/start!)
  (reagent/render-component [calling-component]
                            (.getElementById js/document "container")))
