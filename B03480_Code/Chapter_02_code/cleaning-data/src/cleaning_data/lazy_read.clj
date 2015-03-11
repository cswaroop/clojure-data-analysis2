
(ns cleaning-data.lazy-read
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]))

#_
(require '[clojure.data.csv :as csv]
         '[clojure.java.io :as io])

(defn lazy-read-bad-1 [csv-file]
  (with-open [in-file (io/reader csv-file)]
    (csv/read-csv in-file)))

(defn lazy-read-bad-2 [csv-file]
  (with-open [in-file (io/reader csv-file)]
    (doall
      (csv/read-csv in-file))))

(defn lazy-read-ok [csv-file]
  (with-open [in-file (io/reader csv-file)]
    (frequencies
      (map #(nth % 2) (csv/read-csv in-file)))))

(defn lazy-read-csv [csv-file]
  (let [in-file (io/reader csv-file)
        csv-seq (csv/read-csv in-file)
        lazy (fn lazy [wrapped]
               (lazy-seq
                 (if-let [s (seq wrapped)]
                   (cons (first s) (lazy (rest s)))
                   (.close in-file))))]
    (lazy csv-seq)))

