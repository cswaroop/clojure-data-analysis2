
(ns concurrent-data.side-effects
  (:use concurrent-data.utils))

#_
(require '[clojure.java.io :as io]
         '[clojure.data.csv :as csv])

;; Bad agent. It handles the IO, and it doesn't limit its output.
(defn agent-a [input-file]
  (let [in-file (io/reader input-file)
        csv-rows (csv/read-csv in-file)
        reader (agent csv-rows)
        processor (agent [])

        read-row (fn read-row [rows]
                   (if-let [[item & items] (seq rows)]
                     (do
                       (send processor conj item)
                       (send *agent* read-row)
                       items)
                     (do
                       (.close in-file)
                       '())))]
    (send reader read-row)))

;; This is better. We're hiding the IO behind the seq abstraction (using the
;; recipe from ???). We're also keeping things from getting out of hand by 
;; wrapping that seq in a seque.
(defn agent-b [input-file]
  (let [reader (agent (seque (lazy-read-csv input-file)))
        processor (agent [])

        read-row (fn read-row [rows]
                   (when-let [[item & items] (seq rows)]
                     (send processor conj item)
                     (send *agent* read-row)
                     items))]
    (send reader read-row)))

;; Similarly for output, we want to make sure that data doesn't get backed up
;; waiting to be written to disk and we want to make sure that the IO isn't
;; happening inside a `dosync`. Even better, it should be wrapped in a `io!`.
;; See the next recipe.

