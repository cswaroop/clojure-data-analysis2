
(ns getting-data.read-json
  (:use [incanter.core]
        [clojure.data.json]))

(def data-file "data/small-sample.json")

(defn load-data
  "This loads data from a JSON file."
  [json-file]
  (to-dataset (read-json (slurp json-file))))

(require '[incanter.core :as i]
         '[clojure.data.json :as json]
         '[clojure.java.io :as io])
(import '[java.io EOFException])

(defn test-eof [reader f]
  (try
    (f reader)
    (catch EOFException e
      nil)))

(defn read-all-json [reader]
  (loop [accum []]
    (if-let [record (test-eof reader json/read)]
      (recur (conj accum record))
      accum)))

#_
(def d (i/to-dataset
         (with-open [r (io/reader "data/delicious-rss-214k.json")]
           (read-all-json r))))

