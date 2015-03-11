
(ns getting-data.read-csv
  (:use [incanter.core]
        [incanter.io]))

(def data-file "data/RestAreasCombined(Ver.BJ).csv")
(def data-file-header "data/RestAreasCombined(Ver.BJ)-headers.csv")

(defn load-data
  "This loads a CSV file."
  [csv-file]
  (read-dataset csv-file))

(defn load-data-headers
  "This loads a CSV file with headers."
  [csv-file]
  (read-dataset csv-file :header true))

