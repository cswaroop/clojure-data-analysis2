
(ns concurrent-data.utils
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]))

(defn lazy-read-csv
  "This one has lazily reads from the file and generates the CSV data.

  It will leak if the sequence is not fully consumed."
  [csv-file]
  (let [in-file (io/reader csv-file)
        csv-seq (csv/read-csv in-file)
        lazy (fn lazy [wrapped]
               (lazy-seq
                 (if-let [s (seq wrapped)]
                   (cons (first s) (lazy (rest s)))
                   (.close in-file))))]
    (lazy csv-seq)))

"This takes a lazy sequency in which the header row is the first item and
uses that as keys for the rest of the collection."
(defn with-header [coll]
  (let [headers (map keyword (first coll))]
    (map (partial zipmap headers) (next coll))))

