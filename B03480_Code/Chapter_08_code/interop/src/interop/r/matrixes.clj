
(ns interop.r.matrixes
  (:use interop.r.core)
  (:require [clojure.string :as str]))

(comment
(use 'interop.r.core)
(require '[clojure.string :as str])
  )

(defprotocol ToR
  "A protocol for marshalling data into R."
  (->r [x] "Convert an item to R."))

(extend-protocol ToR
  clojure.lang.ISeq
  (->r [coll] (str "c(" (str/join \, (map ->r coll)) ")"))

  clojure.lang.PersistentVector
  (->r [coll] (->r (seq coll)))

  java.lang.Integer
  (->r [i] (str i))

  java.lang.Long
  (->r [l] (str l))

  java.lang.Float
  (->r [f] (str f))

  java.lang.Double
  (->r [d] (str d)))

(defn r-mean
  ([coll] (r-mean coll *r-cxn*))
  ([coll r-cxn]
   (.. r-cxn
     (eval (str "mean(" (->r coll) ")"))
     asDouble)))

(comment
(r-mean [1.0 2.0 3.0])
(r-mean (map (fn [_] (rand)) (range 5)))
  )

(extend-protocol ToR
  java.lang.String
  (->r [s] (str \' s \')))


