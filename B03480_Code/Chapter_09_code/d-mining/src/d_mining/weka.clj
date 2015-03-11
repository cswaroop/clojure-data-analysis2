
;;;; {{{ Declarations
(ns d-mining.weka
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.set :as set])
  (:import [weka.associations Apriori]
           [weka.core EuclideanDistance Instances]
           [weka.core.converters ArffLoader CSVLoader]
           [weka.classifiers Evaluation]
           [weka.classifiers.bayes NaiveBayes]
           [weka.classifiers.functions LibSVM]
           [weka.classifiers.trees J48]
           [weka.clusterers
            ClusterEvaluation EM HierarchicalClusterer SimpleKMeans]
           [weka.filters Filter]
           [weka.filters.unsupervised.attribute Remove]
           [weka.filters.unsupervised.instance Resample]
           [java.io File]))
(comment
(require '[clojure.string :as str]
         '[clojure.java.io :as io]
         '[clojure.set :as set])
(import [weka.associations Apriori]
        [weka.core EuclideanDistance Instances]
        [weka.core.converters ArffLoader CSVLoader]
        [weka.classifiers Evaluation]
        [weka.classifiers.bayes NaiveBayes]
        [weka.classifiers.functions LibSVM]
        [weka.classifiers.trees J48]
        [weka.clusterers
         ClusterEvaluation EM HierarchicalClusterer SimpleKMeans]
        [java.io File])
  )
; }}}

;;;; {{{ 09.01 Loading CSV and ARFF Data into Weka
(comment
(import [weka.core.converters ArffLoader CSVLoader]
        [java.io File])
  )

(defn ->options [& opts]
  (into-array
    String
    (map str (flatten (remove nil? opts)))))

(defn load-csv [filename & {:keys [header]
                            :or {header true}}]
  (let [options (->options (when-not header "-H"))
        loader (doto (CSVLoader.)
                 (.setOptions options)
                 (.setSource (File. filename)))]
    (.getDataSet loader)))

(defn load-arff [filename]
  (.getDataSet
    (doto (ArffLoader.)
      (.setFile (File. filename)))))

(comment
(def data (load-csv "data/chn-land.csv"))
(def iris (load-arff "data/UCI/iris.arff"))
  )
; }}}

;;;; {{{ 09.02 Filtering and Renaming Columns in Weka Datasets
(comment
(import [weka.filters Filter]
        [weka.filters.unsupervised.attribute Remove])
(require '[clojure.string :as str])
  )

(defn set-fields [instances field-seq]
  (doseq [n (range (.numAttributes instances))]
    (.renameAttribute instances (.attribute instances n)
                      (name (nth field-seq n)))))

(defn attr-n [instances attr-name]
  (->> instances
    (.numAttributes)
    range
    (map #(vector % (.. instances (attribute %) name)))
    (filter #(= (second %) (name attr-name)))
    ffirst))

(defn filter-attributes [dataset remove-attrs]
  (let [attrs (map inc
                   (map (partial attr-n dataset)
                        remove-attrs))
        options (->options "-R"
                           (str/join \, (map str attrs)))
        rm (doto (Remove.)
             (.setOptions options)
             (.setInputFormat dataset))]
    (Filter/useFilter dataset rm)))

(defn attr-names [dataset]
  (map #(vector % (.. dataset (attribute %) name))
       (range (.numAttributes dataset))))

(defn delete-attrs [instances attr-names]
  (reduce (fn [is n]
            (.deleteAttributeAt is (attr-n is n)) is)
          instances
          attr-names))

(comment
  (map #(.. data (attribute %) name)
       (range (.numAttributes data)))

(set-fields data
            [:country-code :year
             :total-land :agri-percent :agri-total])

(delete-attrs data [:agri-percent])
(def data-numbers (filter-attributes data [:country-code]))

(map #(.. data-numbers (attribute %) name)
     (range (.numAttributes data-numbers)))

(def iris-petal
  (filter-attributes
    iris
    [:sepallength :sepalwidth :class]))
(def iris-sepal (filter-attributes iris [:petallength :petalwidth :class]))
  )
; }}}

;;;; {{{ defanalysis
(defn random-seed [seed]
  (if (nil? seed)
    (.intValue (.getTime (java.util.Date.)))
    seed))

(defn analysis-parameter [parameter]
  (condp = (count parameter)
    ;; [option-str variable-name default-value]
    ;; ["-N" k 2]
    3 `[~(first parameter) ~(second parameter)]

    ;; [option-str variable-name default-value flag]
    ;; ["-V" verbose false :flag-true]
    4 (condp = (last parameter)
        :flag-true `[(when ~(second parameter)
                       ~(first parameter))]
        :flag-false `[(when-not ~(second parameter)
                        ~(first parameter))]
        :not-nil `[(when-not (nil? ~(second parameter))
                     [~(first parameter)
                      ~(second parameter)])]
        :seq (let [name (second parameter)]
               (apply concat
                      (map-indexed
                        (fn [i flag]
                          `[~flag (nth ~name ~i)])
                        (first parameter))))

        `[~(first parameter)
          (~(last parameter) ~(second parameter))])

    ;; [option-str variable-name default-value flag option]
    ;; ["-B" distance-of :node-length :flag-equal
    ;;  :branch-length]
    5 (condp = (nth parameter 3)
        :flag-equal `[(when (= ~(second parameter)
                               ~(last parameter))
                        ~(first parameter))]

        :predicate `[(when ~(last parameter)
                       [~(first parameter)
                        ~(second parameter)])])))

(defmacro defanalysis
  [a-name a-class a-method parameters]
  `(defn ~a-name
     [dataset# &
      ;; The variable-names and default-values are
      ;; used here to build the function's
      ;; parameter list.
      {:keys ~(mapv second parameters)
       :or ~(into {}
                  (map #(vector (second %) (nth % 2))
                       parameters))}]
     ;; The options, flags, and predicats are used to
     ;; construct the options list.
     (let [options# (->options
                      ~@(mapcat analysis-parameter
                                parameters))]
       ;; The algorithm's class and invocation
       ;; function are used here to actually
       ;; perform the processing.
       (doto (new ~a-class)
         (.setOptions options#)
         (. ~a-method dataset#)))))

; }}}

;;;; {{{ 09.03 K-Means Clustering in Weka
(comment
(require '[incanter.core :as i]
         '[incanter.charts :as c])
(import [weka.core EuclideanDistance]
        [weka.clusterers SimpleKMeans])
  )

(defanalysis
  k-means SimpleKMeans buildClusterer
  [["-N" k 2]
   ["-I" max-iterations 100]
   ["-V" verbose false :flag-true]
   ["-S" seed 1 random-seed]
   ["-A" distance EuclideanDistance .getName]])

(comment
(macroexpand-1 '(defanalysis
                  k-means SimpleKMeans buildClusterer
                  [["-N" k 2]
                   ["-I" max-iterations 100]
                   ["-V" verbose false :flag-true]
                   ["-S" seed 1 random-seed]
                   ["-A" distance EuclideanDistance .getName]]))

(defn k-means
  ([dataset__1079__auto__ &
    {:or {k 2, max-iterations 100, verbose false, seed 1,
          distance EuclideanDistance},
     :keys [k max-iterations verbose seed distance]}]
   (let [options__1080__auto__
         (->options "-N" k
                    "-I" max-iterations
                    (when verbose "-V")
                    "-S" (random-seed seed)
                    "-A" (.getName distance))]
     (doto (new SimpleKMeans)
       (.setOptions options__1080__auto__)
       (. buildClusterer dataset__1079__auto__)))))

(defn k-means
  ([dataset & {:keys [k max-iterations verbose seed distance]
               :or {k 2 max-iterations 100 verbose false seed 1
                    distance EuclideanDistance}}]
   (let [options (->options "-N" (str k) "-I" (str max-iterations)
                            (when verbose "-V")
                            "-S" (str (random-seed seed))
                            "-A" (.getName distance))]
     (doto (SimpleKMeans.)
       (.setOptions options)
       (.buildClusterer dataset)))))
  )

(comment

(def km (k-means iris-petal :k 3))

(def iris-clusters
  (->> iris
    (.numInstances)
    (range)
    (map #(vector (.clusterInstance km (.get iris-petal %))
                  (.get iris %)))
    (map (fn [[c i]] {:cluster c
                      :petal-length (.value i 2)
                      :petal-width (.value i 3)
                      :class (int (.value i 4))}))))
(def c-matrix
  (reduce (fn [m {:keys [cluster class]}]
            (assoc m [class cluster] (inc (m [class cluster] 0))))
          {}
          iris-clusters))
(sort c-matrix)
(sort-by (comp second first) c-matrix)
(def by-cluster (group-by :cluster iris-clusters))

  (require '[clojure.data.json :as json]
           '[clojure.java.io :as io])

  (with-open [w (io/writer "iris-clusters.json")]
    (json/write iris-clusters w))

  )
; }}}

;;;; {{{ 09.04 Hierarchical Clustering in Weka
;;; **Link types** are one of :single :complete :average :mean :centroid :ward
;;; :adjcomplete :neighbor-joining.
;;; **distance-of** is one of :node-height or :branch-length.
(comment
(import [weka.core EuclideanDistance]
        [weka.clusterers HierarchicalClusterer])
(require '[clojure.string :as str])
  )

(defanalysis
  hierarchical HierarchicalClusterer buildClusterer
  [["-A" distance EuclideanDistance .getName]
   ["-L" link-type :centroid
    #(str/upper-case (str/replace (name %) \- \_))]
   ["-N" k nil :not-nil]
   ["-D" verbose false :flag-true]
   ["-B" distance-of :node-length :flag-equal :branch-length]
   ["-P" print-newick false :flag-true]])

(comment
(defn hierarchical
  ([dataset & {:keys [k link-type distance verbose distance-of]
               :or {k nil link-type :centroid distance EuclideanDistance
                    verbose false distance-of :node-height}}]
   (let [options (->options (when-not (nil? k) "-N") (when-not (nil? k) (str k))
                            "-L" (str/upper-case (str/replace (name link-type) \- \_))
                            "-A" (.getName distance)
                            (when verbose "-D")
                            (when (= distance-of :branch-length) "-B"))]
     (doto (HierarchicalClusterer.)
       (.setOptions options)
       (.buildClusterer dataset)))))
  )

(comment
(def hc
  (hierarchical iris-petal :k 3 :print-newick true))
(def hc (hierarchical iris :k 4 :print-newick true))
(println hc)
(.get iris 2)
(.clusterInstance hc (.get iris-petal 2))

(reduce (fn [m n]
          (let [c (.clusterInstance hc (.get iris-petal n))]
            (assoc m c (inc (m c 0)))))
        {}
        (range (.numInstances iris-petal)))

(def iris-hierarchy
  (->> iris
    (.numInstances)
    (range)
    (map #(vector (.clusterInstance hc (.get iris-petal %))
                  (.get iris %)))
    (map (fn [[c i]] {:cluster c
                      :petal-length (.value i 2)
                      :petal-width (.value i 3)
                      :class (int (.value i 4))}))))
(def h-matrix
  (reduce (fn [m {:keys [cluster class]}]
            (assoc m [class cluster] (inc (m [class cluster] 0))))
          {}
          iris-hierarchy))
(sort h-matrix)
(sort-by (comp second first) h-matrix)
(def by-cluster (group-by :cluster iris-hierarchy))

  (require '[clojure.data.json :as json]
           '[clojure.java.io :as io])

  (with-open [w (io/writer "charts/iris-hierarchy.json")]
    (json/write iris-hierarchy w))

  )
; }}}

;;;; {{{ 09.05 SOM Clustering
;;; See d-mining.som.
; }}}

;;;; {{{ 09.06 EM Clustering in Weka
(comment
(import [weka.clusterers EM])
  )

(defanalysis
  em EM buildClusterer
  [["-N" k -1]
   ["-I" iterations 100]
   ["-V" verbose false :flag-true]
   ["-M" min-sd 1e-6]
   ["-S" seed nil random-seed]])

(comment
(defn em
  ([dataset & {:keys [k iterations verbose min-sd seed]
               :or {k -1 iterations 100 verbose false min-sd 1e-6 seed nil}}]
   (let [options (->options "-N" (str k) "-I" (str iterations)
                            (when verbose "-V")
                            "-M" (str min-sd)
                            (str (random-seed seed)))]
     (doto (EM.)
       (.setOptions options)
       (.buildClusterer dataset)))))
  )

(comment
(def em-cluster (em data :seed 1))
(.numberOfClusters em-cluster)
(require '[clojure.java.io :as io])
(with-open [w (io/writer "em-cluster.txt")]
  (.write w (str em-cluster)))
  )
; }}}

;;;; {{{ 09.xx Evaluating Clusters with Weka
(comment
(import [weka.core Instances]
        [weka.clusterers
         ClusterEvaluation MakeDensityBasedClusterer]
        [weka.filters Filter]
        [weka.filters.unsupervised.instance Resample])
(require '[clojure.java.io :as io])
  )

(defn evaluate
  ([clusterer dataset]
   (doto (ClusterEvaluation.)
     (.setClusterer clusterer)
     (.evaluateClusterer dataset))))

(defn x-validate
  ([clusterer dataset folds]
   (ClusterEvaluation/crossValidateModel
     clusterer dataset folds (java.util.Random.))))

(defn resample
  ([dataset & {:keys [seed sample-size-percent replacement invert]
               :or {seed 1, sample-size-percent 100,
                    replacement true, invert false}}]
   (let [options (->options "-S" seed
                            "-Z" sample-size-percent
                            (when-not replacement "-no-replacement")
                            (when (and (not replacement) invert) "-V"))
         resampler (doto (Resample.)
                     (.setOptions options)
                     (.setInputFormat dataset))]
     (Filter/useFilter dataset resampler))))

(defn class-freq
  ([clusterer with-class without-class]
   (let [class-attr (.classAttribute with-class),
         row-count (count with-class)]
     (loop [freqs {}, n 0]
       (if (>= n row-count)
         freqs
         (let [c-num (.clusterInstance clusterer
                                       (.get without-class n))
               class-value (.stringValue (.get with-class n)
                                         class-attr)
               key [c-num class-value]]
           (recur
             (assoc-in freqs key (inc (get-in freqs key 0)))
             (inc n))))))))

(defn rand-point-around
  ([centroid-x centroid-y radius]
   [(+ (- centroid-x radius) (* 2 radius (rand)))
    (+ (- centroid-y radius) (* 2 radius (rand)))]))

(comment
(def km-cluster (k-means data :k 7 :seed 1))
(def em-cluster (em data :k 7 :seed 1))

(compare-clusterers
  em-cluster (MakeDensityBasedClusterer. km-cluster))

(def training (resample data :sample-size-percent 40))

(def c-eval (evaluate (em training :k 7 :seed 1) data))

(def c-eval (x-validate (em training :k 7 :seed 1) data 10))
; user=> c-eval
; -147.52727038585937

(def em-cluster (em iris- :seed 1))
(def training (resample iris- :sample-size-percent 40))
(x-validate (em training :k 6 :seed 1) iris- 10)
; -1.9725341935651477
; user=> (class-freq em-cluster iris iris-)
; {5 {"Iris-virginica" 10}, 1 {"Iris-virginica" 26}, 4 {"Iris-versicolor" 23}, 0 {"Iris-virginica" 14, "Iris-versicolor" 27}, 3 {"Iris-setosa" 22}, 2 {"Iris-setosa" 28}}
; user=> (doseq [p (sort *1)] (println p))
; [0 {Iris-virginica 14, Iris-versicolor 27}]
; [1 {Iris-virginica 26}]
; [2 {Iris-setosa 28}]
; [3 {Iris-setosa 22}]
; [4 {Iris-versicolor 23}]
; [5 {Iris-virginica 10}]

(def fake (load-csv "data/clusters.csv"))
(def fake-c (em fake :seed 1))
(def fake-t (resample fake :sample-size-percent 40))
(x-validate (em fake-t :k 4 :seed 1) fake 10)
; -6.163216790119804
  )

; }}}

;;;; {{{ 09.07 Decision Tree Classification

(comment
(import [weka.classifiers.trees J48])
(require '[clojure.java.io :as io])
  )

(defanalysis
  j48 J48 buildClassifier
  [["-U" pruned true :flag-false]
   ["-C" confidence 0.25]
   ["-M" min-instances 2]
   ["-R" reduced-error false :flag-true]
   ["-N" folds 3 :predicate reduced-error]
   ["-B" binary-only false :flag-true]
   ["-S" subtree-raising true :flag-false]
   ["-L" clean true :flag-false]
   ["-A" smoothing true :flag-true]
   ["-J" mdl-correction true :flag-false]
   ["-Q" seed 1 random-seed]])

(comment
(defn j48
  ([dataset & {:keys [pruned confidence min-instances reduced-error folds
                      binary-only subtree-raising clean smoothing
                      mdl-correction seed]
               :or {pruned true confidence 0.25 min-instances 2
                    reduced-error false folds 3 binary-only false
                    subtree-raising true clean true smoothing true
                    mdl-correction true seed 1}}]
   (let [options (->options (when-not pruned "-U")
                            "-C" (str confidence)
                            "-M" (str min-instances)
                            (when reduced-error "-R")
                            (when reduced-error "-N")
                            (when reduced-error (str folds))
                            (when binary-only "-B")
                            (when-not subtree-raising "-S")
                            (when-not clean "-L")
                            (when smoothing "-A")
                            (when-not mdl-correction "-J")
                            (str (random-seed seed)))]
     (doto (J48.)
       (.setOptions options)
       (.buildClassifier dataset)))))
  )

(comment
(def shrooms
  (doto (load-arff "data/UCI/mushroom.arff")
    (.setClassIndex 22)))
(def d-tree (j48 shrooms :pruned true))

(with-open [w (io/writer "decision-tree.gv")]
  (.write w (.graph d-tree)))
  )

; }}}

;;;; {{{ 09.08 Bayesian Network Classification
(comment
(import [weka.classifiers.bayes NaiveBayes]
        [weka.core Instances])
  )

(defanalysis
  naive-bayes NaiveBayes buildClassifier
  [["-K" kernel-density false :flag-true]
   ["-D" discretization false :flag-true]])

(defn sample-instances [instances size]
  (let [inst-count (.numInstances instances)]
    (if (<= inst-count size)
      instances
      (let [indexes
            (loop [sample #{}]
              (if (= (count sample) size)
                (sort sample)
                (recur
                  (conj sample
                        (rand-int inst-count)))))
            sample (Instances. instances size)]
        (doall
          (map #(.add sample (.get instances %))
               indexes))
        sample))))

(comment
(defn naive-bayes
  ([dataset & {:keys [kernel-density discretization]
               :or {kernel-density false discretization false}}]
   (let [options (->options ""
                            (when kernel-density "-K")
                            (when discretization "-D"))]
     (doto (NaiveBayes.)
       (.setOptions options)
       (.buildClassifier dataset)))))
  )

(comment
(def shroom-sample (sample-instances shrooms 2000))
(def bayes (naive-bayes shroom-sample))
(.get shrooms 2)
(.classifyInstance bayes (.get shrooms 2))
(println bayes)

(frequencies
  (map #(vector (.classValue (.get shrooms %))
                (.classifyInstance bayes (.get shrooms %)))
       (range (.numInstances shrooms))))

  )
; }}}

;;;; {{{ 09.09 SVM Classification
(comment
(import [weka.classifiers.functions LibSVM])
  )

(defn bool->int [b] (if b 1 0))
(def svm-types
  {:c-svc 0, :nu-svc 1, :one-class-svm 2,
   :epsilon-svr 3, :nu-svr 4})
(def svm-fns
  {:linear 0, :polynomial 1, :radial-basis 2,
   :sigmoid 3})

(defanalysis
  svm LibSVM buildClassifier
  [["-S" svm-type :c-svc svm-types]
   ["-K" kernel-fn :radial-basis svm-fns]
   ["-D" degree 3]
   ["-G" gamma nil :not-nil]
   ["-R" coef0 0]
   ["-C" c 1]
   ["-N" nu 0.5]
   ["-Z" normalize false bool->int]
   ["-P" epsilon 0.1]
   ["-M" cache-size 40]
   ["-E" tolerance 0.001]
   ["-H" shrinking true bool->int]
   ["-W" weights nil :not-nil]])

(defn eval-instance
  ([] {:correct 0, :incorrect 0})
  ([_] {:correct 0, :incorrect 0})
  ([classifier sums instance]
   (if (= (.classValue instance)
          (.classifyInstance classifier instance))
     (assoc sums
            :correct (inc (sums :correct)))
     (assoc sums
            :incorrect (inc (sums :incorrect))))))

(comment
(defn svm
  ([dataset & {:keys [svm-type kernel-fn degree gamma coef0 c nu normalize
                      epsilon cache-size tolerance shrinking weights]
               :or {svm-type :c-svc kernel-fn :radial-basis degree 3 gamma nil
                    coef0 0 c 1 nu 0.5 normalize false epsilon 0.1
                    cache-size 40 tolerance 0.001 shrinking true weights nil}}]
   (let [types {:c-svc 0, :nu-svc 1, :one-class-svm 2, :epsilon-svr 3,
                :nu-svr 4}
         fns {:linear 0, :polynomial 1, :radial-basis 2, :sigmoid 3}
         options (->options (when-not (= :c-svc svm-type) ["-S" (types svm-type)])
                            (when-not (= :radial-basis kernel-fn) ["-K" (fns kernel-fn)])
                            "-D" degree
                            (when-not (nil? gamma) ["-G" gamma])
                            "-R" coef0
                            "-C" c
                            "-N" nu
                            "-Z" (bool->int normalize)
                            "-P" epsilon
                            "-M" cache-size
                            "-E" tolerance
                            "-H" (bool->int shrinking)
                            (when-not (nil? weights) ["-W" weights]))]
     (doto (LibSVM.)
       (.setOptions options)
       (.buildClassifier dataset)))))
  )

(defn eval-dataset [dataset sample-size]
  (let [sample (sample-instances dataset sample-size)
        trained (svm sample)
        {:keys [correct incorrect] :as totals}
        (reduce (partial eval-instance trained)
                (eval-instance)
                dataset)]
    (assoc totals
           :per-correct (float (/ correct (+ incorrect correct)))
           :svm trained
           :sample sample)))

(comment
  (def abalone (load-csv "data/abalone.data" :header false))
  (set-fields abalone [:sex :length :diameter :height :whole-weight
                       :shucked-weight :viscera-weight :shell-weight :rings])
  (.setClassIndex abalone 0)
  (def asvm (svm abalone))
  (reduce (partial eval-instance asvm) (eval-instance) abalone)

  ;; Much better
(def iris (doto (load-arff "data/UCI/iris.arff")
            (.setClassIndex 4)))
(attr-names iris)
(def iris-sample (sample-instances iris 30))
(dissoc (eval-dataset iris 30) :svm :sample)

  ;; This is the one to use.
(def ion (doto (load-arff "data/UCI/ionosphere.arff")
           (.setClassIndex 34)))
(attr-names ion)
(def ion-sample (sample-instances ion 75))
(def ion-svm (svm ion-sample))
(dissoc (eval-dataset ion 75) :svm :sample)

(let [{:keys [correct incorrect] :as totals}
      (reduce (partial eval-instance ion-svm)
              (eval-instance)
              ion)]
  (assoc totals
         :per-correct (float (/ correct
                                (+ incorrect correct)))))

  )

; }}}

;;;; {{{ 09.10 Apriori
(comment
(import [weka.associations Apriori])
  )

(def rank-metrics
  {:confidence 0 :lift 1 :leverage 2 :conviction 3})
(defanalysis
  apriori Apriori buildAssociations
  [["-N" rules 10]
   ["-T" rank-metric :confidence rank-metrics]
   ["-C" min-metric 0.9]
   ["-D" min-support-delta 0.05]
   [["-M" "-U"] min-support-bounds [0.1 1.0] :seq]
   ["-S" significance nil :not-nil]
   ["-I" output-itemsets false :flag-true]
   ["-R" remove-missing-value-columns
    false :flag-true]
   ["-V" progress false :flag-true]
   ["-A" mine-class-rules false :flag-true]
   ["-c" class-index nil :not-nil]])

(comment
(defn apriori
  ([dataset & {:keys [rules rank-metric min-metric min-support-delta
                      min-support-bounds significance output-itemsets
                      remove-missing-value-columns progress mine-class-rules
                      class-index]
               :or {rules 10 rank-metric :confidence min-metric 0.9
                    min-support-delta 0.05 min-support-bounds [0.1 1.0]
                    significance nil output-itemsets false
                    remove-missing-value-columns false progress false
                    mine-class-rules false class-index nil}}]
   (let [rank-metrics {:confidence 0 :lift 1 :leverage 2 :conviction 3}
         options (->options "-N" rules
                            "-T" (rank-metrics rank-metric)
                            "-C" min-metric
                            "-D" min-support-delta
                            "-U" (second min-support-bounds)
                            "-M" (first min-support-bounds)
                            (when-not (nil? significance) ["-S" significance])
                            (when output-itemsets "-I")
                            (when remove-missing-value-columns "-R")
                            (when progress "-V")
                            (when mine-class-rules "-A")
                            (when-not (nil? class-index) ["-c" class-index]))]
     (doto (Apriori.)
       (.setOptions options)
       (.buildAssociations dataset)))))
  )

(comment
(def a (apriori shrooms))
(doseq [r (.. a getAssociationRules getRules)]
  (println
    (format "%s => %s %s = %.4f"
            (mapv str (.getPremise r))
            (mapv str (.getConsequence r))
            (.getPrimaryMetricName r)
            (.getPrimaryMetricValue r))))

  )

; }}}

