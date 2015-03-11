
;; NB: Do this one for 03.02.
(ns concurrent-data.agents
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv])
  (:use [concurrent-data.utils]))

#_
(require '[clojure.java.io :as io]
         '[clojure.data.csv :as csv])

;; From http://census.ire.org/data/bulkdata.html
;; State: Virginia (51)
;; Summary Level: Place (160)
;; Table: P35. FAMILIES
(def data-file "data/all_160_in_51.P35.csv")

(def data-files ["data/campaign-fin/pacs90.txt"
                 "data/campaign-fin/pacs92.txt"
                 "data/campaign-fin/pacs94.txt"
                 "data/campaign-fin/pacs96.txt"
                 "data/campaign-fin/pacs98.txt"
                 "data/campaign-fin/pacs00.txt"
                 "data/campaign-fin/pacs02.txt"
                 "data/campaign-fin/pacs04.txt"
                 "data/campaign-fin/pacs06.txt"
                 "data/campaign-fin/pacs08.txt"
                 "data/campaign-fin/pacs10.txt"])

(defn sum-item
  "This sums the number of families/housing unit in the input collection."
  ([fields] (partial sum-item fields))
  ([fields accum item]
   (mapv + accum (map ->int (map item fields)))))

  "This folds sum-item over a collection."
(defn sum-items [accum fields coll]
  (reduce (sum-item fields) accum coll))

(defn ->int [i] (Integer. i))

(defn get-cid [row] (nth row 3))
(defn get-amount [row] (->int (nth row 4)))

(defn get-cid-amount [row] [(get-cid row) (get-amount row)])

(defn add-amount-by [m cid amount]
  (assoc m cid (+ amount (get m cid 0))))

(defn read-file-amounts [m filename]
  (reduce #(add-amount-by %1 (first %2) (second %2)) m
          (map get-cid-amount (lazy-read-csv filename))))

(defn force-val [a]
  (await a)
  @a)

(defn main [data-files agent-count]
  (let [agents (map agent (repeat agent-count {}))]
    (dorun
      (map #(send %1 read-file-amounts %2)
           (cycle agents)
           data-files))
    (apply merge-with + (map force-val agents))))

