
(ns cleaning-data.normalize-numbers
  (:use [clojure.string :as string]))

#_
(require '[clojure.string :as string])

(defn normalize-number
  "This takes a number and returns a floating-point number.

  This ignores commas or periods used to separate thousands (or higher) places.

  NB: This requires that a decimal or comma is used to specify the fractional
  part of the number. "
  [n]
  (let [v (string/split n #"[,.]")
        [pre post] (split-at (dec (count v)) v)]
    (Double/parseDouble (apply str (concat pre [\.] post)))))

