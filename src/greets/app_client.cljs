(ns greets.app-client
  (:require [greets.sente-client :as client :refer [->output! chsk-send! chsk chsk-state]]
            [clojure.string :as str]
            [reagent.core :as reagent :refer [atom]]
            [taoensso.sente :as sente :refer (cb-success?)]))

(defn calling-component
  []
  (let [token-status-message-element (.getElementById js/document "tokenStatusMessage")
        token-status-message (.getAttribute token-status-message-element "value")
        token-element (.getElementById js/document "token")
        token (.getAttribute token-element "value")]
    [:div
     (if (some? token-status-message) [:p token-status-message])
     ]))

(defn start!
  []
  (client/start!)
  (reagent/render-component [calling-component]
                            (.getElementById js/document "container")))
