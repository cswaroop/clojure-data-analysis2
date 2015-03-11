
(ns parallel-data.rsum
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv])
  (:require [clojure.core.reducers :as r])
  (:import [java.lang Math]))

(comment
(require '[clojure.java.io :as io]
         '[clojure.data.csv :as csv])
(require '[clojure.core.reducers :as r])
(import '[java.lang Math])
  )

; From Handling Data with STM.

;; From http://census.ire.org/data/bulkdata.html
;; State: Virginia (51)
;; Summary Level: Place (160)
;; Table: P35. FAMILIES
(def data-file "data/all_160_in_51.P35.csv")

(defn lazy-read-csv [csv-file]
  (let [in-file (io/reader csv-file)
        csv-seq (csv/read-csv in-file)
        lazy (fn lazy [wrapped]
               (lazy-seq
                 (if-let [s (seq wrapped)]
                   (cons (first s) (lazy (rest s)))
                   (.close in-file))))]
    (lazy csv-seq)))

(defn with-header [coll]
  (let [headers (map keyword (first coll))]
    (map (partial zipmap headers) (next coll))))

; Now on to the rest of it....

(comment
(def zero-counts {:n (long 0)
                  :s 0.0
                  :mean 0.0
                  :m2 0.0
                  :m3 0.0
                  :m4 0.0})

(defn accum-counts
  ([] zero-counts)
  ([{:keys [n mean m2 m3 m4 s] :as accum} x]
   (let [new-n (long (inc n))
         delta (- x mean)
         delta-n (/ delta new-n)
         delta-n2 (* delta-n delta-n)
         term-1 (* delta delta-n n)
         new-mean (+ mean delta-n)]
     {:n new-n
      :mean new-mean
      :s (+ s x)
      :m2 (+ m2 term-1)
      :m3 (+ m3 (- (* term-1 delta-n (- new-n 2))
                   (* 3 delta-n m2)))
      :m4 (+ m4
             (* term-1 delta-n2 (- (* n n) (+ (* 3 n) 3)))
             (- (* 6 delta-n2 m2) (* 4 delta-n m3)))})))

(defn combine-counts
  ([] zero-counts)
  ([xa xb]
   (let [n (long (op-fields + :n xa xb))
         delta (op-fields - :mean xb xa)
         nxa*xb (*' (:n xa) (:n xb))]
     {:n n
      :mean (+ (:mean xa) (* delta (/ (:n xb) n)))
      :s (op-fields + :s xa xb)
      :m2 (+ (:m2 xa) (:m2 xb)
             (* delta delta (/ nxa*xb n)))
      :m3 (+ (:m3 xa) (:m3 xb)
             (* delta delta delta
                (/ (* nxa*xb (- (:n xa) (:n xb)))
                   (* n n)))
             (* 3 delta
                (/ (- (* (:n xa) (:m2 xb)) (* (:n xb) (:m2 xa)))
                   n)))
      :m4 (+' (:m4 xa) (:m4 xb)
             (*' (Math/pow delta 4)
                (/ (*' nxa*xb
                      (+' (-' (*' (:n xa) (:n xa)) nxa*xb)
                         (*' (:n xb) (:n xb))))
                   (Math/pow n 3)))
             (*' 6 delta delta
                (/ (+' (*' (:n xa) (:n xa) (:m2 xb))
                      (*' (:n xb) (:n xb) (:m2 xa)))
                   (*' n n)))
             (*' 4 delta
                (/ (-' (*' (:n xa) (:m3 xb))
                      (*' (:n xb) (:m3 xa)))
                   n)))})))

(defn stats-from-sums [{:keys [n mean m2 m3 m4 s] :as sums}]
  {:mean (double (/ s n))
   :variance (/ m2 (dec n))
   :skew (/ (* (Math/sqrt n) m3)
            (Math/sqrt (* m2 m2 m2)))})

(defn summary-statistics [coll]
   (stats-from-sums (r/fold combine-counts accum-counts coll)))
  )

(def zero-counts {:n (long 0)
                  :s 0.0
                  :mean 0.0
                  :m2 0.0})

(defn accum-counts
  ([] zero-counts)
  ([{:keys [n mean m2 s] :as accum} x]
   (let [new-n (long (inc n))
         delta (- x mean)
         delta-n (/ delta new-n)
         term-1 (* delta delta-n n)
         new-mean (+ mean delta-n)]
     {:n new-n
      :mean new-mean
      :s (+ s x)
      :m2 (+ m2 term-1)})))

(defn op-fields [op field item1 item2]
  (op (field item1) (field item2)))

(defn combine-counts
  ([] zero-counts)
  ([xa xb]
   (let [n (long (op-fields + :n xa xb))
         delta (op-fields - :mean xb xa)
         nxa*xb (*' (:n xa) (:n xb))]
     {:n n
      :mean (+ (:mean xa) (* delta (/ (:n xb) n)))
      :s (op-fields + :s xa xb)
      :m2 (+ (:m2 xa) (:m2 xb)
             (* delta delta (/ nxa*xb n)))})))

(defn stats-from-sums [{:keys [n mean m2 s] :as sums}]
  {:mean (double (/ s n))
   :variance (/ m2 (dec n))})

(defn summary-statistics [coll]
  (stats-from-sums (r/fold combine-counts accum-counts coll)))

#_
(summary-statistics (repeatedly 1000000 rand))
