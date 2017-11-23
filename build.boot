(set-env!
  :source-paths #{"src"}
  :resource-paths #{"resources"}
  :dependencies '[
                  [org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
                  [org.clojure/core.async "0.3.442"]
                  [org.clojure/clojurescript "1.9.473"]
                  [adzerk/boot-cljs "2.0.0" :scope "test"]
                  [org.clojure/tools.nrepl "0.2.12"]
                  [com.taoensso/sente "1.11.0"]
                  [com.taoensso/timbre "4.7.4"]
                  [http-kit "2.2.0"]
                  [ring "1.6.1"]
                  [ring/ring-defaults "0.3.0"]              ; Includes `ring-anti-forgery`, etc.
                  [compojure "1.6.0"]
                  [reagent "0.7.0"]
                  [reagent-utils "0.2.1"]
                  [com.cognitect/transit-clj "0.8.290"]
                  [com.cognitect/transit-cljs "0.8.239"]])

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 'greets.app-server)

(deftask dev
  []
  (comp
   (watch)
   (cljs :source-map true
         :optimizations :none)
   (speak)
   (with-pass-thru _
       (greets.app-server/-main))))
