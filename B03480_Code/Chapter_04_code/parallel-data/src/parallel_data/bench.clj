
(ns parallel-data.bench
  (:use criterium.core
        [parallel-data.hints :only (mean-no-hints mean-hints)]))

#_
(use 'criterium.core)

(defmacro bench-call
  ([name expr]
   `(do
      (println '~name)
      (with-progress-reporting
        (quick-bench
          ~expr))
      (println))))

#_
(defmacro bench-call
  ([name expr]
   `(do
      (println '~name)
      (time ~expr)
      (println))))

