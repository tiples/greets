(ns greets.app-server
    (:require
      [clojure.edn :as edn]
      [greets.sente-server :as sente-server]
      [greets.atoms :as atoms]
      [greets.files :as files]))


(defn -main "For `lein run`, etc."
      []
      (let [accounts-filename-map (files/resolve-file "data" "accounts" nil "edn")
            accounts-filename (files/file-str accounts-filename-map)
            _ (println "Loading file" accounts-filename)
            accounts (slurp accounts-filename)
            accounts-map (edn/read-string accounts)]
           (println accounts-map))
      (sente-server/start! 3001))
