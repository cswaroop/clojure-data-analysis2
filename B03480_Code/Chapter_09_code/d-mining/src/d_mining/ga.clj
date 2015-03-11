
;;;; Clustering Genetic Algorithm by Petra Kudová.
;;;;
;;;; 18th International Workshop on Database and Expert Systems Applications,
;;;; 2007.

;;; {{{ Declarations
(ns d-mining.ga
  (:gen-class)
  (:require [d-mining.k-means :as km]
            [incanter.core :as i]
            [incanter.stats :as s]
            [clojure.core.reducers :as r]
            [clojure.pprint :as pp]))

(comment
  (use 'd-mining.ga :reload)
  (in-ns 'd-mining.ga)
  (require '[d-mining.k-means :as km]
           '[incanter.stats :as s]
           '[clojure.core.reducers :as r])
  )
; }}}

;;;; {{{ Utilities
(defn r-max-key
  ([key-fn coll]
   (r/reduce (partial max-key key-fn) (first coll) (next coll))))

(defn r-min-key
  ([key-fn coll]
   (r/reduce (partial max-key key-fn) (first coll) (next coll))))

(defn nearest
  ([x xs]
   (r-min-key (partial km/distance x) xs)))

(defn rand-between
  ([x y] (+ x (* (rand) (- y x)))))

(defn ratio-between
  ([t x y] (+ x (* t (- y x)))))

(defn rand-between-points
  ([p1 p2]
   (let [t (rand)]
     (mapv (partial ratio-between t) p1 p2))))

(defn rand-point
  ([point-range]
   (mapv (fn [[x y]] (rand-between x y)) point-range)))

(defn get-ranges
  ([loc]
   (let [data (if (instance? d_mining.k_means.Locatable loc) loc (:data loc))]
     (map (partial km/get-range data) (range (km/get-dim data))))))

(defn rand-centroid
  ([k point-range]
   (mapv (fn [_] (rand-point point-range)) (range k))))

(defn rand-centroid-from
  ([k prototype]
   (rand-centroid k (get-ranges prototype))))

(defn rand-population
  ([p k-range point-range]
   (let [[k1 k2] k-range]
     (map (fn [_]
            (rand-centroid (rand-between k1 k2) point-range))
          (range p)))))

(defn rand-population-from
  ([p k-range prototype]
   (rand-population p k-range (get-ranges prototype))))

(defn modify-point
  ([coll n fn] (assoc coll n (fn (get coll n)))))

(defn bound-by
  "This limits free-range to fall within bounding-range."
  ([bounding-range free-range]
   (let [[mb nb] bounding-range
         [mf nf] free-range]
     [(max mb mf) (min nb nf)])))

(defn nudge-point
  "This nudges the point within a square around the point, bound by the
  ranges."
  ([ranges point] (nudge-point ranges point 0.5))
  ([ranges point delta]
   (let [bounds (mapv bound-by
                      ranges
                      (map #(vector (- delta %) (+ delta %)) point))]
     (rand-point bounds))))

(defn assign-points
  "This returns a map of cluster centroids to the nearest rows in the dataset."
  ([coll centroids]
   (if-let [initial-index (reduce #(assoc %1 %2 []) {} centroids)]
     (reduce #(km/assoc-conj %1 (km/nearest-centroid (:data %2) centroids) %2)
             initial-index coll)
     {})))

(defn get-centroid
  "This calculates the centroid of a collection of points."
  ([coll]
   (let [points (map (comp km/get-point :data) coll)]
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

; }}}

;;;; {{{ Basic genetic operators.
;; Centroids are just a vector of vector pairs.

(defn single-crossover
  ([c other]
   (let [c (vec c)
         x (min (rand (count c)) (count other))]
     [(vec (concat (subvec c 0 x) (subvec other x)))
      (vec (concat (subvec other 0 x) (subvec c x)))])))

(defn combining-crossover
  ([c other]
   (let [c-x (fn [a b]
               (mapv (fn [p]
                       (rand-between-points p (nearest p a)))
                     b))]
     (vector (c-x c other) (c-x other c)))))

(def ^:dynamic *crossover-ops* [single-crossover combining-crossover])

(defn crossover
  ([ga s1 s2 crossover-p]
   (if (or (nil? s1) (nil? s2))
     [s1 s2]
     (if (< (rand) crossover-p)
       (let [fn (rand-nth *crossover-ops*)]
         (fn s1 s2))
       [s1 s2]))))

(defn standard-mutation
  ([ga e]
   (assoc e (rand-int (count e)) (rand-point (:point-ranges ga)))))

(defn biased-one-point-mutation
  ([ga e]
   (modify-point
     e (rand-int (count e)) (partial nudge-point (:point-ranges ga)))))

(defn k-means-mutation
  ([ga e] (k-means-mutation ga e 3))
  ([ga e steps]
   (if (zero? steps)
     (mapv vec e)
     (recur ga
            (update-centroids (assign-points (:data ga) e))
            (dec steps)))))

(defn cluster-addition
  ([ga e]
   (if (<= (inc (count e)) (second (:k-range ga)))
     (conj e (rand-point (:point-ranges ga)))
     e)))

(defn cluster-removal
  ([ga e]
   (if (>= (dec (count e)) (first (:k-range ga)))
     (let [x (rand-int (dec (count e)))]
       (vec (concat (subvec e 0 x) (subvec e (inc x)))))
     e)))

(def ^:dynamic *mutation-ops*
  [standard-mutation biased-one-point-mutation k-means-mutation
   cluster-addition cluster-removal])

(defn mutation
  ([ga e mutation-p]
   (if (< (rand) mutation-p)
     (let [fn (rand-nth *mutation-ops*)]
       (fn ga e))
     e)))

; }}}

;;;; {{{ The GA and some utilities
(defrecord GACluster
  [generation population data k-range point-ranges
   cluster-index fitness-index])

(defn index-population
  ([clusters data]
   (map (partial assign-points data) clusters)))

(defn index-clusters
  ([ga]
   (index-population (:population ga) (:data ga))))
; }}}

;;;; {{{ Distance cache

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

(defn get-distance
  "This takes hashes of the actual data, so km/distance no longer really works
  here."
  ([i j] ((:dist i) (:hash j))))

; }}}

;;;; {{{ The fitness function
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
       (/ (rsum (r/map (partial get-distance x) xs))
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

;;;; {{{ Normalization

(defn normalize-clusters
  ([cluster-index]
   (update-centroids cluster-index)))

; }}}

;;;; {{{ Selection

(defn rand-pick-n
  ([n coll]
   (loop [s #{}]
     (if (= (count s) n)
       (seq s)
       (recur (conj s (rand-nth coll)))))))

(defn selection
  ([ga]
   (let [[x y] (rand-pick-n 2 (:population ga))]
     (max-key (:fitness-index ga) x y))))

; }}}

;;;; {{{ The GA itself
(defn get-best-solution
  ([ga]
   (r-max-key second (vec (:fitness-index ga)))))

(defn get-all-solutions
  ([ga] (:fitness-index ga)))

(defn filter-k-range
  ([ga population]
   (let [[k-from k-to] (:k-range ga)]
     (filter #(and (>= (count %) k-from) (<= (count %) k-to)) population))))

(defn next-population
  ([ga m pass-through crossover-p mutation-p]
   (loop [p (set (take pass-through
                       (sort-by (:fitness-index ga) >
                                (:population ga))))]
     (if (>= (count p) m)
       (take m p)
       (let [s1 (selection ga)
             s2 (selection ga)
             xs (remove nil? (crossover ga s1 s2 crossover-p))
             ms (map #(mutation ga % mutation-p) xs)
             fs (filter-k-range ga ms)]
         (recur (reduce conj p ms)))))))

(defn descriptive-stats
  ([ga]
   (let [fs (vals (:fitness-index ga))
         [mn q25 q50 q75 mx] (s/quantile fs)]
     [[:date (java.util.Date.)]
      [:generation (:generation ga)]
      [:population-count (count (:population ga))]
      [:min mn]
      [:quantile-25 q25]
      [:quantile-50 q50]
      [:quantile-75 q75]
      [:max mx]
      [:mean (s/mean fs)]
      [:median (s/median fs)]
      [:sd (s/sd fs)]
      [:variance (s/variance fs)]
      [:kurtosis (s/kurtosis fs)]
      [:skewness (s/skewness fs)]])))

(defn describe
  ([stats]
   (doseq [[label value] stats]
     (printf "%24s %s\n" label value))
   (println)))

(defn index-ga
  ([ga]
   (let [c-index (index-clusters ga)]
     (assoc ga
            :cluster-index c-index
            :fitness-index (index-fitness c-index (:population ga))))))

(defn init-ga-cluster
  ([coll m k-range]
   (let [p (rand-population-from m k-range (:data (first coll)))
         c-index (index-population p coll)]
     (GACluster. 0
                 p
                 coll
                 k-range
                 (get-ranges (first coll))
                 c-index
                 (index-fitness c-index p)))))

(defn update-ga
  ([ga new-population]
   (let [data (:data ga)
         new-p (vec (filter-k-range
                      ga (map (comp vec normalize-clusters)
                              (index-population new-population data))))
         c-index (index-population new-p data)]
     (assoc ga
            :generation (inc (:generation ga))
            :population new-p
            :cluster-index c-index
            :fitness-index (index-fitness c-index new-p)))))

(defn add-ids
  ([coll] (mapv (fn [x] {:data x, :hash (System/identityHashCode x)}) coll)))

(defn ga
  ([coll m k-range max-iteration
    & {:keys [stop-fn pass-through crossover-p mutation-p]
       :or {stop-fn (constantly false) pass-through 5
            crossover-p 0.85 mutation-p 0.0075}}]
   (println "caching distance matrix")
   (let [coll (time (doall (distance-matrix (add-ids coll))))
         _ (println "done.")
         init-ga (init-ga-cluster coll m k-range)]
     ;; A quick sanity check.
     (doseq [c coll]
       (assert (= (count (:dist c)) (dec (count coll)))))
     (loop [ga init-ga, history [(descriptive-stats init-ga)]]
       (describe (last history))
       (if (or (>= (:generation ga) max-iteration) (stop-fn ga))
         {:ga ga
          :solutions (get-all-solutions ga)
          :history history}
         (let [p-next (time (do
                              (println :next-population)
                              (next-population ga m pass-through crossover-p mutation-p)))
               next-ga (update-ga ga p-next)]
           (recur next-ga (conj history (descriptive-stats next-ga)))))))))

; }}}

;;;; {{{ Examples
(comment
(require '[incanter.core :as i]
         'incanter.datasets)
(def data (incanter.datasets/get-dataset :iris))
(def iris (i/$map km/->Iris [:Species :Sepal.Length :Sepal.Width
                             :Petal.Length :Petal.Width] data))
(def d-cache (let [dc (distance-matrix iris)] (count dc) dc))

(def gac (init-ga-cluster iris 5 [2 12]))
(describe gac)

(use 'd-mining.ga :reload)
(in-ns 'd-mining.ga)
(def fs (ga iris 200 [2 12] 200))
(.printStackTrace *e)
  )
; }}}

