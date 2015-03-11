
(ns parallel-data.utils)

(defn raw-time [f & args]
  (let [start (. System (nanoTime))
        ret (apply f args)]
    [(/ (double (- (. System (nanoTime)) start)) 1000000.0) ret]))

