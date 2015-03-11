
(ns webviz.barchart
  (:use [webviz.scatter :only (sum-by)])
  (:require [webviz.core :as webviz]))

(defn count-point [pair]
  (let [[diet items] pair]
    (webviz/Point. diet (count items) 1)))

(defn get-diet-counts [diet-groups]
  (apply array (map count-point diet-groups)))

(defn weight-point [pair]
  (let [[diet items] pair
        weight-total (sum-by #(.-weight %) items)]
    (webviz/Point. diet weight-total 1)))

(defn get-diet-weights [diet-groups]
  (apply array (map weight-point diet-groups)))

(defn json->nv-groups [json]
  (let [diet-groups (group-by #(.-diet %) json)]
    (array (webviz/Group. "Chick Counts"
                          (get-diet-counts diet-groups))
           (webviz/Group. "Chick Weights"
                          (get-diet-weights diet-groups)))))

(defn ^:export bar-chart []
  (webviz/create-chart "/barchart/data.json"
                       "#barchart svg"
                       #(.multiBarChart js/nv.models)
                       json->nv-groups
                       :x-label "Diet"))

; vim: set filetype=clojure:
