
;;;; {{{ Imports
(ns d-mining.validating
  (:require [clojure.core.reducers :as r]
            [clojure.set :as set]
            [d-mining.k-means :as km]
            [d-mining.hierarchical :as h]
            [incanter.core :as i]
            [incanter.stats :as s])
  (:import [d_mining.hierarchical HCluster]))
(comment
  (require '[clojure.core.reducers :as r]
           '[clojure.set :as set]
           '[clojure.stacktrace :as st]
           '[d-mining.k-means :as km]
           '[d-mining.hierarchical :as h]
           '[incanter.core :as i]
           '[incanter.stats :as s])
  (require 'incanter.io)
  (import '[d_mining.hierarchical HCluster])
  )
; }}}

;;;; {{{ Silhouette algorithm for validating clusters.

;; It is a measure of how close a cluster's members are relative to how close
;; the members are to other clusters' members. See Wikipedia for more
;; information.
;;
;; This code was copied from d-mining.ga and cleaned up a very little.

(defn cache-distance
  ([xs x]
   (let [x-hash (:hash x)
         point (km/get-point (:data x))]
     (assoc x
            :dist (->>
                    xs
                    (r/filter (complement (comp (partial = x-hash) :hash)))
                    (r/map (fn [y] [(:hash y) (km/distance point (km/get-point (:data y)))]))
                    (r/fold merge (fn [m [y-hash d]] (assoc m y-hash d))))))))

(defn distance-matrix
  ([coll] (into [] (r/map (partial cache-distance coll) coll))))

(defn get-dist
  "This takes hashes of the actual data, so km/distance no longer really works
  here."
  ([i j] ((:dist i) (:hash j))))

(defn rsum
  ([coll]
   (try
     (r/fold + + coll)
     (catch Exception ex
       ; (doseq [x (into [] coll)]
         ; (println :ERROR :rsum x))
       (throw ex)))))

(defn average-distance
  ([x xs]
   (if (zero? (count xs))
     0.0
     (let [x-hash (:hash x)]
       (/ (rsum (r/map (partial get-dist x) xs))
          (double (count xs)))))))

(defn r-min
  ([coll]
   (let [<> (r/monoid min (constantly Integer/MAX_VALUE))]
     (r/reduce <> coll)
     #_
     (r/fold <> coll))))

(defn silhouette-i
  ([clusters cluster members x]
   (let [a (average-distance x (remove (partial = x) members))
         b (->> (dissoc clusters cluster)
             (r/map (fn [[_ xs]]
                      (average-distance x xs)))
             r-min)
         denom (max a b)]
     (if (zero? denom)
       0.0
       (/ (- b a) (double denom))))))

(defn r-mean-combine
  ([] [0 0])
  ([a b]
   (let [[sum-a n-a] a
         [sum-b n-b] b]
     [(+ sum-a sum-b) (+ n-a n-b)])))

(defn r-mean-reduce
  ([state x]
   (let [[sum n] state]
     [(+ sum x) (inc n)])))

(defn r-mean
  ([r-coll]
   (apply /
          (map double
               (r/reduce r-mean-reduce (r-mean-combine) r-coll)
               #_
               (r/fold r-mean-combine r-mean-reduce r-coll)))))

(defn silhouette-cluster
  ([cluster-index]
   (partial silhouette-cluster cluster-index))
  ([cluster-index cluster-pair]
   (let [[c ms] cluster-pair]
     (silhouette-cluster cluster-index c ms)))
  ([cluster-index c ms]
   (r/map (partial silhouette-i cluster-index c ms) ms)))

(defn fitness-silhouette
  ([cluster-index]
   ; mean or sum?
   (r-mean
     (r/mapcat (silhouette-cluster cluster-index) cluster-index))))

(defn index-fitness
  ([cluster-indexes population]
   (time
     (do
       (println :index-fitness)
       ; r/reduce:
       #_
       (r/reduce
         (fn [m [p ci]] (assoc m p (fitness-silhouette ci)))
         (merge)
         (mapv vector population cluster-indexes))
       ; r/fold: 802293.141 msecs
       (r/fold merge
               (fn [m [p ci]]
                 (assoc m p (fitness-silhouette ci)))
               (mapv vector population cluster-indexes))))))

; }}}

;;;; {{{ Validating hierarchical clustering

;;; {{{ Utilities

(defn map-tail
  ([f coll]
   (lazy-seq
     (when-let [s (seq coll)]
       (cons (f s) (map-tail f (rest s)))))))

; }}}

(defn level-up
  ([cluster]
   (println :level-up cluster)
   (if (instance? HCluster cluster)
     (let [child-a (level-up (:child-a cluster))
           child-b (level-up (:child-b cluster))]
       (println \tab :child-a (:level child-a) :child-b (:level child-b))
       (assoc cluster
              :child-a child-a
              :child-b child-b
              :level (inc (max (:level child-a)
                               (:level child-b)))))
     (assoc cluster :level 0))))

(defn add-child-sets
  ([cluster]
   (if (instance? HCluster cluster)
     (let [child-a (add-child-sets (:child-a cluster))
           child-b (add-child-sets (:child-b cluster))]
       (assoc cluster
              :child-a child-a
              :child-b child-b
              :child-set (set/union (:child-set child-a)
                                    (:child-set child-b))))
     (assoc cluster :child-set #{cluster}))))

(defn build-cophenetic-matrix
  ([cluster]))

(defn cophenetic-matrix
  ([cluster data]))

(defn cophenetic
  ([h-cluster data]
   (let [h-cluster (add-child-sets (level-up h-cluster))
         n (count data)
         m (/ (* n (dec n)) 2.0)
         m1 (/ 1.0 m)
         mean-p nil
         mean-c nil
         means (* mean-p mean-c)]
     )))

; }}}
