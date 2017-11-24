(ns greets.app-server
    (:require
      [clojure.edn :as edn]
      [greets.sente-server :as sente-server]
      [greets.atoms :as atoms]
      [greets.files :as files]))


(defn -main "For `lein run`, etc."
      []
      (files/load-edn-file (files/resolve-file "data" "accounts" nil "edn") atoms/accounts)
      (println @atoms/accounts)
      (sente-server/start! 3001))
