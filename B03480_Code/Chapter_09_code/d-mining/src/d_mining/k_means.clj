
(ns d-mining.k-means
  (:gen-class)
  (:require [incanter.core :as i]
            [incanter.charts :as c]
            [clojure.core.reducers :as r]))

;;;; The protocol that clusterable data need to implement.
(defprotocol Locatable
  "These data types have locations in n-dimensional space. Each can also
  generate a random point in the same space."
  (get-point [p] "This returns a vector of the coordinates of the data.")
  (get-dim [p] "This returns the dimensions.")
  (get-range [p d]
    "This returns the range, as a vector, for the given dimension.")
  (make-random-point [p]
    "This generates a random point in the same space."))

;;;; K-Means
(defn random-indexes
  "This returns k random indexes between [0, n)."
  ([k n]
   (loop [output #{}]
     (if (< (count output) k)
       (recur (conj output (rand-int n)))
       (sort output)))))

(defn map-rand-locatables
  "This takes a Locatable method and picks k random items from the collection.
  It returns the result of applying the method to each random item."
  ([method coll k]
   (map (comp method (partial nth coll)) (random-indexes k (count coll)))))

(defn forgy-partition
  "This implements Forby partition selection. This picks k data points as the
  centroids."
  ([k coll] (map-rand-locatables get-point coll k)))

(defn random-partition
  "This implements a random partition. It picks k random points.

  This requires a function for :rand-row that returns a random data row."
  ([k coll] (map-rand-locatables make-random-point coll k)))

(defn distance
  "This computes the distance of two vectors."
  ; ([a b] (i/sqrt (i/sum (i/pow (i/minus a b) 2))))
  ([a b]
   (->> (mapv vector a b)
     (r/map (fn ^Double [[x y]] (let [x (double x) y (double y)] (- x y))))
     (r/map #(Math/pow % 2.0))
     (r/fold + +)
     Math/sqrt)))

(defn reduce-max-key
  ([key-f coll]
   (let [combiner (fn ([] [Integer/MIN_VALUE nil])
                    ([[k1 _ :as p1] [k2 _ :as p2]]
                     (if (> k1 k2) p1 p2)))]
     (second
       (r/fold combiner combiner
               (mapv #(vector (key-f %) %) coll))))))

(defn nearest-centroid
  "This takes a data row and a list of centroids and returns the nearest one."
  ([row centroids]
   (let [p (get-point row)]
     (reduce-max-key (partial distance p) centroids))))

(defn accum-pair
  "This accumulates a pair into a hash-map where the values are vectors."
  ([m kv]
   (let [[k v] kv]
     (assoc m k (conj (get m k []) v)))))

(defn accum-map
  "This takes a sequence of key-value pairs and returns a map. If a key appears
  more than once, it's values are added to a vector of values."
  ([kvs]
   (reduce accum-pair {} kvs)))

(defn assoc-conj
  ([m k v] (assoc m k (conj (get m k) v))))

(defn assign-points
  "This returns a map of cluster centroids to the nearest rows in the dataset."
  ([coll centroids]
   (if-let [initial-index (reduce #(assoc %1 %2 []) {} centroids)]
     (reduce #(assoc-conj %1 (nearest-centroid %2 centroids) %2)
             initial-index coll)
     {})))

(defn get-centroid
  "This calculates the centroid of a collection of points."
  ([coll]
   (let [points (map get-point coll)]
     (if (empty? points)
       nil
       (i/div (apply i/plus points) (count points))))))

(defn get-centroid*
  ([cluster coll]
   (if-let [centroid (get-centroid coll)]
     centroid
     cluster)))

(defn update-centroids
  "This takes a map of centroids and returns a list of new centroids."
  ([clusters] (map (partial apply get-centroid*) clusters)))

(defn k-means
  "This performs the k-means process."
  ([k data & {:keys [initialize] :or {initialize random-partition}}]
   (loop [centroids (initialize k data), prev nil]
     (let [assigned (assign-points data centroids)]
       (if (= assigned prev)
         assigned
         (recur (update-centroids assigned) assigned))))))

;;;; Clustering the Iris dataset.
(defn random-point
  ([locatable]
   (mapv (fn [d]
           (let [[from to] (get-range locatable d)]
             (+ from (* (rand) (- to from)))))
         (range (get-dim locatable)))))

(defrecord Iris
  [species sepal-length sepal-width petal-length petal-width]

  Locatable
  (get-point [iris] [(:petal-length iris) (:petal-width iris)
                     (:sepal-length iris) (:sepal-width iris)])
  (get-dim [_] 4)
  (get-range [_ d] (case d
                     0 [0.0 7.0]
                     1 [0.0 3.0]
                     2 [4.0 8.0]
                     3 [2.0 5.0]))
  (make-random-point [iris] (random-point iris)))

;;;; A utility for graphing groups of data.
(defn chart-values
  ([dataset group-key x-key y-key & {:keys [x-label y-label]}]
    (let [[f & groups] (vals (i/$group-by group-key dataset))
          add-ps (fn [c v] (c/add-points c (i/$ x-key v) (i/$ y-key v)))
          chart (c/scatter-plot (i/$ x-key f) (i/$ y-key f))]
      (reduce add-ps chart groups)
      (i/view chart)
      chart)))

(defn chart-clusters
  ([clusters]
   (let [centroids (keys clusters)
         clusters (vals clusters)
         add-ps (fn [c locatables]
                  (let [points (map get-point locatables)]
                    (c/add-points c (map first points) (map second points))))
         chart (c/scatter-plot (map first centroids) (map second centroids))]
     (reduce add-ps chart clusters)
     (i/view chart)
     chart)))

(comment
(require '[incanter.core :as i]
         '[incanter.charts :as c]
         'incanter.datasets)
(def iris (incanter.datasets/get-dataset :iris))
(def iris-objs (i/$map ->Iris [:Species :Sepal.Length :Sepal.Width
                               :Petal.Length :Petal.Width] iris))
(def clusters (k-means 3 iris-objs))
(def chart (chart-clusters clusters))
  )

;;;; Clustering Locations in Virginia Based on their Ratio of Racial Groups.

(defrecord VaRacialLocation
  [geoid state name pop100 white black indian asian hawaii other multiple]

  Locatable
  (get-point [x]
    (let [pop100 (double (:pop100 x))]
      (map #(/ (% x) pop100)
           [:white :black :indian :asian :hawaii :other :multiple])))
  (get-dim [x] 7)
  (get-range [x _] [0.0 1.0])
  (make-random-point [x] (random-point x)))

(comment
(require '[incanter.core :as i]
         'incanter.io)
(def data-file "data/all_160_in_51.P3.csv")
(def dataset (incanter.io/read-dataset data-file :header true))

(def va-data
  (doall
    (map #(apply ->VaRacialLocation %)
         (i/to-list
           (i/sel dataset
                  :cols
                  [:GEOID :STATE :NAME :POP100 :P003002 :P003003 :P003004
                   :P003005 :P003006 :P003007 :P003008])))))

(def clusters (k-means 10 va-data))
(def chart (chart-clusters clusters))

  ;;;; For optimal value of k, a rule of thumb is:
  ;; user=> (Math/sqrt (/ (count va-data) 2.0))
  ;; 17.190113437671084

(def clusters (k-means 17 va-data))
(def chart (chart-clusters clusters))

  )

;;;; Clustering economic and racial census data.
(defn min-max
  ([dataset col]
   (let [data (filter (complement string?)
                      (i/sel dataset :cols col))]
     [(apply min data) (apply max data)])))

(defrecord CensusLocation
  [label id median-income median-housing race-black race-white race-native
   race-asian poverty race-hawaiian id2 error-housing race-total race-other]

  Locatable
;;;  (get-point [c]
;;;    (mapv #(% c) [:median-income :median-housing :race-total
;;;                  :race-black :race-white :race-native :race-asian
;;;                  :race-hawaiian :race-other]))
;;;  (get-dim [c] 9)
;;;  (get-range [c i]
;;;    (let [ranges [[15000 150000] [50000 1000000] [60000 10000000]
;;;                  [150 1500000] [10000 5500000] [150 150000] [200 1500000]
;;;                  [100 250000] [150 2500000]]]
;;;      (nth ranges i)))
  (get-point [c]
    (let [total (:race-total c)]
      (concat [(:median-income c) (:median-housing c)]
              (mapv #(float (/ (% c) total))
                    [:race-black :race-white :race-native :race-asian
                     :race-hawaiian :race-other]))))
  (get-dim [c] 8)
  (get-range [c i]
    (let [ranges [[15000 150000] [50000 1000000]
                  [0.0 1.0] [0.0 1.0] [0.0 1.0] [0.0 1.0]
                  [0.0 1.0] [0.0 1.0]]]
      (nth ranges i)))
  (make-random-point [x] (random-point x)))

(comment
  ;;; This data was downloaded from [US Census Bureau
  ;;; FactFinder](http://factfinder2.census.gov/faces/nav/jsf/pages/searchresults.xhtml?refresh=t).
  ;;; I performed some projections and joins to get what's below.
(require '[clojure.string :as str]
         '[incanter.core :as i]
         'incanter.io)
(def data-file "data/census-data.csv")
(def dataset (incanter.io/read-dataset data-file :header true))
(def c-data
  (doall
    (map #(apply ->CensusLocation %)
         (i/to-list dataset))))
(def clusters (k-means 20 c-data))
(doseq [[centroid c] clusters]
  (println (mapv float centroid))
  (println (str/join "; " (map :label c)))
  (println))
  )

