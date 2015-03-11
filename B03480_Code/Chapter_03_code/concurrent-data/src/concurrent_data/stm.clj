
;; NB: Do this one for 03.01.
(ns concurrent-data.stm
  (:use [concurrent-data.agents :only (data-file sum-items)]
        [concurrent-data.utils :only (lazy-read-csv with-header)]))

(def total-hu (ref 0))
(def total-fams (ref 0))

(defn update-totals [fields items]
  (let [mzero (mapv (constantly 0) fields)
        [sum-hu sum-fams] (sum-items mzero fields items)]
    (dosync
      (alter total-hu #(+ sum-hu %))
      (alter total-fams #(+ sum-fams %)))))

(defn thunk-update-totals-for [fields data-chunk]
  (fn [] (update-totals fields data-chunk)))

(defn main
  ([data-file] (main data-file [:HU100 :P035001] 5))
  ([data-file fields chunk-count]
   (doall
     (->>
       (lazy-read-csv data-file)
       with-header
       (partition-all chunk-count)
       (map (partial thunk-update-totals-for fields))
       (map future-call)
       (map deref)))
   (float (/ @total-fams @total-hu))))

