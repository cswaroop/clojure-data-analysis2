
;;;; Recipes
;;;; * 07.05 -- Differencing variables
(ns statim.core
  (:require [incanter.core :as i]
            incanter.io
            [incanter.stats :as s]
            [incanter.charts :as c]
            [incanter.zoo :as zoo]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.lang Math]
           [org.apache.commons.collections.buffer CircularFifoBuffer]))

#_
(require '[incanter.core :as i]
         '[incanter.stats :as s]
         'incanter.io
         '[incanter.charts :as c]
         '[incanter.zoo :as zoo]
         '[clj-time.format :as tf])
         '[clojure.string :as string]
         '[clojure.data.csv :as csv]
         '[clojure.java.io :as io]
         '[clj-time.core :as t])
#_
(import [java.lang Math]
        [java.io File]
        [org.apache.commons.collections.buffer CircularFifoBuffer])

;;; 07.05 -- Differencing variables
(def data-file "data/all_160_in_51.P3.csv")
(def data (incanter.io/read-dataset data-file :header true))
(defn check-int [x] (if (integer? x) x 0))
(def growth-rates
  (->> data
    (i/$map check-int :POP100.2000)
    (i/minus (i/sel data :cols :POP100))
    (i/dataset [:POP.DELTA])
    (i/conj-cols data)))

#_
(i/sel growth-rates
       :cols [:NAME :POP100 :POP100.2000 :POP.DELTA]
       :rows (range 5))

;;; 07.06 -- Smoothing variables
;; For this, we'll use a moving average.
(def ^:dynamic *formatter* (tf/formatter "dd-MMM-yy"))
(defn parse-date [date] (tf/parse *formatter* date))

(def data-file "data/ibm.csv")
(def data
  (i/add-derived-column
    :date [:date-str] parse-date
    (i/col-names
      (incanter.io/read-dataset data-file)
      [:date-str :open :high :low :close :volume])))

(defn rolling-fn
  [f n coll]
  (map f (partition n 1 coll)))

(comment

(i/sel data :cols :close :rows (range 5))
(take 5 (rolling-fn s/mean 5 (i/sel data :cols :close)))

  (def chart
    (let [date (i/sel data :cols 0)
          closing (i/sel data :cols :Close)
          running-avg-5 (moving-average 5 closing)
          running-avg-30 (moving-average 30 closing)]
      (doto
        (c/line-chart date closing
                      :title "NYSE:IBM"
                      :x-label "Date"
                      :y-label "Closing"
                      :legend true)
        (c/add-categories date running-avg-5
                          :series-label "5-day Running Average")
        (c/add-categories date running-avg-30
                          :series-label "30-day Running Average"))))
  (i/view chart)
  )

;;; 07.07 -- Scaling variables
(comment
(def data-file "data/chn-years.csv")
(def data (incanter.io/read-dataset data-file :header true))

;; First, we'll look at the raw data.
(def ag-land (i/$where {:Indicator-Code {:$in
                                         #{"AG.LND.AGRI.K2"
                                           "AG.SRF.TOTL.K2"
                                           "AG.LND.AGRI.ZS"}}}
                       data))

  ;; This needs to rotate from [:Indicator-Code :Year :Value] to
  ;; [:Year :AG.LND.AGRI.K2 :AG.LND.AGRI.ZS :AG.SRF.TOTL.K2]
(defn rotate-col [rotate-key rotate-val value-key keep-cols dataset]
  (let [value-keyword (keyword rotate-val)]
    ; (println \tab value-key '=> value-keyword)
    (->> dataset
         (i/$where {rotate-key rotate-val})
         (i/rename-cols {value-key value-keyword})
         (i/$ (conj keep-cols value-keyword)))))

(defn rotate-on [rotate-key value-key dataset drop-cols]
  (let [keep-cols (filter (apply disj (set (i/col-names dataset))
                                 rotate-key value-key drop-cols)
                          (i/col-names dataset))]
    (println keep-cols)
    (->> dataset
         (i/$ rotate-key)
         (pmap #(rotate-col rotate-key % value-key keep-cols dataset))
         (reduce #(i/$join [keep-cols keep-cols] %1 %2)))))

(def d2 (rotate-on :Indicator-Code :Value ag-land [:Indicator-Name :Country-Name]))
(count d2)

(with-open [f-out (io/writer "data/chn-land.csv")]
  (csv/write-csv f-out [(map name (i/col-names d2))])
  (csv/write-csv f-out (i/to-list d2)))

(def data-file "data/chn-land.csv")
(def data (incanter.io/read-dataset data-file :header true))

(def data
  (->> data
       (i/$where {:AG.LND.AGRI.K2 {:$ne nil},
                  :AG.SRF.TOTL.K2 {:$ne nil}})
       (i/add-derived-column :AGRI.PRC
                             [:AG.LND.AGRI.K2 :AG.SRF.TOTL.K2]
                             #(float (/ %1 %2)))))

(def geo-ids (i/sel data :cols :GEOID))
(def names (i/sel data :cols :NAME))
(def chart
  (let [raw-population (i/sel data :cols :POP100)]
    (c/line-chart geo-ids raw-population
                  :legend true
                  :series-label "Raw Population")))
(defn save-chart
  ([chart filename]
   (with-open [s (io/output-stream (File. filename))]
     (i/save chart s))))
;; Second, we'll add it scaled by 1,000s.
(def data
  (->> (i/div (i/sel data :cols :POP100) 1000.0)
    (i/dataset [:POP100.1000])
    (i/conj-cols data)))
(def chart
  (let [population-1000 (i/sel data :cols :POP100.1000)]
    (c/line-chart
      geo-ids population-1000
      :series-label "Population in 1,000's")))
;; Third, we'll look at it on a log scale.
(def data
  (->> (i/sel data :cols :POP100)
    i/log
    (i/dataset [:POP100.LOG])
    (i/conj-cols data)))
(def data
  (->> (i/sel data :cols :POP100)
    i/log2
    (i/dataset [:POP100.LOG2])
    (i/conj-cols data)))
(def data
  (->> (i/sel data :cols :POP100)
    i/log10
    (i/dataset [:POP100.LOG10])
    (i/conj-cols data)))
(def chart
  (let [pop-log10 (i/sel data :cols :POP100.LOG10)]
    (c/line-chart
      geo-ids pop-log10
      :series-label "Population (log-10)"
      :legend true)))
(def chart
  (let [pop-log (i/sel data :cols :POP100.LOG)
        pop-log2 (i/sel data :cols :POP100.LOG2)
        pop-log10 (i/sel data :cols :POP100.LOG10)]
    (doto
      (c/line-chart
        geo-ids pop-log
        :series-label "Population (log scale)"
        :legend true)
      (c/add-categories
        geo-ids pop-log2
        :series-label "Population (log-2 scale)")
      (c/add-categories
        geo-ids pop-log10
        :series-label "Population (log-10 scale)"))))
;; Let's look at all of them now.
(def chart
  (let [geo-ids (i/sel data :cols :GEOID)
        pop-raw (i/sel data :cols :POP100)
        pop-1000 (i/sel data :cols :POP100.1000)
        pop-log10 (i/sel data :cols :POP100.LOG10)]
    (doto
      (c/line-chart
        geo-ids pop-raw
        :legend true
        :series-label "Population (raw)")
      (c/add-categories
        geo-ids pop-1000
        :series-label "Population (in 1000s)")
      (c/add-categories
        geo-ids pop-log10
        :series-label "Population (log-10)"))))
  )

;;; 07.08 -- Testing values with Benford's law
(comment

(def data-file "data/all_160_in_51.P35.csv")
(def data (incanter.io/read-dataset data-file :header true))
(def bt (s/benford-test (i/sel data :cols :POP100)))
(:X-sq bt)
(:df bt)
(:p-value bt)
(def chart
  (let [digits (map inc (:row-levels bt))
        frequency (:table bt)]
    (i/view (c/bar-chart digits frequency))))
(save-chart chart "2643OS_07_06.png")
;; user=> (:X-sq bt)
;; 15.74894048668777
;; user=> (:df bt)
;; 8
;; user=> (:p-value bt)
;; 0.046117795289705776

(def bt-bad
  (s/benford-test
    (map (fn [_] (rand-int Integer/MAX_VALUE)) (range 1000))))
(:X-sq bt-bad)
(:df bt-bad)
(:p-value bt-bad)
(i/view (c/bar-chart (:row-levels bt-bad) (:table bt-bad)))
(save-chart (c/bar-chart (:row-levels bt-bad) (:table bt-bad))
            "2643OS_07_09.png")
;; user=> (:X-sq bt-bad)
;; 286.62825246572424
;; user=> (:df bt-bad)
;; 8
;; user=> (:p-value bt-bad)
;; 2.879412752448083E-57
;;; So interesting. The random data is actually *better* than the
;;; populations. Let's try something else.

(doseq [col (i/col-names data)]
  (let [rows (filter #(or (instance? Integer %) (instance? Long %))
                     (i/sel data :cols col))]
    (when-not (zero? (count rows))
      (println col :=> (:p-value (s/benford-test rows))))))

;; The housing unit column doesn't conform.
(def bt-bad (s/benford-test (i/sel data :cols :HU100)))
(:X-sq bt-bad)
(:df bt-bad)
(:p-value bt-bad)
(def chart
  (let [digits (map inc (:row-levels bt-bad))
        frequency (:table bt-bad)]
    (i/view (c/bar-chart digits frequency))))
;; user=> (:X-sq bt-bad)
;; 7.162968432776601
;; user=> (:df bt-bad)
;; 8
;; user=> (:p-value bt-bad)
;; 0.5191561695202658

  )

;;; 07.09 --  Using incanter.zoo with timeseries.
;;; I may want to revisit 07.06 after this, since
;;; incanter.zoo/roll-mean exists.
(comment
;; Load the IBM stock-price data from 07.06.
(def data-zoo (zoo/zoo data :date))

(def data-roll5
  (->>
    (i/sel data-zoo :cols :close)
    (zoo/roll-mean 5)
    (i/dataset [:five-day])
    (i/conj-cols data-zoo)))

(def chart
  (let [data-zoo (i/sel data-zoo :rows (drop 2523 (range 2783)))
        date (i/sel data-zoo :cols :index)
        closing (i/sel data-zoo :cols :close)
        roll-5 (zoo/roll-mean 5 closing)
        roll-30 (zoo/roll-mean 30 closing)]
    (doto
      (c/line-chart date closing
                    :title "NYSE:IBM (Zoo)"
                    :x-label "Date"
                    :y-label "Closing"
                    :legend true)
      (c/add-categories date roll-5
                        :series-label "5-day Running Mean")
      (c/add-categories date roll-30
                        :series-label "30-day Running Mean"))))
(def data-zoo (->> (i/sel data-zoo :cols :close)
                (zoo/roll-mean 5)
                (i/dataset [:5day])
                (i/conj-cols data-zoo)))
(def data-zoo (->> (i/sel data-zoo :cols :cLose)
                (zoo/roll-mean 30)
                (i/dataset [:30day])
                (i/conj-cols data-zoo)))
(i/sel data-zoo :cols [:index :close :5day :30day]
       :rows (drop (- (i/nrow data-zoo) 10)
                   (range (i/nrow data-zoo))))
  )

;;; 07.10 -- Bootstrap re-sampling
(comment
(def data-file "data/all_160_in_51.P35.csv")
(def data (incanter.io/read-dataset data-file :header true))

(def pop100 (i/sel data :cols :POP100))
(def samples (s/bootstrap pop100 s/median :size 2000))

(s/median pop100)
(s/median samples)

(i/view (c/histogram samples))
(i/view (c/histogram pop100))
  )

