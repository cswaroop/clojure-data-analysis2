
(ns webviz.histogram
  (:require [webviz.core :as webviz]))

(defn get-bucket-number [mn size x]
  (Math/round (/ (- x mn) size)))
(defn inc-bucket [mn size buckets x]
  (let [b (get-bucket-number mn size x)]
    (assoc buckets b (inc (buckets b)))))
(defn get-buckets [coll n]
  (let [mn (reduce min coll)
        mx (reduce max coll)
        bucket-size (/ (- mx mn) n)
        first-center (+ mn (/ bucket-size 2.0))
        centers (map #(* (inc %) first-center)
                     (range n))
        initial (reduce #(assoc %1 %2 0) {}
                        (range n))]
    (->> coll
      (reduce (partial inc-bucket mn bucket-size)
              initial)
      seq
      (sort-by first)
      (map second)
      (map vector centers))))

(defn ->point [pair]
  (let [[bucket count] pair]
    (webviz/Point. (inc bucket) count 1)))

(defn data->nv-groups [data]
  (aset js/window "hist_data" data)
  (let [lengths (map #(.-length %) data)
        buckets (apply array
                       (map ->point
                            (get-buckets lengths 10)))]
    (array (webviz/Group. "Abalone Lengths" buckets))))

(defn make-chart [] (.multiBarChart (.-models js/nv)))

(defn ^:export histogram []
  (webviz/create-chart
    "/histogram/data.json"
    "#histogram svg"
    make-chart
    data->nv-groups
    :transition true))

; vim: set filetype=clojure:
