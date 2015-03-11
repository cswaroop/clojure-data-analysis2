
(ns webviz.lineplot
  (:require [webviz.core :as webviz]))

(defn ->point [date-fmt item]
  (webviz/Point. (.parse date-fmt (.-date item))
                 (.-close item)
                 0.25))
(defn data->nv
  [date-fmt data]
  (array
    (->> data
      (map (partial ->point date-fmt))
      (apply array)
      (webviz/Group. "IBM"))))

(defn tooltip [formatter key x y e graph]
  (let [dt (-> e .-point .-x formatter)]
    (str "<h3>" key "</h3>"
         "<p>" y " at " dt "</p>")))

(defn set-scale [axis label tick-format]
  (-> axis
    (.axisLabel label)
    (.tickFormat tick-format)))

(defn make-chart [date-fmt]
  (let [c (-> (.lineChart nv.models)
            (.tooltipContent (partial tooltip date-fmt)))]
    (.xScale c (.scale (.-time js/d3)))
    (-> c .-lines .-scatter (.useVoronoi false))
    (set-scale (.-xAxis c) "Date" (.format (.-time js/d3) "%Y"))
    (set-scale (.-yAxis c) "Closing" (.format js/d3 ".02f"))
    c))

(defn remove-fill [_]
  (-> js/d3
    (.select ".nv-group")
    (.style "fill" "transparent")))
(defn ^:export line-plot []
  (let [date-fmt (.format (.-time js/d3) "%e-%b-%y")]
    (webviz/create-chart
      "/lineplot/data.json"
      "#linechart svg"
      (partial make-chart date-fmt)
      (partial data->nv date-fmt)
      :continuation remove-fill)))

; vim: set filetype=clojure:
