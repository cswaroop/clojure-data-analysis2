(ns d-mining.hierarchical-spec
  (:require [d-mining.k-means :as km]
            [clojure.pprint :as pp])
  (:use speclj.core
        d-mining.hierarchical))

(defrecord Location
  [id x y]

  Identifiable
  (get-id [l] (:id l))

  km/Locatable
  (get-point [l] [(:x l) (:y l)])
  (get-dim [l] 2)
  (get-range [l d] [0.0 1.0])
  (make-random-point [l] [(rand) (rand)]))

(describe
  "get-distance"
  (it "should return the distance from the cache if it's there."
      (let [cache {[1 2] 3}, l1 (Location. 1 0.0 0.0), l2 (Location. 2 1.0 1.0)]
        (should= 3 (:distance (get-distance cache [l1 l2])))))
  (it "should return the cache unchanged if the distance is cached."
      (let [cache {[1 2] 3}, l1 (Location. 1 0.0 0.0), l2 (Location. 2 1.0 1.0)]
        (should= cache (:cache (get-distance cache [l1 l2])))))
  (it "should calculate the distance if the items aren't in the cache."
      (let [cache {[1 2] 3}, l1 (Location. 3 0.0 0.0), l2 (Location. 4 1.0 1.0)]
        (should= (km/distance (km/get-point l1) (km/get-point l2))
                 (:distance (get-distance cache [l1 l2])))))
  (it "should add the distance to the cache if it's not there."
      (let [cache {[1 2] 3}, l1 (Location. 3 0.0 0.0), l2 (Location. 4 1.0 1.0)]
        (should= {[1 2] 3, [3 4] (km/distance (km/get-point l1) (km/get-point l2))}
                 (:cache (get-distance cache [l1 l2]))))))

(describe
  "same?"
  (it "should return true if the indexes of the inputs are the same."
      (let [l1 (Location. 1 0.0 0.0), l2 (Location. 2 1.0 1.0)]
        (should (same? l1 l1))))
  (it "should return false if the indexes of the inputs are different."
      (let [l1 (Location. 1 0.0 0.0), l2 (Location. 2 1.0 1.0)]
        (should-not (same? l1 l2))))
  (it "should ignore the items themselves."
      (let [l1 (Location. 1 0.0 0.0), l2 (Location. 2 1.0 1.0)]
        (should (same? (Location. 1 0.0 0.0) (Location. 1 1.0 1.0)))
        (should-not (same? (Location. 1 0.0 0.0) (Location. 2 0.0 0.0))))))

(describe
  "cache-accum-distance"
  (it "should pull distances from the cache if they're there."
      (let [l1 (Location. 1 0.0 0.0), l2 (Location. 2 1.0 1.0),
            state {:cache {[1 2] 3}, :accum []}]
        (should= 3 (->>
                     (cache-accum-distance state [l1 l2])
                     :accum
                     last
                     :distance))))
  (it "should calculate the distance if it's not cached."
      (let [l1 (Location. 3 0.0 0.0), l2 (Location. 4 1.0 1.0),
            state {:cache {[1 2] 3}, :state []},
            d (km/distance (km/get-point l1) (km/get-point l2))]
        (should= d (->>
                     (cache-accum-distance state [l1 l2])
                     :accum
                     last
                     :distance))))
  (it "should add the distance to the cache when it must be calculated."
      (let [l1 (Location. 3 0.0 0.0), l2 (Location. 4 1.0 1.0),
            state {:cache {[1 2] 3}, :state []}
            d (km/distance (km/get-point l1) (km/get-point l2))]
        (should= {[1 2] 3, [3 4] d}
                 (:cache (cache-accum-distance state [l1 l2])))))
  (it "should add the pair with their distance to the accumulator."
      (let [l1 (Location. 1 0.0 0.0), l2 (Location. 2 1.0 1.0),
            state {:cache {[1 2] 3}, :accum []},
            pair [l1 l2]]
        (should= {:pair pair :distance 3}
                 (last
                   (:accum
                     (cache-accum-distance state [l1 l2])))))))

(describe
  "min-dist"
  (it "should return the item with the minimum distance."
      (let [a {:distance 1}, b {:distance 2}]
        (should= a (min-dist a b))
        (should= a (min-dist b a)))))

(describe
  "not-in-pair"
  (it "should return false if the item's index is one of the two index parameters."
      (let [a (Location. 1 2 3), b (Location. 2 3 4)]
        (should-not (not-in-pair 1 2 a))
        (should-not (not-in-pair 1 2 b))))
  (it "should return true if the item's index is not one of the two parameters."
      (let [a (Location. 1 2 3), b (Location. 2 3 4)]
        (should (not-in-pair 3 4 a))
        (should (not-in-pair 3 4 b)))))

(describe
  "h-cluster"
  (it "should return one Cluster object."
      (let [locs [(Location. 1 0.1 0.1) (Location. 2 0.1 0.2) (Location. 3 0.5 0.5)]]
;;;        (pp/pprint (h-cluster locs))
        (should (instance? d_mining.hierarchical.HCluster (h-cluster locs)))))
  (it "should cluster (0.5, 0.5) with a cluster of the other two."
      (let [locs [(Location. 1 0.1 0.1) (Location. 2 0.1 0.2) (Location. 3 0.5 0.5)]
            cluster (h-cluster locs)
            children (map (partial get cluster) [:child-a :child-b])
            point (first (filter (partial instance? Location) children))
            subcluster (first (filter (partial instance? d_mining.hierarchical.HCluster)
                                      children))]
        (should-not (nil? point))
        (should-not (nil? subcluster))
        (should= [0.5 0.5] (km/get-point point))
        (should= [(/ (+ 0.1 0.1) 2.0) (/ (+ 0.1 0.2) 2.0)]
                 (km/get-point subcluster))))
  (it "should cluster (0.1, 0.1) and (0.1, 0.2) together."
      (let [locs [(Location. 1 0.1 0.1) (Location. 2 0.1 0.2) (Location. 3 0.5 0.5)]
            cluster (h-cluster locs)
            subcluster (->>
                         (map (partial get cluster) [:child-a :child-b])
                         (filter (partial instance? d_mining.hierarchical.HCluster))
                         first)
            children (map (partial get subcluster) [:child-a :child-b])
            c1 (first (filter #(= 1 (:id %)) children))
            c2 (first (filter #(= 2 (:id %)) children))]
        (should= [0.1 0.1] (km/get-point c1))
        (should= [0.1 0.2] (km/get-point c2))))
  (it "should calculate the distance between the children in the cluster."
      (let [locs [(Location. 1 0.1 0.1) (Location. 2 0.1 0.2) (Location. 3 0.5 0.5)]
            cluster (h-cluster locs)
            d (apply km/distance (map #(km/get-point (% cluster)) [:child-a :child-b]))]
        (should= d (:distance cluster))))
  (it "should calculate the new center of the cluster as the average of its children."
      (let [locs [(Location. 1 0.1 0.1) (Location. 2 0.1 0.2) (Location. 3 0.5 0.5)]
            cluster (h-cluster locs)
            [x1 y1] (km/get-point (:child-a cluster))
            [x2 y2] (km/get-point (:child-b cluster))
            c [(/ (+ x1 x2) 2.0) (/ (+ y1 y2) 2.0)]]
        (should= c (km/get-point cluster)))))

(run-specs)

