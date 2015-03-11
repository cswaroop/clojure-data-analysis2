
(ns statim.outliers-spec
  (:use speclj.core
        (incanter core stats)
        statim.outliers)
  (:import [java.lang Math]))

(def ^:dynamic *sample-size* 100)

(defn approx=
  ([delta] (partial approx= delta))
  ([delta a b]
   (<= (Math/abs (- a b)) delta)))

(defn all-approx=
  ([delta xs ys]
   (reduce #(and %1 (apply approx= delta %2)) true (map vector xs ys))))

(describe
  "find-outliers"
  (it "should remove outliers from an input collection."
      (should=
        [9 10 10 10 11]
        (rm-chauvenet-outlier [9 10 10 10 11 50]))))

(run-specs)

