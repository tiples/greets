(ns greets.vecer)

(defmulti vec-op
          (fn [[db-value transaction-state :as state]
               [op-kw key-args positional-args :as op]]
            op-kw))

(defmethod vec-op :default
  [[db-value transaction-state :as state]
   [op-kw key-args positional-args :as op]]
  (throw (Exception. (str "Unrecognized Journal-entry-id: " (:journal-entry-id journal-entry)))))

(defn normalize-op
  [[op-kw remaining :as op]]
  (let [key-args (first remaining)
        [key-args positional-args] (if (map? key-args)
                                     [key-args (rest remaining)]
                                     [{} remaining])]
    [op-kw key-args positional-args]))

(defn eval-op
  [[db-value transaction-state :as state]
   [op-kw remaining :as op]]
  (if (= op-kw :quote)
    [state op]
    (let [[op-kw key-args positional-args :as op] (normalize-op op)
          [state key-args]
          (reduce
            (fn [[state key-args] e]
              (let [k (key e)
                    v (val e)
                    [state v] (if (vector? v) (eval-op state v) [state v])]
                [state (assoc key-args k v)]))
            [state {}]
            key-args)
          [state positional-args]
          (reduce
            (fn [[state positional-args] v]
              (if (vector? v) (eval-op state v) [state v]))
            [state []]
            positional-args)]
      (vec-op state [op-kw key-args positional-args]))))

