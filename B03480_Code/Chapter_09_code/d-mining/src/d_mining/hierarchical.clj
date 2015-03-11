
(ns d-mining.hierarchical
  (:require [incanter.core :as i]
            incanter.io
            [d-mining.k-means :as km]
            [clojure.data.json :as json]
            [clojure.java.io :as io]))

(comment
(require '[incanter.core :as i]
         'incanter.io
         '[d-mining.k-means :as km]
         '[clojure.data.json :as json]
         '[clojure.java.io :as io])
  )

;;;; Hierarchical Clustering

;; This is just something that can be identified.
(defprotocol Identifiable
  (get-id [i] "This returns an identifier for the object."))

;; This represents a hierarchical cluster. The children are both Locatables.
(defrecord HCluster
  [id child-a child-b distance location level]

  Identifiable
  (get-id [c] (:id c))

  km/Locatable
  (get-point [c] (:location c))
  (get-dim [c] (count (:location c)))
  (get-range [c d] (km/get-range (:child-a c) d))
  (make-random-point [c] (km/random-point c)))


(defn get-distance
  "This looks for the distance betwen two locatables in the cache and returns
  that. If it's not in the cache, it calculates the distance and returns that,
  as well as the cache with the value added (or not)."
  ([cache pair]
   (let [[a-loc b-loc] pair,
         a-index (get-id a-loc),
         b-index (get-id b-loc)]
     (cond (contains? cache [a-index b-index]) {:cache cache
                                                :distance (cache [a-index b-index])}
           (contains? cache [b-index a-index]) {:cache cache
                                                :distance (cache [b-index a-index])}
           :else (let [d (km/distance (km/get-point a-loc)
                                      (km/get-point b-loc))]
                   {:cache (assoc cache [a-index b-index] d)
                    :distance d})))))

(defn same?
  ([a b] (= (get-id a) (get-id b))))

(defn cache-accum-distance
  ([state pair]
   (let [{:keys [cache accum]} state
         {:keys [cache distance]} (get-distance cache pair)]
     {:cache cache
      :accum (conj accum {:pair pair :distance distance})})))

(defn min-dist
  ([a b]
   (let [a-dist (:distance a), b-dist (:distance b)]
     (if (< a-dist b-dist) a b))))

(defn not-in-pair
  ([a-index b-index item]
   (let [index (get-id item)]
     (and (not= a-index index) (not= b-index index)))))

(defn get-distance-matrix
  ([cache locatables]
   (reduce cache-accum-distance
           {:cache cache :accum []}
           (for [a locatables, b locatables,
                 :when (not (same? a b))]
             [a b]))))

(defn insert-new-cluster
  ([locs id a b distance]
   (let [loc (i/div (i/plus (km/get-point a) (km/get-point b)) 2.0)
         cluster (HCluster. id a b distance loc nil)
         new-locs (filter (partial not-in-pair (get-id a) (get-id b)) locs)]
     (conj new-locs cluster))))

(defn h-cluster
  "This performs an hierarchical cluster on the collection of Locatables passed
  in. It returns a single HCluster, which is the root of the hierarchy."
  ([locatables]
   (loop [locs locatables,
          cache {},
          i (inc (apply max (map get-id locatables)))]
     (if (= (count locs) 1)
       (first locs)
       (let [{:keys [cache accum]} (get-distance-matrix cache locs)
             {[a b] :pair distance :distance} (reduce min-dist accum)]
         (recur (insert-new-cluster locs i a b distance) cache (inc i)))))))

;;;; Clustering Locations in Virginia Based on their Ratio of Racial Groups.

(extend-protocol Identifiable
  d_mining.k_means.VaRacialLocation
  (get-id [x] (:geoid x))
  
  d_mining.k_means.CensusLocation
  (get-id [x] (:id2 x)))

;;;; For Visualization

(defprotocol ToJson
  (->json [j] "This converts it to a more basic Clojure datatype for converting to JSON."))

(extend-protocol ToJson
  HCluster
  (->json [j]
    {:name (format "%.4f" (:distance j)),
     :children (mapv (comp ->json (partial get j)) [:child-a :child-b]),
     :size (:distance j)})

  d_mining.k_means.VaRacialLocation
  (->json [j]
    {:name (:name j),
     :children []})
  
  d_mining.k_means.CensusLocation
  (->json [c]
    {:name (:label c)
     :children []}))

(defn save-json-data
  ([data filename]
   (with-open [f-out (io/writer filename)]
     (json/write (->json data) f-out))))

;;;; Now, the actual example code.

(comment
(def data-file "data/all_160_in_51.P3.csv")
(def dataset (incanter.io/read-dataset data-file :header true))

(def va-data
  (doall
    (map #(apply km/->VaRacialLocation %)
         (i/to-list
           (i/sel dataset
                  :cols
                  [:GEOID :STATE :NAME :POP100 :P003002 :P003003 :P003004
                   :P003005 :P003006 :P003007 :P003008])))))

(def va-clusters (h-cluster va-data))
(save-json-data va-clusters "va-clusters.json")
  ;; Take a snapshot of part of this for the book.
)

(comment
  (def data-file "data/census-data.csv")
  (def dataset (incanter.io/read-dataset data-file :header true))
  (def c-data
    (doall
      (map #(apply km/->CensusLocation %)
           (i/to-list dataset))))
  (def c-clusters (h-cluster c-data))
  (save-json-data c-clusters "census-clusters.json")
  )

