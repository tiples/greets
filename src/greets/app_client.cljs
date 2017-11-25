(ns greets.app-client
  (:require [greets.sente-client :as client :refer [->output! chsk-send! chsk chsk-state]]
            [clojure.string :as str]
            [reagent.core :as reagent :refer [atom]]
            [taoensso.sente :as sente :refer (cb-success?)]))

(defn calling-component
  []
  (let [hdn (.getElementById js/document "fudge")
        value (.getAttribute hdn "value")]
    [:p value]))

(defn start!
  []
  (client/start!)
  (reagent/render-component [calling-component]
                            (.getElementById js/document "container")))
