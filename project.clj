(defproject
  boot-project
  "0.0.0-SNAPSHOT"
  :dependencies
  [[org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
   [org.clojure/core.async "0.3.442"]
   [org.clojure/clojurescript "1.9.473"]
   [adzerk/boot-cljs "2.0.0" :scope "test"]
   [org.clojure/tools.nrepl "0.2.12"]
   [com.taoensso/sente "1.11.0"]
   [com.taoensso/timbre "4.7.4"]
   [http-kit "2.2.0"]
   [ring "1.6.1"]
   [ring/ring-defaults "0.3.0"]
   [compojure "1.6.0"]
   [reagent "0.7.0"]
   [reagent-utils "0.2.1"]
   [com.cognitect/transit-clj "0.8.290"]
   [com.cognitect/transit-cljs "0.8.239"]
   [boot/core "2.6.0" :scope "compile"]]
  :repositories
  [["clojars" {:url "https://clojars.org/repo/"}]
   ["maven-central" {:url "https://repo1.maven.org/maven2"}]]
  :source-paths
  ["src" "resources"])