
(ns concurrent-data.watcher
  (:use concurrent-data.utils
        [concurrent-data.agents :only (data-files ->int)])
  (:import [java.lang Thread]))

#_
(do
(require '[clojure.java.io :as io]
         '[clojure.data.csv :as csv]
         '[clojure.string :as str])
(import '[java.lang Thread])
  )

(def row-ints [0 4])

(defn try->int [v]
  (try
    (->int (str/trim (str/replace v \| \space)))
    (catch Exception ex
      v)))

(defn coerce-row-ints [_ row indices sink]
  (let [cast-row
        (->> indices
             (mapcat #(vector % (try->int (nth row %))))
             (apply assoc row))]
    (send sink conj cast-row)
    cast-row))

(defn read-row [rows caster sink done]
  (if-let [[item & items] (seq rows)]
    (do
      (send caster coerce-row-ints item row-ints sink)
      (send *agent* read-row caster sink done)
      items)
    (do
      (dosync (commute done (constantly true)))
      '())))

(defn watch-caster [counter watch-key watch-agent old-state new-state]
  (when-not (nil? new-state)
    (dosync (commute counter inc))))

(defn wait-for-it [sleep-for ref-var]
  (loop []
    (when-not @ref-var
      (Thread/sleep sleep-for)
      (recur))))

(defn watch-processing [input-files]
  (let [reader (agent (seque
                        (mapcat
                          lazy-read-csv
                          input-files)))
        caster (agent nil)
        sink (agent [])
        counter (ref 0)
        done (ref false)]
    (add-watch caster :counter
               (partial watch-caster counter))
    (send reader read-row caster sink done)
    (wait-for-it 250 done)
    {:results @sink
     :count-watcher @counter}))

#_
(:count-watcher (watch-processing data-files))

