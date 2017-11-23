(ns greets.app-client
    (:require [greets.sente-client :as client :refer [->output! chsk-send! chsk chsk-state]]
      [clojure.string :as str]
      [reagent.core :as reagent :refer [atom]]
      [taoensso.sente :as sente :refer (cb-success?)]))

(defn start!
      []
      (client/start!))
