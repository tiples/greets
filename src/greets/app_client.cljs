(ns greets.app-client
  (:require [greets.sente-client :as client :refer [->output! chsk-send! chsk chsk-state]]
            [clojure.string :as str]
            [reagent.core :as reagent :refer [atom]]
            [taoensso.sente :as sente :refer (cb-success?)]))

(defn calling-component
      []
      (let [hdn (.getElementById js/document "fudge")
            atts (.-attributes hdn)]
           (last (for [i (range (.-length atts))]
                      (.log js/console (str (.-name (aget atts i)) " "
                                               (.-value (aget atts i))))))
           )
      [:p "Ribits"])

(defn start!
      []
      (client/start!)
      (reagent/render-component [calling-component]
                                (.getElementById js/document "container")))
