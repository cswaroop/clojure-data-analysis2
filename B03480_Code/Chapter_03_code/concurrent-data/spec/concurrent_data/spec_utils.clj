
(ns concurrent-data.spec-utils
  (:import [java.lang Math]))

(defn approx=
  ([delta] (partial approx= delta))
  ([delta a b]
   (<= (Math/abs (- a b)) delta)))

