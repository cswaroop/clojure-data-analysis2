
(ns statim.smoothing
  (:require [incanter.core :as i]
            [incanter.stats :as s]
            [incanter.charts :as c]
            [clojure.string :as str]))

#_
(require '[incanter.core :as i]
         '[incanter.stats :as s]
         '[incanter.charts :as c]
         '[clojure.string :as str])

(def data-file "data/pg1661.txt")

(defn tokenize [text]
  (map str/lower-case (re-seq #"\w+" text)))

(defn count-hits [x coll]
  (get (frequencies coll) x 0))

;; From statim.core.
(defn rolling-fn [f n coll]
  (map f (partition n 1 coll)))

(def windows
  (partition 500 250 (tokenize (slurp data-file))))
(def baker-hits
  (map (partial count-hits "baker") windows))
(def baker-avgs (rolling-fn s/mean 10 baker-hits))

(def chart
  (doto
    (c/xy-plot (range (count baker-hits)) baker-hits
               :title "Counts of 'Baker'"
               :x-label "Window (500 tokens)"
               :y-label "Frequency"
               :series-label "Raw frequencies"
               :legend true)
    (c/add-lines (range (count baker-avgs)) baker-avgs
                 :series-label "10-window rolling average")
    i/view))

(def indexed-windows (map vector (range) windows))
(def freq-dist (frequencies baker-hits))
(def freq-index
  (reduce
    (fn [m [hits window]] (assoc m hits (conj (m hits) window)))
    (zipmap (keys freq-dist) (repeatedly (constantly [])))
    (map vector baker-hits indexed-windows)))

(def the-hits (map (partial count-hits "the") windows))
(def the-avgs (rolling-fn s/mean 10 the-hits))
(def the-chart
  (doto
    (c/xy-plot (range (count the-hits)) the-hits
               :title "Counts of 'the'"
               :x-label "Window (500 tokens)"
               :y-label "Frequency"
               :series-label "Raw frequencies"
               :legend true)
    (c/add-lines (range (count the-avgs)) the-avgs
                 :series-label "10-window rolling average")
    i/view))

