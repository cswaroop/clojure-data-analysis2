
(ns com.ericrochester.text-data.tfidf
  (:require [clojure.set :as set]
            [com.ericrochester.text-data.tokenizing :as t]))

#_
(require '[clojure.set :as set])


(defn tf [term-freq max-freq]
  (+ 0.5 (/ (* 0.5 term-freq) max-freq)))

(defn idf [corpus term]
  (Math/log
    (/ (count corpus)
       (inc (count (filter #(contains? % term) corpus))))))

(defn get-corpus-terms [corpus]
  (->> corpus
       (map #(set (keys %)))
       (reduce set/union #{})))

(defn get-idf-cache [corpus]
  (reduce #(assoc %1 %2 (idf corpus %2)) {}
          (get-corpus-terms corpus)))

(defn tf-idf [idf-value freq max-freq]
  (* (tf freq max-freq) idf-value))

(defn tf-idf-pair [idf-cache max-freq pair]
  (let [[term freq] pair]
    [term (tf-idf (idf-cache term) freq max-freq)]))

(defn tf-idf-freqs [idf-cache freqs]
  (let [max-freq (reduce max 0 (vals freqs))]
    (->> freqs
         (map #(tf-idf-pair idf-cache max-freq %))
         (into {}))))

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

(def cache (get-idf-cache corpus))
(def freqs (map #(tf-idf-freqs cache %) corpus))

(doseq [[term idf-freq] (->> freqs
                            first
                            (sort-by second)
                            reverse
                            (take 10))]
  (println [term idf-freq ((first corpus) term)]))

(def top (->> freqs first (sort-by second) reverse (take 10) (map first)))
(def corpus-freqs (reduce #(merge-with + %1 %2) {} corpus))

(doseq [term top]
  (println [term (corpus-freqs term)]))

  )
