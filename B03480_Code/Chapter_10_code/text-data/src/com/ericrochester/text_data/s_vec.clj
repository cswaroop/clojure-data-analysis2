
(ns com.ericrochester.text-data.s-vec
  (:require [com.ericrochester.text-data.tfidf :as tfidf])
  (:import [cern.colt.matrix DoubleFactory2D]))

#_
(require '[com.ericrochester.text-data.tokenizing :as t]
         '[com.ericrochester.text-data.tfidf :as tfidf]
         '[com.ericrochester.text-data.s-vec :as s-vec])

#_
(require '[clojure.set :as set]
         '[opennlp.nlp :as nlp])
#_
(import [cern.colt.matrix DoubleFactory2D])

(defn build-index [corpus]
  (into {}
        (zipmap (tfidf/get-corpus-terms corpus)
                (range))))

(defn ->matrix [index pairs]
  (let [matrix (.make DoubleFactory2D/sparse
                 1 (count index) 0.0)
        inc-cell (fn [m p]
                   (let [[k v] p,
                         i (index k)]
                     (.set m 0 i v)
                     m))]
    (reduce inc-cell matrix pairs)))

(defn ->count-matrix [index doc]
  (let [matrix (.make DoubleFactory2D/sparse 1 (count index) 0.0)
        inc-cell (fn [m w]
                   (let [i (index w)]
                     (.set m 0 i (inc (.get m 0 i)))
                     m))]
    (reduce inc-cell matrix doc)))

(comment

(def corpus
  (->> "sotu"
       (java.io.File.)
       (.list)
       (map #(str "sotu/" %))
       (map slurp)
       (map tokenize)
       (map normalize)
       (map frequencies)))
(def index (build-index corpus))
(def vecs (map #(->matrix index %) corpus))
(def vec0 (s-vec/->matrix index (first corpus)))

  )
