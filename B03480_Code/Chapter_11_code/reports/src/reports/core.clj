
;;;; {{{ Declarations
(ns reports.core
  (:require [incanter.core :as i]
            [incanter.charts :as c]
            [incanter.excel :as xl]
            [incanter.io :as iio]
            [incanter.pdf :as pdf]
            [incanter.latex :as latex]
            [incanter.stats :as s]
            incanter.datasets
            [clj-time.format :as tf]
            [clj-time.core :as t]
            [clojure.string :as str]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io])
  (:import org.jfree.chart.renderer.category.LayeredBarRenderer
           org.jfree.util.SortOrder))

#_
(require '[incanter.core :as i]
         '[incanter.charts :as c])

(comment
(require '[incanter.core :as i]
         '[incanter.charts :as c]
         '[incanter.excel :as xl]
         '[incanter.io :as iio]
         '[incanter.latex :as latex]
         '[incanter.pdf :as pdf]
         '[incanter.stats :as s]
         'incanter.datasets
         '[clj-time.format :as tf]
         '[clj-time.core :as t]
         '[clojure.string :as str]
         '[clojure.data.csv :as csv]
         '[clojure.java.io :as io])
(import org.jfree.chart.renderer.category.LayeredBarRenderer
        org.jfree.util.SortOrder)
  )

(defn bootstrap []
  (require '[incanter.core :as i]
           '[incanter.charts :as c]
           '[incanter.excel :as xl]
           '[incanter.io :as iio]
           '[incanter.latex :as latex]
           '[incanter.pdf :as pdf]
           '[incanter.stats :as s]
           'incanter.datasets
           '[clj-time.format :as tf]
           '[clj-time.core :as t]
           '[clojure.string :as str]
           '[clojure.data.csv :as csv]
           '[clojure.java.io :as io])
  (import org.jfree.chart.renderer.category.LayeredBarRenderer
          org.jfree.util.SortOrder))

; }}}

;;;; {{{ Data

;;; {{{ Iris
(println "loading iris...")
(def iris (incanter.datasets/get-dataset :iris))
; }}}

;;; {{{ Chick Weight
(println "loading chick weight...")
(def chick-weight (incanter.datasets/get-dataset :chick-weight))
; }}}

;;; {{{ Census Race Data for All Locations
(println "loading race-data...")
(def state-codes
  {1 "Alabama", 2 "Alaska", 4 "Arizona", 5 "Arkansas",
   6 "California", 8 "Colorado", 9 "Connecticut", 10 "Delaware",
   11 "District of Columbia", 12 "Florida", 13 "Georgia",
   15 "Hawaii", 16 "Idaho", 17 "Illinois", 18 "Indiana",
   19 "Iowa", 20 "Kansas", 21 "Kentucky", 22 "Louisiana",
   23 "Maine", 24 "Maryland", 25 "Massachusetts", 26 "Michigan",
   27 "Minnesota", 28 "Mississippi", 29 "Missouri",
   30 "Montana", 31 "Nebraska", 32 "Nevada", 33 "New Hampshire",
   34 "New Jersey", 35 "New Mexico", 36 "New York",
   37 "North Carolina", 38 "North Dakota", 39 "Ohio",
   40 "Oklahoma", 41 "Oregon", 42 "Pennsylvania",
   72 "Puerto Rico", 44 "Rhode Island", 45 "South Carolina",
   46 "South Dakota", 47 "Tennessee", 48 "Texas", 49 "Utah",
   50 "Vermont", 51 "Virginia", 53 "Washington",
   54 "West Virginia", 55 "Wisconsin", 56 "Wyoming"})

(def race-data
  (let [data (iio/read-dataset "data/all_160.P3.csv"
                               :header true)]
    (->>
      data
      (i/$map state-codes :STATE)
      (map #(hash-map :STATE-NAME %))
      (i/conj-cols data))))

#_
(def race-data (iio/read-dataset "data/all_160.P3.csv"
                                 :header true))

(def va-race-data (i/$where {:STATE-NAME "Virginia"} race-data)) 

(def fields [:P003002 :P003003 :P003004 :P003005 :P003006
             :P003007 :P003008])
(def race-by-state
  (reduce #(i/$join [:STATE :STATE] %1 %2)
          (map #(i/$rollup :sum % :STATE race-data)
               fields)))

; }}}

;;; {{{ Abalone
(println "loading abalone...")
(def abalone
  (iio/read-dataset "data/abalone.data" :header true))
; }}}

;;; {{{ Mushroom
(println "loading mushroom...")
(def shrooms
  (iio/read-dataset "data/agaricus-lepiota.data" :header true))
; }}}

;;; {{{ IBM
(println "loading IBM...")
(def ^:dynamic *formatter* (tf/formatter "dd-MMM-yy"))
(defn parse-date ([date] (tf/parse *formatter* date)))
(defn clean-header
  ([s] (keyword (str/join \- (re-seq #"\w+" s)))))

(def ibm-data-file "data/ibm.csv")
(def ibm-data
  (with-open [reader (io/reader ibm-data-file)]
    (let [csv-reader (csv/read-csv reader)]
      (i/dataset (mapv clean-header (first csv-reader))
                 (sort-by first
                          (doall
                            (map (fn [[d o h l c v]]
                                   [(parse-date d)
                                    (Double/parseDouble o)
                                    (Double/parseDouble h)
                                    (Double/parseDouble l)
                                    (Double/parseDouble c)
                                    (Integer/parseInt v)])
                                 (rest csv-reader))))))))
; }}}

; }}}

;;;; {{{ 10.01 Creating Scatter Charts with Incanter

(def iris-petal-scatter
  (c/scatter-plot
    (i/sel iris :cols :Petal.Width)
    (i/sel iris :cols :Petal.Length)
    :title "Irises: Petal Width by Petal Length"
    :x-label "Width (cm)"
    :y-label "Length (cm)"))

#_
(i/view iris-petal-scatter)

; }}}

;;;; {{{ 10.02 Creating Bar Charts with Incanter

(def chick-weight-bar
  (i/with-data
    (i/$order :Diet :asc
        (i/$rollup :sum :weight :Diet chick-weight))
    (c/bar-chart (i/$map int :Diet)
                 :weight
                 :title "Chick Weight"
                 :x-label "Diet"
                 :y-label "Weight")))

#_
(i/with-data
  (i/$rollup :count :Chick :Diet chick-weight)
  (c/add-categories chick-weight-bar :Diet :Chick
                    :series-label "Count"))

#_
(i/view chick-weight-bar)

; }}}

;;;; {{{ 10.03 Plotting Non-Numeric Data in Bar Charts

(def shroom-cap-bar
  (i/with-data
    (->> shrooms
      (i/$group-by :cap-shape)
      (map (fn [[k v]] (assoc k :count (i/nrow v))))
      (sort-by :cap-shape)
      i/to-dataset)
    (c/bar-chart :cap-shape :count)))

#_
(i/view shroom-cap-bar)

; }}}

;;;; {{{ 10.04 Creating Histograms with Incanter

(def pop-hist
  (c/histogram
    (i/sel race-data :cols :POP100)
    :nbins 20
    :title "Population Distribution"
    :x-label "Population"))
#_
(i/view pop-hist)

(def abalone-hist
  (c/histogram
    (i/sel abalone :cols :length)
    :title "Abalone Size"
    :x-label "Length (mm)"))

#_
(i/view abalone-hist)

(defn species-petal-length
  [species-groups species]
  (i/sel (get species-groups {:Species species})
         :cols :Petal.Length))

(def iris-petal-length-multi-hist
  (let [species (i/$group-by :Species iris)]
    (doto
      (c/histogram (species-petal-length species "setosa")
                   :title "Iris Petal Lengths"
                   :x-label "cm"
                   :series-label "Setosa"
                   :nbins 20
                   :legend true)
      (c/add-histogram (species-petal-length species "versicolor")
                       :nbins 20 :series-label "Versicolor")
      (c/add-histogram (species-petal-length species "virginica")
                       :nbins 20 :series-label "Virginica"))))

(def iris-petal-length-hist
  (c/histogram (i/sel iris :cols :Petal.Length)
               :title "Iris Petal Lengths"
               :x-label "cm"
               :nbins 20))

#_
(i/view iris-petal-length-hist)

; }}}

;;;; {{{ 10.05 Creating Box Plots with Incanter

(def ibm-year
  (i/conj-cols
    ibm-data
    (i/$map
        #(hash-map :Year (t/year %))
        :Date ibm-data)))

(def ibm-box
  (i/with-data
    ibm-year
    (c/box-plot :Close
                :title "IBM Closing Price by Year"
                :x-label "Year"
                :y-label "Closing Price"
                :group-by :Year)))

#_
(i/view ibm-box)

; }}}

;;;; {{{ 10.06 Creating Function Plots with Incanter

(def f-plot
  (c/function-plot
    #(/ 1.0 (Math/log %)) 0.0 0.99
    :title "Inverse log function."
    :y-label "Inverse log"))
#_
(i/view f-plot)

; }}}

;;;; {{{ 10.07 Adding Equations to Incanter Charts
;; This will extend the plot from 10.06.

(def inv-log "f(x)=\\frac{1.0}{\\log x}")
(comment
(i/view (latex/latex inv-log))
; (latex/add-latex f-plot 0.3 -150 inv-log)
(latex/add-latex-subtitle f-plot inv-log)
  )

; }}}

;;;; {{{ 10.08 Adding Lines to Scatter Charts
;; Use race-data-by-state and rbs-scatter from 10.01

(def rbs-lm (s/linear-model
              (i/sel race-data-by-state :cols :P003003)
              (i/sel race-data-by-state :cols :P003002)
              :intercept false))
#_
(c/add-lines
  rbs-scatter
  (i/sel race-data-by-state :cols :P003002)
  (:fitted rbs-lm)
  :series-label "Linear Model")

;; Use iris and iris-petal-scatter from 10.01.
(def iris-petal-lm
  (s/linear-model
    (i/sel iris :cols :Petal.Length)
    (i/sel iris :cols :Petal.Width)
    :intercept false))

#_
(c/add-lines
  iris-petal-scatter
  (i/sel iris :cols :Petal.Width)
  (:fitted iris-petal-lm)
  :series-label "Linear Relationship")

; }}}

;;;; {{{ Customizing Charts with JFreeChart

(comment
(require '[incanter.core :as i]
         '[incanter.charts :as c]
         'incanter.datasets)
(import org.jfree.chart.renderer.category.LayeredBarRenderer
        org.jfree.util.SortOrder)
  )

(def iris-dimensions
  (i/with-data
    iris
    (doto (c/bar-chart :Species :Petal.Width
                       :title "iris' dimensions"
                       :x-label "species"
                       :y-label "cm"
                       :series-label "petal width"
                       :legend true)
      (c/add-categories
        :Species :Sepal.Width
        :series-label "sepal width")
      (c/add-categories
        :Species :Petal.Length
        :series-label "petal length")
      (c/add-categories
        :Species :Sepal.Length
        :series-label "sepal length"))))

(doto (.getPlot iris-dimensions)
  (.setRenderer (doto (LayeredBarRenderer.)
                 (.setDrawBarOutline false)))
  (.setRowRenderingOrder SortOrder/DESCENDING))

#_
(i/view iris-dimensions)
 
; }}}

;;;; {{{ Customizing chart colors and styles

(def colors [(java.awt.Color. 0x00 0xb3 0x66)
             (java.awt.Color. 0x80 0xff 0xc9)])

#_
(doseq [[i c] (map vector (range) colors)]
  (.. iris-petal-scatter
      getPlot
      (getRenderer i)
      (setSeriesPaint 0 c)))

#_
(i/view iris-petal-scatter)

; }}}

;;;; {{{ 10.09 Saving Incanter Graphs to PNG
#_
(i/save iris-petal-scatter "iris-petal-scatter.png")
; }}}

;;;; {{{ 10.10 Saving Incanter Graphs to PDF
#_
(pdf/save-pdf ibm-box "ibm-box.pdf")
; }}}

;;;; {{{ 10.12 Using PCA to Graph Multi-Dimensional Data
;;; http://data-sorcery.org/category/pca/

(def race-by-state-matrix (i/to-matrix race-by-state))
(def x (i/sel race-by-state-matrix :cols (range 1 8)))

(def pca (s/principal-components x))

(def components (:rotation pca))
(def pc1 (i/sel components :cols 0))
(def pc2 (i/sel components :cols 1))
(def x1 (i/mmult x pc1))
(def x2 (i/mmult x pc2))

(def pca-plot (c/scatter-plot
                x1 x2
                :x-label "PC1"
                :y-label "PC2"
                :title "Census Race Data by State"))
#_
(i/view pca-plot)

; }}}

;;;; {{{ 10.12 Creating Dynamic Charts with Incanter
;; 

(def d-plot
  (let [x (range -1 1 0.1)]
    (c/dynamic-xy-plot
      [a (range -1.0 1.0 0.1)
       b (range -1.0 1.0 0.1)
       c (range -1.0 1.0 0.1)]
      [x (i/plus (i/mult a x x) (i/mult b x) c)])))

#_
(i/view d-plot)

; }}}

;;;; {{{ writing to excel
#_
(xl/save-xls ibm-data "ibm-data.xls")
; }}}

