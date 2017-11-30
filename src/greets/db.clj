(ns greets.db
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [greets.files :as files]))

(defmulti journal-entry-handler
          (fn [db-atom journal-entry] (:journal-entry-id journal-entry)))

(defmethod journal-entry-handler :default
  [db-atom journal-entry]
  (throw (Exception. (str "Unrecognized Journal-entry-id: " (:journal-entry-id journal-entry)))))

(defn initialize
  [db-atom folder base label value]
  (reset! db-atom {:folder folder
                   :base   base
                   :label  label
                   :value  value}))

(defn close-journal
  [db-atom]
  (swap! db-atom
         (fn [db]
           (let [journal-writer (:journal-writer db)]
             (if (nil? journal-writer)
               db
               (do
                 (.close journal-writer)
                 (dissoc db :journal-writer)))))))

(defn build-file-name
  [db-atom file-prefix]
  (let [db @db-atom
        folder (:folder db)
        base (:base db)
        label (:label db)
        suffix (files/file-timestamp)
        file-map (files/file-map folder file-prefix base label suffix "edn")]
    (files/file-str file-map)))

(defn write-ledger
  [db-atom]
  (let [ledger-file-name (build-file-name db-atom "ledger")
        value (:value @db-atom)
        edn-value (pr-str value)]
    (close-journal db-atom)
    (io/make-parents ledger-file-name)
    (with-open [ledge-writer (io/writer ledger-file-name :encoding "UTF-8")]
      (.write ledge-writer edn-value))))

(defn open-journal
  [db-atom]
  (let [journal-writer (:journal-writer @db-atom)]
    (if (some? journal-writer)
      journal-writer
      (let [journal-file-name (build-file-name db-atom "journal")
            _ (io/make-parents journal-file-name)
            journal-writer (io/writer journal-file-name :encoding "UTF-8" :append true)]
        (swap! db-atom assoc :journal-writer journal-writer)
        journal-writer))))

(defn post-journal-entry
  [db-atom journal-entry]
  (journal-entry-handler db-atom journal-entry)
  (let [journal-writer (open-journal db-atom)
        journal-entry-edn (pr-str journal-entry)]
    (doto journal-writer (.write (str journal-entry-edn "\n")) (.flush))))
