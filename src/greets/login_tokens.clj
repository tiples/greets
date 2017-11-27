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
                (let [account (key e)
                      m (val e)
                      expires (:expires m)]
                  (if (> tm expires)
                    login-tokens
                    (assoc login-tokens account m))))
              {}
              @atoms/login-tokens))))

(defn get-token
  [account]
  (purge-tokens)
  (get-in @atoms/login-tokens [account :token]))

(defn remove-token
  [account]
  (let [token (get-token account)]
    (swap! atoms/login-tokens dissoc account)
    token))

(defn make-token
  [account]
  (purge-tokens)
  (let [token (uuid)]
    (swap! atoms/login-tokens assoc account {:token   token
                                             :expires (+ (time-millis) @atoms/max-token-life-millis)})
    token))

(defn initialize
  [max-token-life-min]
  (reset! atoms/max-token-life-millis (* max-token-life-min 60 1000))
  (reset! atoms/login-tokens {}))
