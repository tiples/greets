(ns greets.atoms-client
  (:require [reagent.core :as reagent :refer [atom]]))

(def user-account (atom nil))
(def email-address (atom nil))
(def token-status-message (atom nil))
(def account-status-message (atom nil))
