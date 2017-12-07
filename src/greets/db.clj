(ns greets.db
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [greets.files :as files]
    [greets.vecer :as vecer]))

(defn initialize!
  [db-atom folder base label value]
  (reset! db-atom {:folder folder
                   :base   base
                   :label  label
                   :value  value}))

(defn close-journal!
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

(defn write-ledger!
  [db-atom]
  (let [ledger-file-name (build-file-name db-atom "ledger")
        value (:value @db-atom)
        edn-value (pr-str value)]
    (close-journal! db-atom)
    (io/make-parents ledger-file-name)
    (with-open [ledge-writer (io/writer ledger-file-name :encoding "UTF-8")]
      (.write ledge-writer edn-value))))

(defn open-journal!
  [db-atom]
  (let [journal-writer (:journal-writer @db-atom)]
    (if (some? journal-writer)
      journal-writer
      (let [journal-file-name (build-file-name db-atom "journal")
            _ (io/make-parents journal-file-name)
            journal-writer (io/writer journal-file-name :encoding "UTF-8" :append true)]
        (swap! db-atom assoc :journal-writer journal-writer)
        journal-writer))))

(defn post-journal-entry!
  [db-atom journal-entry]
  (swap! db-atom (fn [db]
                   (first
                     (vecer/eval-op
                       [db {}]
                       [(:journal-entry-id journal-entry) journal-entry])))))

(defn write-journal-entry!
  [db-atom journal-entry]
  (post-journal-entry! db-atom journal-entry)
  (let [journal-writer (open-journal! db-atom)
        journal-entry-edn (pr-str journal-entry)]
    (doto journal-writer (.write (str journal-entry-edn "\n")) (.flush))))

(defn load-ledger!
  [db-atom]
  (swap! db-atom
         (fn [db]
           (let [folder (:folder db)
                 base (:base db)
                 file-map (files/resolve-file folder "ledger" base "edn")
                 label (:label file-map)
                 db (assoc db :label label)
                 suffix (:suffix file-map)
                 db (assoc db :suffix suffix)
                 value (files/load-edn-file file-map)
                 db (assoc db :value value)]
             db))))

(defn load-journal!
  [db-atom journal-filename]
  (println "Loading file" journal-filename)
  (with-open [journal-reader (io/reader journal-filename :encoding "UTF-8")]
    (last (for [journal-entry-string (line-seq journal-reader)]
            (let [journal-entry (edn/read-string journal-entry-string)]
              (post-journal-entry! db-atom journal-entry))))))

(defn load-db!
  [db-atom]
  (load-ledger! db-atom)
  (let [db @db-atom
        folder (:folder db)
        base (:base db)
        files (files/resolve-files folder "journal" base "edn")
        ledger-suffix (:suffix db)
        last-journal-mapentry (last files)
        last-suffix (if (nil? last-journal-mapentry)
                      nil
                      (key last-journal-mapentry))]
    (last (for [e files]
      (let [journal-suffix (key e)
            journal-filemap (val e)]
        (if (> (compare journal-suffix ledger-suffix) 0)
          (load-journal! db-atom (files/file-str journal-filemap))))))
    (if (> (compare last-suffix ledger-suffix) 0)
      (write-ledger! db-atom))))
