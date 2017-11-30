(ns greets.files
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.string :as str])
  (:import (java.text SimpleDateFormat)
           (java.util Date TimeZone)))

(defn file-timestamp
  []
  (let [formatter (SimpleDateFormat. "yywwu_HHmmss")
        _ (.setTimeZone formatter (TimeZone/getTimeZone "GMT"))
        now (Date.)]
    (.format formatter now)))

;To access a file, you need:
;1. the folder it is in
;2. the optional prefix
;3. the optional base file name
;4. the optional file label
;5. the file suffix
;6. the file extension.
;examples:
;resources-exp/Universe/Inputs/Deckapp- baseline-1736b.csv
;1. folder resources-exp/Universe/Inputs
;2. prefix Deckapp
;4. label baseline
;5. file suffix 1736b
;6. file extension csv
;resources-exp/Decks/Sights/S1_D1_input/Decktype-picture_description-1736c.csv
;1. folder resources-exp/Decks/Sights/S1_D1_input
;2. prefix Decktype
;3. base file name picture_description
;5. file suffix 1736c
;6. file extension csv
;resources-exp/Decks/Sights/S2_D2_input/Decktype-picture_description-1736b.csv
;1. folder resources-exp/Decks/Sights/S1_D1_input
;2. file prefix Decktype
;3. base file name picture_description
;5. file suffix 1736b
;6. file extension csv

(defn file-map
  [folder prefix base label suffix extension]
  {:folder    folder
   :prefix    prefix
   :base      base
   :label     label
   :suffix    suffix
   :extension extension})

(defn file-str
  [file-map]
  (let [folder (:folder file-map)
        prefix (:prefix file-map)
        base (:base file-map)
        label (:label file-map)
        suffix (:suffix file-map)
        extension (:extension file-map)]

    (str (if (nil? folder) "" (str folder "/"))
         (if (some? prefix)
           (str prefix "-")
           "")
         (if (some? base) base "")
         (if (some? label) (str " " label) "")
         "-"
         suffix
         "."
         extension)))

(defn parse-file
  [file-str]
  (let [i (str/last-index-of file-str "/")
        [folder r] (if (nil? i)
                     [nil file-str]
                     [(subs file-str 0 i) (subs file-str (+ i 1))])
        ldot (str/last-index-of r ".")
        extension (subs r (+ ldot 1))
        r (subs r 0 ldot)
        lh (str/last-index-of r "-")
        suffix (subs r (+ lh 1))
        r (subs r 0 lh)
        fh (str/index-of r "-")
        [prefix r] (if (some? fh)
                     [(subs r 0 fh) (subs r (+ fh 1))]
                     [nil r])
        fu (str/index-of r " ")
        [base label] (if (nil? fu)
                       [r nil]
                       [(subs r 0 fu) (subs r (+ fu 1))])
        base (if (empty? base) nil base)]
    (file-map folder prefix base label suffix extension)))

(defn get-file-name-
  [folder pre post]
  (let [names (.list (io/as-file folder))
        file-names (reduce
                     (fn [file-names name]
                       (if (not (str/starts-with? name pre))
                         file-names
                         (if (not (str/ends-with? name post))
                           file-names
                           (let [mid (subs name (count pre) (- (count name) (count post)))
                                 i (str/index-of mid "-")]
                             (if (nil? i)
                               file-names
                               (let [fm (parse-file (str folder "/" name))]
                                 (assoc file-names (:suffix fm) fm)))))))
                     (sorted-map)
                     names)]
    file-names))

(defn resolve-file-
  [folder prefix base extension]
  (let [pre (str (if (some? prefix)
                   (str prefix "-")
                   "")
                 (if (some? base)
                   (str base " ")
                   ""))
        post (str "." extension)
        file-names (get-file-name- folder pre post)
        lst (last (seq file-names))]
    (if (nil? lst)
      nil
      (val lst))))

(defn resolve-file
  [folder prefix base extension]
  (let [rf (resolve-file- folder prefix base extension)]
    (if (some? rf)
      rf
      (let [pre (str (if (some? prefix)
                       (str prefix "-")
                       "")
                     (if (some? base)
                       (str base " ")
                       ""))
            post (str "." extension)]
        (println "***Error*** No file matching" (str pre "*" post) "in folder" folder)
        (throw (Exception. "Missing file"))))))

(defn load-edn-file
  [edn-filemap contentmap-atom]
  (let [edn-filename (file-str edn-filemap)
        _ (println "Loading file" edn-filename)
        edn-string (slurp edn-filename)
        edn-map (edn/read-string edn-string)]
    (reset! contentmap-atom edn-map)))