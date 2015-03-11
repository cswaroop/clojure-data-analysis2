
; {{{ Declarations
(ns d-mining.k-prototypes
  (:require [incanter.core :as i]
            [incanter.stats :as s]
            incanter.io
            [d-mining.k-means :as km]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.tools.trace :as trace]
            [clojure.set :as set]))

(comment
(use 'd-mining.k-prototypes :reload)
(in-ns 'd-mining.k-prototypes)
(require '[incanter.core :as i]
         '[incanter.stats :as s]
         'incanter.io
         '[d-mining.k-means :as km])
  )
; }}}

;;;; {{{ K-prototypes clustering
;;; {{{ Protocols
(defprotocol Categorical
  (get-c-dim [c]
    "This returns the number of dimensions of categorical data.")
  (get-categories [c]
    "This returns a vector of the categorical data in the item."))

(defprotocol Updatable
  (update-point [updatable pairs]
    "This updates the point corresponding to the index in the location.")
  (update-category [updatable pairs]
    "This updates the category corresponding to the index in the location."))
; }}}

;;; {{{ Some utilities

(defn any?
  ([coll] (reduce #(or %1 %2) false coll))
  ([pred coll] (any? (map pred coll))))

(defn all?
  ([coll] (reduce #(and %1 %2) true coll))
  ([pred coll] (all? (map pred coll))))

(defn transpose
  ([coll]
   (if (empty? coll)
     coll
     (apply mapv vector coll))))

(defn modify-field
  "This updates a field using a function."
  ([obj field fn]
   (try
     (assoc obj field (fn (get obj field)))
     (catch Exception exc
       (println :ERROR :modify-field obj field (get obj field))
       (throw exc)))))

(defn rm-first
  ([x xs]
   (loop [i 0]
     (cond
       (>= i (count xs)) xs
       (= (nth xs i) x) (vec (concat (subvec xs 0 i) (subvec xs (inc i))))
       :else (recur (inc i))))))

(defn cmp-cats
  "This compares two values for a category. It returns 0 if the two are the
  same, 1 if not."
  ([x y] (if (= x y) 0 1)))

(defn sum
  ([coll] (apply + coll)))

(defn tee
  ([tag] (partial tee tag))
  ([tag o]
   (println (str tag \: \space o))))

(defn assoc-many
  ([coll kvs]
   (if (empty? kvs)
     coll
     (apply assoc coll kvs))))

; }}}

;;; {{{ Cluster type and operations.

(defrecord Cluster
  [id point-prototype category-prototype members])

(defmethod print-method Cluster
  ([o w]
   ; (print-simple (str "#<Cluster: " (count (:members o)) " members>") w)))
   (print-simple (str "#<Cluster: " (:id o) \space
                      (count (:members o)) \space
                      (:point-prototype o) \space
                      (:category-prototype o) \>)
                 w)))

(defn create-cluster
  "This creates a cluster from a Locatable and Categorical."
  ([id point]
   (Cluster. id (km/get-point point) (get-categories point) #{})))

(defn update-cluster
  "This updates the prototypes in a cluster from the members."
  ([cluster]
   (let [members (:members cluster)
         member-count (count members)]
     (if (zero? member-count)
       cluster
       (assoc cluster
              :point-prototype
              (->> members
                (map km/get-point)
                transpose
                (mapv #(/ (sum %) member-count)))
              :category-prototype
              (->> members
                (map get-categories)
                transpose
                (mapv #(ffirst (sort-by second > (frequencies %))))))))))

;; {{{ Monoid protocol
(defn empty-cluster
  "This returns an empty cluster."
  ([] (Cluster. nil [] [] #{})))

(defn empty-cluster?
  ([c] (nil? (:id c))))

(defn join-clusters
  "This joins two clusters."
  ([a b]
   (cond
     (empty-cluster? a) b
     (empty-cluster? b) a
     :else (update-cluster
             (Cluster. (:id a)
                       (:point-prototype a)
                       (:category-prototype a)
                       (set/union (:members a) (:members b)))))))
; }}}

(defn insert-item
  "This inserts an item into a cluster and updates the cluster."
  ([cluster item]
   (modify-field cluster :members #(conj % item))))
   ; (update-cluster (modify-field cluster :members #(conj % item)))))

(defn remove-item
  "This removes an item from a cluster and updates the cluster."
  ([cluster item]
   (modify-field cluster :members #(disj % item))))
   ; (update-cluster (modify-field cluster :members #(disj % item)))))

(defn transfer-item
  "This moves an item from one cluster to another, updating both."
  ([from to item] [(remove-item from item) (insert-item to item)]))

(defn get-distance*
  ([cluster item gamma]
   (get-distance* (km/get-point item)
                  (get-categories item)
                  (:point-prototype cluster)
                  (:category-prototype cluster)
                  gamma))
  ([p1 c1 p2 c2 gamma]
   (+ (sum (map #(Math/pow (- %1 %2) 2) p2 p1))
      (* gamma (sum (map cmp-cats c2 c1))))))

(defn get-distance
  "(2.3) d(Xi, Qi)

  This returns the distance between two points, taking into account both
  numerical data (get-point) and categorical data (get-categories). gamma is
  the weight for categorical data."
  ([cluster item gamma]
   (get-distance* (km/get-point item)
                  (get-categories item)
                  (:point-prototype cluster)
                  (:category-prototype cluster)
                  gamma)))

(defn cluster-distance*
  ([cluster-a cluster-b gamma]
   (get-distance* (:point-prototype cluster-a)
                  (:category-prototype cluster-a)
                  (:point-prototype cluster-b)
                  (:category-prototype cluster-b)
                  gamma)))

(defn get-member-distances
  ([cluster gamma]
   (let [members (:members cluster)]
     (map #(get-distance cluster % gamma) members))))

(defn get-member-distances*
  ([cluster gamma]
   (let [members (:members cluster)]
     (map #(get-distance cluster % gamma) members))))

;;; TODO: Not sure what number this gives. The results seem very close to the
;;; counts.
(defn get-dispersion*
  "(2.4) El

  This finds the within-cluster-disperion for a set of clusters.

  * gamma is the weight for categorical data."
  ([cluster gamma]
   (let [point-p (:point-prototype cluster)
         cat-p (:category-prototype cluster)
         members (:members cluster)
         dist-p (fn [x]
                  (sum
                    (map #(Math/pow (- %1 %2) 2)
                         (km/get-point x)
                         point-p)))
         dist-c (fn [x]
                  (cmp-cats (get-categories x) cat-p))]
     (+ (sum (map dist-p members))
        (* gamma (sum (map dist-c members)))))))

(defn get-dispersion
  "(2.4) El

  This finds the within-cluster-disperion for a set of clusters.

  * gamma is the weight for categorical data."
  ([cluster gamma]
   (let [point-p (:point-prototype cluster) 
         cat-p (:category-prototype cluster) 
         members (:members cluster) 
         dist-p (fn [x]
                  (sum
                    (map #(Math/pow (- %1 %2) 2)
                         (km/get-point x)
                         point-p)))
         dist-c (fn [x]
                  (cmp-cats (get-categories x) cat-p))]
     (+ (sum (map dist-p members))
        (* gamma (sum (map dist-c members)))))))

(defn distance-from-others*
  ([clusters cluster gamma]
   [cluster
    (map #(cluster-distance* cluster % gamma)
         (filter (partial not= cluster) clusters))]))

(defn inter-cluster-distances*
  ([clusters gamma]
   (apply hash-map
          (mapcat #(distance-from-others* clusters % gamma)
                  clusters))))

(defn clear-members
  ([cluster] (assoc cluster :members #{})))

(defn freeze
  "This freezes the transients in the cluster."
  ([cluster]
   (Cluster. (:id cluster)
             (persistent! (:point-prototype cluster))
             (persistent! (:category-prototype cluster))
             (persistent! (:members cluster)))))

(defn thaw
  "This makes the input cluster transient."
  ([cluster]
   (Cluster. (:id cluster)
             (transient (:point-prototype cluster))
             (transient (:category-prototype cluster))
             (transient (:members cluster)))))
; }}}

;;; {{{ Utilities and Miscellaneous Functions

(defn mode
  ([coll]
   (let [[[x xf] & xs] (sort-by second > (frequencies coll))]
     (into [x]
           (map first
                (take-while #(= xf (second %)) xs))))))

(defn cost
  "(2.6) E

  * gamma is the weight for categorical data.
  * clusters is a map from prototypes to items for each cluster."
  ([clusters gamma]
   (sum (map #(get-dispersion % gamma) clusters))))

(defn select-k
  "Randomly select k items from the collection."
  ([k coll]
   (if (= (count coll) k)
     coll
     (loop [sample #{}]
       (if (= (count sample) k)
         (vec sample)
         (recur (conj sample (rand-nth coll))))))))

(defn nearest-prototype*
  ([clusters item gamma]
   (->> clusters
     (map #(vector % (get-distance* % item gamma)))
     (apply min-key second)
     first)))

(defn nearest-prototype
  ([clusters item gamma]
   (->> (vals clusters)
     (map #(vector % (get-distance % item gamma)))
     (apply min-key second)
     first)))

(defn rekey-with
  "This removes the value of key-a from m, runs it through fn, and adds it back
  to m under key-b."
  ([m key-a key-b] (rekey-with m key-a key-b identity))
  ([m key-a key-b fn]
   (dissoc (assoc m key-b (fn (get m key-a))) key-a)))

(defn update-clusters
  ([clusters]
   (reduce (fn [cs [k v]] (assoc cs k (update-cluster v))) {} clusters)))

(defn allocate-item
  ([gamma clusters item]
   (let [nearest (nearest-prototype clusters item gamma)]
     (assoc clusters (:id nearest) (insert-item nearest item)))))

(defn allocate-items
  ([clusters coll gamma]
   (reduce (partial allocate-item gamma) clusters coll)))

(defn index-clusters
  ([clusters]
   (reduce (fn [cs c] (assoc cs (:id c) c)) {} clusters)))

(defn clear-all-members
  ([cluster-map]
   (index-clusters (map clear-members (vals cluster-map)))))

(defn member-pairs
  ([cluster]
   (map (partial vector cluster) (:members cluster))))

(defn reallocate-reduce
  ([gamma state task]
   (let [[moves clusters] state
         [current item] task
         nearest (nearest-prototype clusters item gamma)]
     ; (println :reallocate-reduce (:id current) '=> (:id nearest))
     [(if (= (:id nearest) (:id current)) moves (inc moves))
      (assoc clusters (:id nearest) (insert-item nearest item))])))

(defn reallocate-items
  ([clusters gamma]
   (->>
     clusters
     vals
     (mapcat member-pairs)
     (reduce (partial reallocate-reduce gamma)
             [0 (clear-all-members clusters)]))))

(defn cluster-counts
  ([clusters]
   (map (comp count :members) clusters)))

(defn cluster=
  "Cluster equality ignoring the id field."
  ([cluster-a cluster-b]
   (and (= (:members cluster-a) (:members cluster-b))
        (= (:point-prototype cluster-a) (:point-prototype cluster-b))
        (= (:category-prototype cluster-a) (:category-prototype cluster-b)))))

(defn cluster-in
  "Tests whether a cluster is in an index, based on cluster=."
  ([c cs]
   (any? (map (partial cluster= c) (vals cs)))))

(defn cluster-indexes=
  "Tests whether all the items in index-a are in index-b, according to
  cluster=."
  ([index-a index-b]
   (->>
     index-a
     vals
     (map #(cluster-in % index-b))
     all?)))

; }}}

;;; {{{ The primary k-partition function.

;; Notes:

;; * The gamma (the categorical weight) is related to the standard deviation of
;; the numeric attributes for each cluster. As a stand-in, the overall standard
;; deviation of numerical attributes can be used to guide in determining this
;; value.

(defn k-prototypes
  ([coll]
   (k-prototypes (Math/ceil (Math/sqrt (/ (count coll) 2.0)))
                 coll
                 :gamma 1.0))
  ([k coll & {:keys [gamma] :or {gamma 1.0}}]
   (let [initial (map create-cluster (range) (select-k k coll))
         _ (do
             (println :initial-clusters)
             (doseq [c initial]
               (println \tab c)))
         clusters (update-clusters
                    (allocate-items
                      (index-clusters initial) coll gamma))]
     (loop [i 1, moves (count coll), clusters clusters]
       (println :i i (java.util.Date.))
       (println :moves moves '/ (sum (cluster-counts (vals clusters))))
       (println :counts (cluster-counts (vals clusters)))
       ; (doseq [[_ c] clusters] (println \tab c))
       (let [[moves new-clusters] (reallocate-items clusters gamma)
             updated-clusters (update-clusters new-clusters)]
         ; (if (or (zero? moves) (cluster-indexes= clusters updated-clusters))
         (if (zero? moves)
           updated-clusters
           (recur (inc i) moves updated-clusters)))))))
; }}}
; }}}

;;;; {{{ Cluster analysis
(defn describe
  ([coll]
   (let [[mn q25 q50 q75 mx] (s/quantile coll)]
     [[:count (count coll)]
      [:min mn]
      [:quantile-25 q25]
      [:quantile-50 q50]
      [:quantile-75 q75]
      [:max mx]
      [:mean (s/mean coll)]
      [:median (s/median coll)]
      ; [:mode (mode coll)]
      [:sd (s/sd coll)]
      [:variance (s/variance coll)]
      [:kurtosis (s/kurtosis coll)]
      [:skewness (s/skewness coll)]])))

(defn cluster-report
  ([clusters gamma]
   (let [inter (inter-cluster-distances* (vals clusters) gamma)]
     (println :population (sum (map (comp count :members) (vals clusters))))
     (println :cluster-count (count clusters))
     (doseq [c (vals clusters)]
       (println :prototype (:point-prototype c) (:category-prototype c))
       (printf "%13s %s\n" :dispersion (get-dispersion* c gamma))

       (println :intra-cluster)
         (doseq [[f v] (describe (get-member-distances* c gamma))]
         (printf "%13s %s\n" f v))

       (println :inter-cluster)
       (doseq [[f v] (describe (get inter c))]
         (printf "%13s %s\n" f v))

       (println)))))

; }}}

;;;; {{{ Examples
;;; {{{ Mushroom Data Set from http://archive.ics.uci.edu/ml/datasets/Mushroom
;; TODO: Get proper citations from the site.
(def col-names [:cap-shape :cap-surface :cap-color
                :bruises? :odor
                :gill-attachment :gill-spacing :gill-size :gill-color
                :stalk-shape :stalk-root
                :stalk-surface-above-ring :stalk-surface-below-ring
                :stalk-color-above-ring :stalk-color-below-ring
                :veil-type :veil-color :ring-number :ring-type
                :spore-print-color :population :habitat])
(def col-ranges {:cap-shape                (mapv str "bcxfks")
                 :cap-surface              (mapv str "fgys")
                 :cap-color                (mapv str "nbcgrpuewy")
                 :bruises?                 (mapv str "tf")
                 :odor                     (mapv str "alcyfmnps")
                 :gill-attachment          (mapv str "adfn")
                 :gill-spacing             (mapv str "cwd")
                 :gill-size                (mapv str "bn")
                 :gill-color               (mapv str "knbhgropuewy")
                 :stalk-shape              (mapv str "et")
                 :stalk-root               (mapv str "bcuezr?")
                 :stalk-surface-above-ring (mapv str "fyks")
                 :stalk-surface-below-ring (mapv str "fyks")
                 :stalk-color-above-ring   (mapv str "nbcgopewy")
                 :stalk-color-below-ring   (mapv str "nbcgopewy")
                 :veil-type                (mapv str "pu")
                 :veil-color               (mapv str "nowy")
                 :ring-number              (mapv str "not")
                 :ring-type                (mapv str "ceflnpsz")
                 :spore-print-color        (mapv str "knbhrouwy")
                 :population               (mapv str "acnsvy")
                 :habitat                  (mapv str "glmpuwd")})

(defrecord Mushroom
  [edible?
   cap-shape cap-surface cap-color
   bruises? odor
   gill-attachment gill-spacing gill-size gill-color
   stalk-shape stalk-root
   stalk-surface-above-ring stalk-surface-below-ring
   stalk-color-above-ring stalk-color-below-ring
   veil-type veil-color ring-number ring-type
   spore-print-color population habitat]

  d_mining.k_means.Locatable
  (get-point [_] [])
  (get-dim [_] 0)
  (get-range [_ i] (col-ranges (nth col-names i)))
  (make-random-point [m]
    (->> col-names
      count
      range
      (map (comp rand-nth (partial km/get-range m)))
      (apply ->Mushroom)))

  Categorical
  (get-c-dim [_] (count col-names))
  (get-categories [m] (mapv (partial get m) col-names))

  Updatable
  (update-point [_ _]
    nil)
  (update-category [updatable pairs]
    (let [[index value & _] pairs]
      (assoc updatable (nth col-names index) value))))

(defmethod print-method Mushroom
  ([o w]
   (print-simple
     (str "#<Mushroom " (get-categories o) \>)
     w)))

(defn cluster-shrooms
  ([] (cluster-shrooms nil 1.0))
  ([k gamma]
   (let [data-file "data/agaricus-lepiota.data"
         data (incanter.io/read-dataset data-file :header false)
         mrooms (map (partial apply ->Mushroom) (i/to-list data))
         k (if (nil? k) (Math/ceil (Math/sqrt (/ (count mrooms) 2.0))) k)]
     (println :mushrooms \tab :k k \tab :count (count mrooms))
     (k-prototypes k mrooms :gamma gamma))))

(defn report-shrooms
  ([clusters]
   (doseq [c (vals clusters)]
     (println c)
     (doseq [[edible? number] (frequencies (map :edible? (:members c)))]
       (println \tab edible? number))
     (println))))

; }}}

;;; {{{ Person: For playing around
(defrecord Person
  [given-name surname age]

  d_mining.k_means.Locatable
  (get-point [p] [(:age p)])
  (get-dim [_] 1)
  (get-range [_ _] [0 100])
  (make-random-point [p]
    (let [[from to] (km/get-range p 1)]
      (assoc p :age (+ from (rand-int (- to from))))))

  Categorical
  (get-c-dim [_] 2)
  (get-categories [p] [(:given-name p) (:surname p)])

  Updatable
  (update-point [p pairs] (assoc p :age (second pairs)))
  (update-category [p pairs]
    (let [[i v & _] pairs]
      (assoc p (nth [:given-name :surname] i) v))))

(comment
(def p (Person. "eric" "rochester" 42))
(def ps [p (Person. "jackie" "rochester" 42) (Person. "melina" "rochester" 4) (Person. "micah" "rochester" 3) (Person. "elsa" nil 7) (Person. "melina" "mcdonald" 13)])
  )
; }}}

;;; {{{ Abalone data
;;; http://archive.ics.uci.edu/ml/datasets.html
(def abalone-points [:length :diameter :height
                     :whole-weight :shucked-weight :viscera-weight
                     :sell-weight :rings])

(defn nil->zero
  ([x] (if (nil? x) 0 x)))

(defrecord Abalone
  [sex length diameter height whole-weight shucked-weight viscera-weight
   shell-weight rings]

  d_mining.k_means.Locatable
  (get-point [a]
    (mapv (comp nil->zero (partial get a)) abalone-points))
  (get-dim [_] 8)
  (get-range [_ i]
    (let [ranges [[0.07 0.9] [0.5 0.7] [0.0 1.15]
                  [0.0 3.0] [0.0 1.5] [0.0 0.8]
                  [0.0 1.1] [1 30]]]
      (ranges i)))
  (make-random-point [a] (km/random-point a))

  Categorical
  (get-c-dim [_] 1)
  (get-categories [a] [(:sex a)])

  Updatable
  (update-point [a pairs]
    (apply assoc a
           (mapcat (fn [[i v]] [(nth abalone-points i) v])
                   pairs)))
  (update-category [a pairs] (assoc a :sex (second (first pairs)))))

(defn cluster-abalone
  ([] (cluster-abalone 64 1.0))
  ([k gamma]
   (let [data-file "data/abalone.data"
         data (i/dataset
                (concat [:sex] abalone-points)
                (i/to-list
                  (incanter.io/read-dataset data-file :header false)))
         abalone (map (partial apply ->Abalone) (i/to-list data))]
     (k-prototypes k abalone :gamma gamma))))
; }}}

;;; {{{ Plant data
;;; http://archive.ics.uci.edu/ml/datasets.html

(def plant-cols
  [:ab :ak :ar :az :ca :co :ct :de :dc :fl :ga :hi :id :il :in :ia :ks :ky :la
   :me :md :ma :mi :mn :ms :mo :mt :ne :nv :nh :nj :nm :ny :nc :nd :oh :ok :or
   :pa :pr :ri :sc :sd :tn :tx :ut :vt :va :vi :wa :wv :wi :wy :al :bc :mb :nb
   :lb :nf :nt :ns :nu :on :Prince :qc :sk :yt :dengl :fraspm])

(defrecord Plant
  [latin-name
   ab ak ar az ca co ct de dc fl ga hi id il in ia ks ky la
   me md ma mi mn ms mo mt ne nv nh nj nm ny nc nd oh ok or
   pa pr ri sc sd tn tx ut vt va vi wa wv wi wy al bc mb nb
   lb nf nt ns nu on Prince qc sk yt dengl fraspm]

  d_mining.k_means.Locatable
  (get-point [_] [])
  (get-dim [_] 0)
  (get-range [_ _] nil)
  (make-random-point [_] nil)

  Categorical
  (get-c-dim [_] (count plant-cols))
  (get-categories [a] (mapv (partial get a) plant-cols))

  Updatable
  (update-point [a _] a)
  (update-category [a pairs]
    (apply assoc a
           (mapcat (fn [[i v]] [(nth plant-cols i) v])
                   pairs))))

(defn sample
  ([n coll]
   (loop [s #{}, input (vec coll)]
     (if (< (count s) n)
       (recur (conj s (rand-nth input)) input)
       (vec s)))))

(defn cluster-plants
  ([] (cluster-plants 32 1.0 nil))
  ([k gamma] (cluster-plants k gamma nil))
  ([k gamma sample-size]
   (let [data-file "data/plants.data"
         data (apply hash-map
                     (mapcat (fn [row] [(first row) (set (map keyword (rest row)))])
                             (with-open [f-in (io/reader data-file)]
                               (doall (csv/read-csv f-in)))))
         plants (->>
                  data
                  (map (fn [[k v-set]]
                         (into [k] (map #(if (contains? v-set %) \t \f) plant-cols))))
                  (map (partial apply ->Plant)))
         s (if (nil? sample-size) plants (sample sample-size plants))]
     (k-prototypes k s :gamma gamma))))

(defn report-plants
  ([clusters]
   (doseq [c (vals clusters)]
     (println c)
     (doseq [[latin-name number] (frequencies (map :latin-name (:members c)))]
       (println \tab latin-name number))
     (println))))

; }}}
; }}}

;;;; {{{ -main
(defn -main
  ([]
   (let [
         clusters (cluster-shrooms 8 1.0)
         ; clusters (cluster-abalone 32 1.0)
         ; clusters (cluster-plants 32 1.0 7500)
         ]
     (cluster-report clusters 1.0)

     ; (report-plants clusters)
     (report-shrooms clusters)

     )))
; }}}
