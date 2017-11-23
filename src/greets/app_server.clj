(ns greets.app-server
    (:require [greets.sente-server :as sente-server]))


(defn -main "For `lein run`, etc." []
      (sente-server/start! 3001))
