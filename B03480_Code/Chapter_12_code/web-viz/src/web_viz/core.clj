
;;;; {{{ Dependencies
(ns web-viz.core
  (:use [hiccup core element page]
        [ring.util.response :only (redirect)])
  (:require [clojure.data.json :as json]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]))
(comment
(use '[hiccup core element page])
(require '[clojure.data.json :as json]
         '[clojure.data.csv :as csv]
         '[clojure.java.io :as io]
         '[clojure.set :as as]
         '[clojure.string :as str])
  )
; }}}

;;;; {{{ d3 page
(defn d3-page
  [title js body & {:keys [extra-js] :or {extra-js []}}]
  (html5
    [:head
     [:title title]
     (include-css "/css/nv.d3.css")
     (include-css "/css/style.css")]
    [:body
     (concat
       [body]
       [(include-js "http://d3js.org/d3.v3.min.js")
        (include-js "/js/nv.d3.min.js")]
       (map include-js extra-js)
       [(include-js "/js/script.js")
        (javascript-tag js)])]))

(defn d3-only-page
  [title js body & {:keys [extra-js] :or {extra-js []}}]
  (html5
    [:head
     [:title title]
     (include-css "/css/style.css")]
    [:body
     (concat
       [body]
       [(include-js "http://d3js.org/d3.v3.min.js")]
       (map include-js extra-js)
       [(include-js "/js/script.js")
        (javascript-tag js)])]))
; }}}

;;;; {{{ Data

;;; {{{ Conversions
(defn or-nil
  [f v]
  (try (f v)
    (catch Exception e nil)))
(defn ->double [d] (or-nil #(Double/parseDouble %) d))
(defn ->long [l] (or-nil #(Long/parseLong %) l))

(defn apply-arg [f v] (f v))
(defn apply-schema
  "Merge a data map with a map of converters by applying the
  converters to the data."
  ([coerce-map] (partial apply-schema coerce-map))
  ([coerce-map data-map]
   (merge-with apply-arg coerce-map data-map)))
; }}}

;;; {{{ read-data-file-

(defn read-data-file-
  ([filename coerce-map & {:keys [header]
                           :or {header nil}}]
   (with-open [in-file (io/reader filename)]
     (let [data (csv/read-csv in-file)
           [header data]
           (if (nil? header)
             [(map (comp keyword str/lower-case str/trim)
                   (first data))
              (rest data)]
             [header data])]
       (doall
         (->> data
           (filter #(= (count header) (count %)))
           (map (partial zipmap header))
           (map (apply-schema coerce-map))))))))

(defn read-data-file
  ([filename coerce-map]
   (with-open [in-file (io/reader filename)]
     (let [data (csv/read-csv in-file)
           header (map (comp keyword str/lower-case str/trim)
                       (first data))]
       (doall
         (->> (rest data)
           (filter #(= (count header) (count %)))
           (map (partial zipmap header))
           (map (apply-schema coerce-map))))))))
; }}}

;;; {{{ IBM
(defn read-ibm
  ([] (read-ibm "resources/data/ibm.csv"))
  ([filename]
   (read-data-file filename {:volume ->long,
                             :close  ->double,
                             :low    ->double,
                             :high   ->double,
                             :open   ->double})))
; }}}

;;; {{{ Census Race Data for All Locations
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

(defn read-race-data
  ([] (read-race-data "resources/data/all_160.P3.csv"))
  ([filename]
   (let [data (read-data-file- filename
                               {:geoid   ->long,
                                :state   ->long,
                                :pop100  ->long, :pop100.2000  ->long,
                                :hu100   ->long, :hu100.2000   ->long,
                                :p003001 ->long, :p003001.2000 ->long,
                                :p003002 ->long, :p003002.2000 ->long,
                                :p003003 ->long, :p003003.2000 ->long,
                                :p003004 ->long, :p003004.2000 ->long,
                                :p003005 ->long, :p003005.2000 ->long,
                                :p003006 ->long, :p003006.2000 ->long,
                                :p003007 ->long, :p003007.2000 ->long,
                                :p003008 ->long, :p003008.2000 ->long})
         re-key {:pop100.2000 :pop100_2000, :hu100.2000 :hu100_2000,
                 :p003001 :total,           :p003001.2000 :total_2000,
                 :p003002 :white,           :p003002.2000 :white_2000,
                 :p003003 :black,           :p003003.2000 :black_2000,
                 :p003004 :native_american, :p003004.2000 :native_american_2000,
                 :p003005 :asian,           :p003005.2000 :asian_2000,
                 :p003006 :hawaiian,        :p003006.2000 :hawaiian_2000,
                 :p003007 :other,           :p003007.2000 :other_2000,
                 :p003008 :multiracial,     :p003008.2000 :multiracial_2000}]
     (->> data
       (map #(dissoc % :sumlev :county :cbsa :csa :necta :cnecta))
       (map #(set/rename-keys % re-key))
       (map #(assoc % :state_name (state-codes (:state %))))))))

; }}}

;;; {{{ Chick Weight
(defn read-chick-weight
  ([] (read-chick-weight "resources/data/chick_weight.csv"))
  ([filename]
   (read-data-file- filename {:weight (comp ->long str/trim),
                              :time   (comp ->long str/trim),
                              :chick  (comp ->long str/trim),
                              :diet   (comp ->long str/trim)})))
; }}}

;;; {{{ Abalone
(defn read-abalone
  ([] (read-abalone "resources/data/abalone.data"))
  ([filename]
   (read-data-file-
     filename
     {:length ->double,
      :diameter ->double,
      :height ->double,
      :whole_weight ->double,
      :shucked_weight ->double,
      :viscera_weight ->double,
      :shell_weight ->double,
      :rings ->long}
     :header [:sex :length :diameter :height :whole_weight
              :shucked_weight :viscera_weight :shell_weight :rings])))
; }}}

;;; {{{ Clusters.
;; Some of this requires the code in d-mining.weka.
(comment
(require '[clojure.data.json :as json]
         '[clojure.java.io :as io])

(import [weka.core DenseInstance])
(def data (load-csv "resources/data/all_040.P3.csv"))
(set-fields
  data
  [:geoid :sumlev :state :county :cbsa :csa :necta :cnecta :name
   :pop100 :housing-units-100 :pop100-2000 :housing-units-100-2000
   :race-total :race-total-2000 :race-white :race-white-2000
   :race-black :race-black-2000 :race-indian :race-indian-2000
   :race-asian :race-asian-2000 :race-hawaiian :race-hawaiian-2000
   :race-other :race-other-2000 :race-two-more :race-two-more-2000])
(doto data
  (.deleteAttributeAt 7)
  (.deleteAttributeAt 6)
  (.deleteAttributeAt 5)
  (.deleteAttributeAt 4)
  (.deleteAttributeAt 3)
  (.deleteAttributeAt 1))
(def d2 (filter-attributes data [1 2 3]))
(def clusters (k-means d2 :k 10 :seed 1))

(defn find-center [clusters]
  (let [centroids (.getClusterCentroids clusters)
        center (DenseInstance. (.get centroids 0))]
    (doseq [i (range (.numAttributes centroids))]
      (.setValue center i (.meanOrMode centroids i)))
    center))

(defn attributes [inst]
  (map #(.attribute inst %) (range (.numAttributes inst))))

(defn instance->map [inst attrs]
  (reduce (fn [m a]
            (assoc m (keyword (.name a)) (.value inst a)))
          {}
          attrs))

(defn build-center-node [n inst attrs]
  {:n n
   :name "center"
   :group 2
   :instance inst
   :data (instance->map inst attrs)})

(defn build-centroid-node [n inst]
  {:n n
   :name "centroid"
   :group 1
   :instance inst
   :data (instance->map inst (attributes inst))})

(defn build-data-node [n name group data-inst]
  {:n n
   :name name
   :group group
   :instance data-inst
   :data (instance->map data-inst (attributes data-inst))})

(defn build-link [clusters centroids node]
  (let [dist (.getDistanceFunction clusters)
        c-number (.clusterInstance clusters (:instance node))
        centroid (.get centroids c-number)]
    {:source (inc c-number)
     :target (:n node)
     :value (.distance dist centroid (:instance node))}))

(defn link-to-center [clusters center centroid]
  (let [dist (.getDistanceFunction clusters)]
    {:source (:n center)
     :target (:n centroid)
     :value (.distance dist (:instance center) (:instance centroid))}))

(defn clean-instance [node] (dissoc node :instance))

(defn build-cluster-graph [instances full-instances clusters]
  (let [centroids (.getClusterCentroids clusters)
        center (build-center-node 0 (find-center clusters) (attributes (.get centroids 0)))
        c-nodes (reduce (fn [cs n]
                          (conj cs
                                (build-centroid-node (inc n) (.get centroids n))))
                        []
                        (range (.numInstances centroids)))
        d-nodes (second
                  (reduce (fn [[n ds] index]
                          (let [inst (.get full-instances index)]
                            [(inc n)
                             (conj ds
                                   (build-data-node n
                                                    (.stringValue inst 2)
                                                    0
                                                    (.get instances index)))]))
                        [(inc (count c-nodes)) []]
                        (range (.numInstances instances))))]
    {:nodes (vec (map clean-instance (concat [center] c-nodes d-nodes)))
     :links (vec
              (concat
                (map (partial link-to-center clusters center) c-nodes)
                (map (partial build-link clusters centroids) d-nodes)))}))

(def cluster-data (build-cluster-graph d2 data clusters))
(with-open [w (io/writer "resources/data/clusters.json")]
  (json/write cluster-data w))
  )
; }}}

; }}}

;;;; {{{ Utilities

(defn json-response
  [data]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/write-str data)})

; }}}

;;;; {{{ 11.01 serving data with ring and compojure

(defn ibm-json []
  (json-response (read-ibm)))

; }}}

;;;; {{{ 11.02 html with hiccup
; }}}

;;;; {{{ 11.03 clojurescript setup
; }}}

;;;; {{{ 11.04 creating scatter charts

(defn scatter-data []
  (json-response (read-race-data)))

(defn scatter-charts []
  (d3-page "Scatter Charts"
           "webviz.scatter.scatter_plot();"
           [:div#scatter.chart [:svg]]))

; }}}

;;;; {{{ 11.05 creating bar charts

(defn bar-data []
  (json-response (read-chick-weight)))

(defn bar-chart []
  (d3-page "Bar Chart"
           "webviz.barchart.bar_chart();"
           [:div#barchart.chart [:svg]]))

; }}}

;;;; {{{ 11.06 creating line plots

(defn line-plot []
  (d3-page "Line Chart"
           "webviz.lineplot.line_plot();"
           [:div#linechart.chart [:svg]]))

; }}}

;;;; {{{ 11.07 creating histograms

(defn hist-data []
    (json-response (read-abalone)))

(defn hist-plot []
  (d3-page "Histogram"
           "webviz.histogram.histogram();"
           [:div#histogram.chart [:svg]]))

; }}}

;;;; {{{ time series data

(defn time-series []
  (d3-page
    "IBM Stock Data"
    "webviz.time.ibm_stock();"
    [:div#time.chart [:svg]]))

; }}}

;;;; {{{ 11.08 creating box plots

(defn box-plot []
  (d3-page "Box Plot"
           "webviz.boxplot.boxplot();"
           [:div#boxplot.chart]
           :extra-js ["https://gist.github.com/raw/4061502/box.js"]))

; }}}

;;;; {{{ 11.XX adding lines to scatter plots

(defn scatter-line-plot []
  (d3-page "Scatter Line Chart"
           "webviz.scatterline.scatter_line_plot();"
           [:div#scatterline.chart [:svg]]))

; }}}

;;;; {{{ 11.09 visualizing relationships with force-directed layouts

(defn force-layout-data []
  (redirect "/resources/data/clusters.json"))

(defn force-layout-plot []
  (d3-page "Force-Directed Layout"
           "webviz.force.force_layout();"
           [:div#force.chart [:svg]]))

; }}}

;;;; {{{ 11.10 creating interactive visualizations
(defn interactive-force-data []
  (redirect "/resources/data/clusters.json"))

(defn interactive-force-plot []
  (d3-page "Interactive Force-Directive Layout"
           "webviz.int_force.interactive_force_layout();"
           [:div
            [:div#force.chart [:svg]]
            [:div#datapane]]))
; }}}

