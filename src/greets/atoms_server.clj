(ns greets.atoms-server)

(def app-handler (atom nil))
(def accounts-db (atom nil))
(def email-addresses (atom nil))
(def login-tokens (atom nil))
(def max-token-life-millis (atom nil))
(def postal (atom nil))
(def sessions (atom nil))
