
(ns concurrent-data.validator
  (:use concurrent-data.utils))

#_
(require '[clojure.java.io :as io]
         '[clojure.data.csv :as csv])

(def data-file "data/all_160_in_51.P35.csv")

(def int-rows
  [:GEOID :SUMLEV :STATE :POP100 :HU100 :POP100.2000
   :HU100.2000 :P035001 :P035001.2000])

(defn int? [x]
  (or (instance? Integer x) (instance? Long x)))

(defn try-read-string [x]
  (try
    (read-string x)
    (catch Exception ex
      x)))

(defn coerce-row [_ row sink]
  (let [cast-row
        (apply assoc row
               (mapcat
                 (fn [k]
                   [k (try-read-string (k row))])
                 int-rows))]
    (send sink conj cast-row)
    cast-row))

(defn read-row [rows caster sink]
  (when-let [[item & items] (seq rows)]
    (send caster coerce-row item sink)
    (send *agent* read-row caster sink)
    items))

(defn int-val? [x] (or (int? x) (empty? x)))
(defn validate [row]
  (or (nil? row)
      (reduce #(and %1 (int-val? (%2 row)))
              true int-rows)))

(defn agent-ints [input-file]
  (let [reader (agent (seque
                        (with-header
                          (lazy-read-csv
                            input-file))))
        caster (agent nil)
        sink (agent [])]
    (set-validator! caster validate)
    (send reader read-row caster sink)
    {:reader reader
     :caster caster
     :sink sink}))

