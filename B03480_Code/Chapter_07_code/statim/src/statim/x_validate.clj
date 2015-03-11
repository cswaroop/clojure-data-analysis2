
(ns statim.x-validate
  (:require [incanter.core :as i]
            incanter.io
            [incanter.stats :as s]
            [incanter.charts :as c])
  (:use [statim.linear
         :only (*pivot-categories* wb-data wb-pivoted wb-filtered)])
  (:import [java.lang Math]))

(comment
(require
  '[incanter.core :as i]
  'incanter.io
  '[incanter.stats :as s])
(import [java.lang Math])
(use '[statim.linear
       :only (*pivot-categories* wb-data wb-pivoted wb-filtered)])
(def world-bank
  (incanter.io/read-dataset "data/world-bank-filtered.csv"
                            :header true))
  )

(defn partition-seq
  "This does a lazy sequence, partitioning coll by size n. If the
  modulus (rem) is non-zero, and item is added to the partition."
  [n rem coll]
  (let [[larger right] (split-at (* (inc n) rem) coll)]
    (concat (partition-all (inc n) larger)
            (partition-all n right))))

(defn partition-folds
  "This returns partitions of row indexes. This takes the
  results of dividing by k and spreads the modulus over the
  partitions.

  `c` is the number of items in the dataset;
  `k` is the number of partitions."
  ([c k]
   (partition-seq (Math/floor (/ c k)) (mod c k) (range c))))

(defn step-folds-seq
  "This builds the sequence use by step-folds."
  ([folds data steps]
   (lazy-seq
     (when-let [[s & ss] (seq steps)]
       (let [prefix (take s folds)
             [validation & suffix] (drop s folds)
             training (flatten (concat prefix suffix))
             current [(i/sel data :rows validation)
                      (i/sel data :rows training)]]
         (cons current (step-folds-seq folds data ss)))))))

(defn step-folds
  "This takes a dataset and a sequence of sequence of indices
  (the folds), and it returns a sequence of pairs of
  validation subset and training subset. It walks through each
  subsequence of the folds and pulls that item out as the
  validation set and uses the rest of the items as the
  training set. It returns the slices from the original
  dataset. Essentially, this reifies the fold partitions used
  in K-fold cross-validation."
  ([folds] (step-folds folds i/$data))
  ([folds data] (step-folds-seq folds data (range (count folds)))))

(defn k-fold
  ([train error combine] (k-fold train error combine 10 i/$data))
  ([train error combine k] (k-fold train error combine k i/$data))
  ([train error combine k input-data]
   (->> input-data
     i/to-list
     shuffle
     (i/dataset (i/col-names input-data))
     (step-folds (partition-folds (i/nrow input-data) k))
     (map (fn [[v t]] [v (train t)]))
     (map (fn [[v t]] (error t v)))
     (reduce combine (combine)))))

(def ^:dynamic *independent-vars*
  (map keyword
       (conj (filter #(.startsWith (name %) "SI.DST.")
                     *pivot-categories*)
             :SI.POV.GINI)))
(def ^:dynamic *dependent-var* :VC.IHR.PSRC.P5)

(defn train-vars
  "This performs the linear regression training for every
  dependent variable. It adds the key `:independent-var` to
  the output of each in order to record the col key used."
  ([training-set]
   (let [dep-var (i/sel training-set :cols *dependent-var*)]
     (map #(assoc (s/linear-model dep-var
                                  (i/sel training-set :cols %))
                  :independent-var %)
          *independent-vars*))))

(defn f
  ([b m] (partial f b m))
  ([b m x] (+ b (* m x))))
(defn sse
  ([actuals estimates]
   (i/sum-of-squares (i/minus actuals estimates))))
(defn ssr
  ([actuals estimates]
   (i/sum-of-squares (i/minus estimates (s/mean actuals)))))
(defn sst
  ([actuals estimates]
   (+ (ssr actuals estimates) (sse actuals estimates))))
(defn r-square
  ([actuals estimates]
   (let [ssr' (ssr actuals estimates)
         sse' (sse actuals estimates)
         sst' (+ ssr' sse')]
   (/ ssr' sst'))))
(defn validate
  ([trained validation-set dep-values]
   (let [{:keys [coefs independent-var]} trained
         model (apply f coefs)
         ind-values (i/sel validation-set :cols independent-var)]
     (assoc trained
            :validation (r-square dep-values
                                  (map model ind-values))))))
(defn training-errors
  "This looks at the output of `train-vars` and orders
  everything from best to worse, based on the r^2 calculated
  from the validation set."
  ([trained validation]
   (let [dep-var (i/sel validation :cols *dependent-var*)]
     (sort-by :validation
              (map #(validate % validation dep-var) trained)))))

(defn training-inner
  "This is used by training-combine to handle the data reduction."
  ([totals validated-item]
   (let [{:keys [validation independent-var]} validated-item
         {:keys [n sum] :or {n 0, sum 0.0}}
         (get totals independent-var)]
     (assoc totals
            independent-var
            {:n (inc n) :sum (+ sum validation)}))))
(defn training-combine
  "This combines two items by keeping the data necessary to
  compute the average for each independent variable key seen."
  ([] {})
  ([totals validated] (reduce training-inner totals validated)))

;;; Interesting, so the better predictor of the homicide rate
;;; is :SI.DST.FRST.10, "Income share held by lowest 10%."
;;; It's an inverse relationship: as this value goes up, the
;;; homicide rate goes down. This (and the share held by the
;;; lowest 20%) is a better predictor than the GINI.
(comment
(def world-bank wb-filtered)
(def kf (k-fold train-vars training-errors training-combine
                10 world-bank))
(dorun (->> kf
         (map (fn [[k {:keys [n sum]}]] [k (float (/ sum n))]))
         (sort-by second)
         reverse
         (map println)))
  )

