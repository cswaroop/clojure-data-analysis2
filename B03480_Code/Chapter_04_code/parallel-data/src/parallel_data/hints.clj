
(ns parallel-data.hints)

;;;; mean
(defn mean-no-hints
  ([xs]
   (/ (reduce + 0 xs) (count xs))))

(defn mean-hints
  (^double [xs]
     (/ (double (reduce + (double 0) ^doubles (to-array xs)))
        (double (count xs)))))

(def without-hints mean-no-hints)
(def with-hints mean-hints)

;;;; Monte Carlo PI
(defn rand-point [] [(rand) (rand)])

(defn center-dist [[x y]]
  (Math/sqrt (+ (Math/pow x 2.0) (Math/pow y 2.0))))

(defn mc-pi [n]
  (let [in-circle (->> (repeatedly n rand-point)
                       (map center-dist)
                       (filter #(<= % 1.0))
                       count)]
    (* 4.0 (/ in-circle n))))

(defn center-dist-hint
  (^double [[x y]]
     (Math/sqrt (+ (Math/pow (double x) (double 2.0))
                   (Math/pow (double y) (double 2.0))))))

(defn mc-pi-hint
  (^double [n]
     (let [in-circle (double (->> (repeatedly n rand-point)
                                  (map center-dist-hint)
                                  (filter #(<= % (double 1.0)))
                                  count))]
       (double (* (double 4.0) (/ in-circle (double n)))))))

(comment
(use 'criterium.core)
(import [java.lang Math])
(def data (repeatedly 10000 rand))
(bench (without-hints data))
(bench (with-hints data))
  )

;;;; user=> (report-result (benchmark (mc-pi 100000)))
;;;; Evaluation count : 1320 in 60 samples of 22 calls.
;;;;              Execution time mean : 47.615120 ms
;;;;     Execution time std-deviation : 610.350153 us
;;;;    Execution time lower quantile : 46.690273 ms ( 2.5%)
;;;;    Execution time upper quantile : 48.966694 ms (97.5%)
;;;; nil
;;;; user=> (report-result (benchmark (mc-pi-hint 100000)))
;;;; Evaluation count : 1740 in 60 samples of 29 calls.
;;;;              Execution time mean : 35.626201 ms
;;;;     Execution time std-deviation : 608.936601 us
;;;;    Execution time lower quantile : 34.653523 ms ( 2.5%)
;;;;    Execution time upper quantile : 36.880537 ms (97.5%)
;;;; nil
