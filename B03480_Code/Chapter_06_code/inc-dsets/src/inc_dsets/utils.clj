
(ns inc-dsets.utils
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.string :as str]
            [incanter.core :as i]))

#_
(require '[clojure.java.io :as io]
         '[clojure.data.csv :as csv]
         '[clojure.string :as str]
         '[incanter.core :as i])

#_
(require '[incanter.core :as i]
         '[incanter.io :as i-io])

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

(defn with-header [coll]
  (let [headers (map #(keyword (str/replace % \space \-))
                     (first coll))]
    (map (partial zipmap headers) (next coll))))

(defn read-country-data [filename]
  (with-open [r (io/reader filename)]
    (i/to-dataset
      (doall (with-header
               (drop 2 (csv/read-csv r)))))))

(defn read-accident-data [filename]
  (with-open [r (io/reader filename)]
    (doall (with-header
             (map #(str/split % #" \| ")
                  (line-seq r))))))

(comment
(def data-file "data/chn/chn_Country_en_csv_v2.csv")
(def chn-data (read-country-data data-file))
  )

(comment
(def data-file "data/all_160.P3.csv")
(def race-data (i-io/read-dataset data-file :header true))
  )
