
(ns webviz.scatterline
  (:require d3
            nv
            [clojure.browser.dom :as dom]
            [webviz.core :as webviz])
  (:use [webviz.scatter :only (->nv-data)]))

(deftype LineGroup [key values slope intercept])

(defn get-bounds [coll]
  (let [p (first coll)]
    (loop [coll (rest coll)
           bounds {:left (.-x p) :bottom (.-y p)
                :right (.-x p) :top (.-y p)}]
      (if-let [p (first coll)]
        (recur (rest coll)
               {:left (min (:left bounds) (.-x p))
                :right (max (:right bounds) (.-x p))
                :bottom (min (:bottom bounds) (.-y p))
                :top (max (:top bounds) (.-y p))})
        bounds))))

;; TODO: Check intercept formula
(defn get-line [x1 y1 x2 y2]
  (let [m (/ (- y2 y1) (- x2 x1))]
    {:slope m
     :intercept (- y2 (* m x2))}))

(defn group->line-group [group]
  (let [values (.-values group)
        bounds (get-bounds values)
        line (get-line (:left bounds) (:bottom bounds)
                       (:right bounds) (:top bounds))]
    (LineGroup. (.-key group)
                values
                (:slope line)
                (:intercept line))))

(defn nv-data-with-line [data]
  (aset data 0 (group->line-group (aget data 0)))
  data)

(defn make-chart []
  (let [c (-> (.scatterPlusLineChart (aget js/nv "models"))
            (.showDistX true)
            (.showDistY true)
            (.useVoronoi false)
            (.color (-> js/d3 .-scale .category10 .range)))]
    (.tickFormat (.-xAxis c) (.format js/d3 "d"))
    (.tickFormat (.-yAxis c) (.format js/d3 "d"))
    c))

(defn ^:export scatter-line-plot []
  (webviz/create-chart "/scatterline/data.json"
                       "#scatterline svg"
                       make-chart
                       (comp nv-data-with-line
                             (partial ->nv-data "Racial Data"))
                       :transition true))

; vim: set filetype=clojure:
