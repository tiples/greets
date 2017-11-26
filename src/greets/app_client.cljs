(ns greets.app-client
  (:require [greets.sente-client :as client :refer [->output! chsk-send! chsk chsk-state]]
            [greets.atoms-client :as atoms]
            [clojure.string :as str]
            [reagent.core :as reagent :refer [atom]]
            [taoensso.sente :as sente :refer (cb-success?)]))

(defn get-login-token
  []
  (let [token-status-message-element (.getElementById js/document "tokenStatusMessage")
        token-status-message (.getAttribute token-status-message-element "value")]
    [:div
     [:p token-status-message]]))

(defn got-login-token
  [token]
  [:p (str "token=" token)])

(defn calling-component
  []
  (let [token-element (.getElementById js/document "token")
        token (.getAttribute token-element "value")]
    (if (some? token)
      [got-login-token token]
      [get-login-token])))

(defn start!
  []
  (client/start!)
  (reagent/render-component [calling-component]
                            (.getElementById js/document "container")))
