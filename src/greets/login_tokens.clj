(ns greets.login-tokens
  (:require
    [greets.atoms-server :as atoms]))

(defn uuid [] (.toString (java.util.UUID/randomUUID)))

(defn time-millis [] (System/currentTimeMillis))

(defn purge-tokens
  []
  (let [tm (time-millis)]
    (reset! atoms/login-tokens
            (reduce
              (fn [login-tokens e]
                (let [token (key e)
                      m (val e)
                      expires (:expires m)]
                  (if (> tm expires)
                    login-tokens
                    (assoc login-tokens token m))))
              {}
              @atoms/login-tokens))))

(defn get-account
  [token]
  (purge-tokens)
  (get-in @atoms/login-tokens [token :account]))

(defn remove-token
  [token]
  (let [account (get-account token)]
    (swap! atoms/login-tokens dissoc token)
    account))

(defn make-token
  [account]
  (purge-tokens)
  (let [token (uuid)]
    (swap! atoms/login-tokens assoc token {:account account
                                           :expires (+ (time-millis) @atoms/max-token-life-millis)})
    token))

(defn initialize
  [max-token-life-min]
  (reset! atoms/max-token-life-millis (* max-token-life-min 60 1000))
  (reset! atoms/login-tokens {}))
