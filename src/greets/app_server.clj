(ns greets.app-server
    (:require
      [greets.sente-server :as sente-server]
      [greets.atoms :as atoms]))


(defn -main "For `lein run`, etc." []
      (sente-server/start! 3001))
