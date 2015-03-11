
;;;; The box.js that I'm basing this on is from
;;;; https://gist.github.com/4061502, which attributes it to [Jason
;;;; Davies](http://www.jasondavies.com/).

(ns webviz.boxplot
  (:require [clojure.browser.dom :as dom]
            goog.style))

(defn min-max 
  ([] [js/Infinity (* -1 js/Infinity)])
  ([[mn mx] x]
   [(min mn x) (max mx x)]))
(defn get-bounds [colls]
  (reduce min-max (min-max) (flatten colls)))

(defn iqr [k]
  (fn [d i]
    (let [q1 (aget d "quartiles" 0), q3 (aget d "quartiles" 2)
          iqr (* (- q3 q1) k)
          lower (- q1 iqr), upper (+ q3 iqr)
          d-index (map-indexed array d)]
      (array
        (ffirst
          (drop-while #(< (second %) lower)
                      d-index))
        (ffirst
          (drop-while #(> (second %) upper)
                      (reverse d-index)))))))

(defn on-second [f]
  (fn [pair]
    (let [[k v] pair]
      [k (f v)])))
(defn offset-year [date-fmt date-str]
  (+ 1900 (.getYear (.parse date-fmt (.-date date-str)))))
(defn json->data [date-fmt json]
  (->> json
    (group-by (partial offset-year date-fmt))
    seq
    (sort-by first)
    (map (on-second (partial map #(.-close %))))))
(defn data->array [data]
  (apply array (map #(apply array %) (vals data))))

(defn make-chart [width height]
  (-> js/d3
    (.box)
    (.whiskers (iqr 1.5))
    (.width width)
    (.height height)))

(defn get-data-container [data]
  (-> js/d3
    (.select "#boxplot")
    (.selectAll "svg")
    (.data data)))

(defn create-svg-container
  [d3-obj width height transform]
  (-> d3-obj
    (.enter)
    (.append "svg")
    (.attr "class" "box")
    (.attr "width" width)
    (.attr "height" height)
    (.append "g")
    (.attr "transform" transform)))

(defn render-plot
  [chart inner-width inner-height transform json]
  (when json
    (let [date-fmt (.format (.-time js/d3) "%e-%b-%y")
          data (json->data date-fmt json)
          bounds (get-bounds (vals data))
          data-obj (data->array data)]
      (.domain chart (apply array bounds))
      (-> data
        data->array
        get-data-container
        (create-svg-container
          inner-width inner-height transform)
        (.call chart)))))

(defn ^:export boxplot []
  (let [size (.getSize goog.style (dom/get-element "boxplot"))
        margin {:top 10, :right 30, :bottom 20, :left 30}
        width (- 80 (:left margin) (:right margin))
        height (- (.-height size) (:top margin) (:bottom margin))]
    (.json js/d3
           "/boxplot/data.json"
           (partial render-plot
                    (make-chart width height)
                    (+ width (:left margin) (:right margin))
                    (+ height (:top margin) (:bottom margin))
                    (str "translate("
                         (:left margin) \,
                         (:top margin) ")")))))

; vim: set filetype=clojure:
