(ns greets.accounts
  (:require [greets.vecer :as vecer]))

(defn new-account
  [who url timestamp account email permissions]
  {:journal-entry-id :new-account
   :who who :url url :timestamp timestamp
   :account account :email email :permissions permissions})

(defmethod vecer/vec-op :new-account
  [[db transaction-state :as state]
   [op-kw journal-entry positional-args :as op]]
  (let [account (:account journal-entry)
        account-path [:value :account account]
        email (:email journal-entry)
        email-path (conj account-path :email)
        permissions (:permissions journal-entry)
        permissions-path (conj account-path :permissions)
        db (-> db
               (assoc-in email-path email)
               (assoc-in permissions-path permissions))]
    [db transaction-state]))
