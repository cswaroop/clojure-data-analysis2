
(ns parallel-data.reducers
  (:require [clojure.core.reducers :as r])
  (:use [parallel-data.pmap]))

#_
(require '[clojure.string :as str]
         '[clojure.core.reducers :as r])

(defn ->int [x]
  (try
    (Long/parseLong x)
    (catch Exception e
      x)))

(def data
  (str/split "This is a small list. It contains 42 items. Or less."
             #"\s+"))

(map ->int
     (map str/lower-case
          data))

(r/map ->int
       (r/map str/lower-case
              data))

(into []
      (r/map ->int
             (r/map str/lower-case
                    data)))

