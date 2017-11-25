(ns greets.app-server
  (:require
    [clojure.edn :as edn]
    [hiccup.core :as hiccup]
    [greets.sente-server :as sente-server]
    [greets.atoms :as atoms]
    [greets.files :as files]))

(defn landing-pg-handler
  [ring-req]
  (let [params (:params ring-req)
        token (:token params)]
    (println token)
    (hiccup/html
      [:head
       [:meta {:charset "utf-8"}]
       [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
       [:meta {:name "description" :content ""}]
       [:title "greets"]]
      [:body
       [:input {:type "hidden" :id "fudge" :name "foo" :value token}]
       [:div {:id "container"}]
       [:script {:type "text/javascript" :src "app.js"}]])))

(defn -main "For `lein run`, etc."
  []
  (reset! atoms/app-handler landing-pg-handler)
  (files/load-edn-file (files/resolve-file "data" "accounts" nil "edn") atoms/accounts)
  (println @atoms/accounts)
  (sente-server/start! 3001))
