
(ns inc-dsets.core
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.data.json :as json]
            [clojure.string :as string])
  (:use incanter.core
        incanter.datasets
        incanter.io
        inc-dsets.utils))

(comment

;;;; 06.01
(use 'incanter.core
     'incanter.datasets)

(def iris (get-dataset :iris))
;; List some of the datasets in incanter.datasets.

;;;; 06.02
(use 'incanter.core)
(def matrix-set (to-dataset [[1 2 3] [4 5 6]]))
(nrow matrix-set)
(col-names matrix-set)

(def map-set (to-dataset {:a 1, :b 2, :c 3}))
(nrow map-set)
(col-names map-set)

(def maps-set (to-dataset [{:a 1, :b 2, :c 3},
                           {:a 4, :b 5, :c 6}]))
(nrow maps-set)
(col-names maps-set)

(def matrix-set-2
  (dataset [:a :b :c]
           [[1 2 3] [4 5 6]]))
(nrow matrix-set-2)
(col-names matrix-set-2)

;;;; (Old 06.02)
(use 'incanter.io)
(require '[clojure.string :as string])

(def us-econ-assistance
  (read-dataset "data/us_foreign_economic_assistance_constant.csv"
                :header true))

(defn clean-number
  ([n]
   (if (string? n)
     (if (.isEmpty n)
       0
       (read-string (string/replace n "," "")))
     n)))

(def int-rows
  [:FY1946 :FY1947 :FY1948 :FY1949 :FY1950 :FY1951 :FY1952 :FY1953
   :FY1954 :FY1955 :FY1956 :FY1957 :FY1958 :FY1959 :FY1960 :FY1961
   :FY1962 :FY1963 :FY1964 :FY1965 :FY1966 :FY1967 :FY1968 :FY1969
   :FY1970 :FY1971 :FY1972 :FY1973 :FY1974 :FY1975 :FY1976 :FY1976tq
   :FY1977 :FY1978 :FY1979 :FY1980 :FY1981 :FY1982 :FY1983 :FY1984
   :FY1985 :FY1986 :FY1987 :FY1988 :FY1989 :FY1990 :FY1991 :FY1992
   :FY1993 :FY1994 :FY1995 :FY1996 :FY1997 :FY1998 :FY1999 :FY2000
   :FY2001 :FY2002 :FY2003 :FY2004 :FY2005 :FY2006 :FY2007 :FY2008
   :FY2009 :FY2010])

(def us-econ-assistance
  (reduce #(transform-col %1 %2 clean-number)
          us-econ-assistance
          int-rows))

;;;; 06.03
(view iris)

;;;; 06.04
(use 'incanter.core
     'incanter.io)
(def data-file "data/all_160_in_51.P35.csv")
(def va-data (read-dataset data-file :header true))
(def va-matrix (to-matrix ($ [:POP100 :HU100 :P035001] va-data)))

(reduce plus va-matrix)

;;;; 06.05
(use 'incanter.core)
($= 7 * 4)
($= 7 * 4 + 3)
($= (nth va-matrix 0) * 4)
($= (sum (nth va-matrix 0)) / (count (nth va-matrix 0)))
(macroexpand-1 '($= 7 * 4))
(macroexpand-1 '($= 7 * 4 + 3))

;;;; 06.06
(use 'incanter.core)
(def richmond ($where {:NAME "Richmond city"} va-data))
richmond
(def small ($where {:POP100 {:lte 1000}} va-data))
(nrow small)
($ [0 1 2 3 4] :all small)
(def medium ($where {:POP100 {:gt 1000 :lt 40000}} va-data))
(nrow medium)
($ [0 1 2 3] :all medium)
(def random-half
  ($where {:GEOID {:$fn (fn [_] (< (rand) 0.5))}} va-data))

;;;; 06.07
;;; Downloaded the data file from http://censusdata.ire.org/.
(use 'incanter.core
     'incanter.io)
(def data-file "data/all_160.P3.csv")
(def race-data (read-dataset data-file :header true))
(def by-state ($group-by :STATE race-data))

;;;; 06.08
($ 0 :all race-data)
($ [0 1 2 3 4] :all race-data)

;;;; 06.09
(take 10 ($ :POP100 race-data))
($ [0 1 2 3 4] :all ($ [:STATE :POP100 :POP100.2000] race-data))
($ [0 1 2 3 4] :all ($ [:STATE :POP100 :P003002 :P003003 :P003004 :P003005 :P003006
    :P003007 :P003008]
   race-data))
($ 0 [:STATE :POP100 :P003002 :P003003 :P003004 :P003005 :P003006
      :P003007 :P003008]
   race-data)
($ [0 1 2 3 4]
   [:STATE :POP100 :P003002 :P003003 :P003004 :P003005 :P003006
    :P003007 :P003008]
   race-data)

;;;; 06.10
(require '[incanter.core :as i]
         '[incanter.io :as i-io]
         '[clojure.data.csv :as csv]
         '[clojure.data.json :as json]
         '[clojure.java.io :as io])

(def data2000
  (i/$ [:Indicator-Code :Indicator-Name :2000] chn-data))

(with-open [f-out (io/writer "data/chn-2000.csv")]
  (csv/write-csv f-out [(map name (i/col-names data2000))])
  (csv/write-csv f-out (i/to-list data2000)))

(with-open [f-out (io/writer "data/chn-2000.json")]
  (json/write (:rows data2000) f-out))

;;;; 06.12
(require '[clojure.java.io :as io]
         '[clojure.data.csv :as csv]
         '[clojure.string :as str]
         '[incanter.core :as i])
(use 'inc-dsets.utils)

(def data-file "data/chn/chn_Country_en_csv_v2.csv")
(def chn-data (read-country-data data-file))

(def chn-1990
  (i/$ [:Indicator-Code :Indicator-Name :1990]
       chn-data))
(def chn-2000
  (i/$ [:Indicator-Code :2000] chn-data))

(def chn-decade
  (i/$join [:Indicator-Code :Indicator-Code]
           chn-1990 chn-2000))

(i/$ (range 5) :all chn-decade)

;;; curl http://censusdata.ire.org/51/all_160_in_51.P3.csv | gzip -cd > data/all_160_in_51.P3.csv
(use '(incanter core io charts)
     '[clojure.set :only (union)])

(def family-data (read-dataset
                   "data/all_160_in_51.P35.csv" :header true))
(def racial-data (read-dataset
                   "data/all_160_in_51.P3.csv" :header true))

(union (set (col-names family-data))
       (set (col-names racial-data)))
(frequencies (concat (col-names family-data)
                     (col-names racial-data)))

(defn dedup-second
  [a b id-col]
  (let [a-cols (set (col-names a))]
    (conj (filter #(not (contains? a-cols %)) (col-names b))
          id-col)))

(def racial-short
  ($ (vec (dedup-second family-data racial-data :GEOID))
     racial-data))

(nrow family-data)
(nrow racial-short)

(def all-data
  ($join [:GEOID :GEOID] family-data racial-short))
(def small ($order :POP100 :asc
              ($where {:POP100 {:$lte 1000}} all-data)))

(def big10
  ($order :POP100 :desc
     ($where {:POP100 {:gt 90000}} all-data)))

(defn group-per
  ([group-key]
   ($map (fn [group total] (float (/ group total)))
      [group-key :POP100])))

(defn add-pop-key
  ([x chart pop-key]
   (add-categories chart x (group-per pop-key)
                   :series-label (name pop-key))))

;; Need to pivot the data:
;; Each row gets broken out into different rows.
;; Name stays the same.
;; :P003002 -> :white
;; :P003003 -> :black
;; :P003004 -> :amerindian
;; :P003005 -> :asian
;; :P003006 -> :pacific-islander
;; :P003007 -> :other
;; :P003008 -> :two-more

(def ^:dynamic *chart-fields*
  [:P003002 :P003003 :P003004 :P003005 :P003006 :P003007 :P003008])

(def ^:dynamic *field-labels*
  {:P003002 "White alone"
   :P003003 "Black or African American alone"
   :P003004 "American Indian and Alaska Native alone"
   :P003005 "Asian alone"
   :P003006 "Native Hawaiian and Other Pacific Islander alone"
   :P003007 "Some Other Race alone"
   :P003008 "Two or More Races"})

(defn to-maps
  ([dataset]
   (let [cols (col-names dataset)]
     (map #(zipmap cols %) (to-list dataset)))))

(defn ->value-map
  ([map-row value-key]
   (-> map-row
     (assoc :value (get map-row value-key)
            :field (get *field-labels* value-key))
     (dissoc value-key))))

(defn pivot-map
  ([map-row]
   (map (fn [k]
          (->value-map
            (select-keys map-row [:NAME :SUMLEV :GEOID :STATE k])
            k))
        *chart-fields*)))

(def big10-pivot (to-dataset (mapcat pivot-map (to-maps big10))))

(view 
  (with-data
    big10-pivot
    (let [names ($ :NAME)]
      (stacked-bar-chart
        :NAME
        :value
        :group-by :field
        :legend true
        :x-label "Cities"
        :y-label "Population"
        :series-label "P003002"))))

(def richmond ($where {:NAME "Richmond city"} all-data))


