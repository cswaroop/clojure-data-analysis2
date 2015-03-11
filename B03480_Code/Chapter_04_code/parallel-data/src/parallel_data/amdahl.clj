
(ns parallel-data.amdahl
  (:use [incanter core charts]))

(defn amdahls
  ([p n]
   (/ 1 (+ (- 1 p) (/ p n)))))

(defn graph-amdahls
  ([p & {:keys [from to] :or {from 1 to 1000}}]
   (function-plot (partial amdahls p) from to)))


