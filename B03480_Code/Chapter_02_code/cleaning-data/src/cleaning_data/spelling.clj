
; This is a simple implementation of Peter Norvig's "How to Write a Spelling
; Corrector" (http://norvig.com/spell-correct.html).

(ns cleaning-data.spelling
  (:require [clojure.string :as string])
  (:use [clojure.set :only (union)]))

#_
(do
(require '[clojure.string :as string])
(use '[clojure.set :only (union)]))

(defn tee
  ([tag item]
   (println tag \: item)
   (.flush *out*)
   item))

(defn words [text]
  (re-seq #"[a-z]+" (string/lower-case text)))

(defn train [feats] (frequencies feats))

(def n-words (train (words (slurp "data/big.txt"))))
(def alphabet "abcdefghijklmnopqrstuvwxyz")

; Split a word into two parts at position i.
(defn split-word [word i]
  [(.substring word 0 i) (.substring word i)])

; Delete the first character in the second part.
(defn delete-char [[w1 w2]] (str w1 (.substring w2 1)))

; Transpose the first two characters of the second part.
(defn transpose-split [[w1 w2]]
  (str w1 (second w2) (first w2) (.substring w2 2)))

; Replace the first character of the second part with every letter.
(defn replace-split [[w1 w2]]
  (let [w2-0 (.substring w2 1)]
    (map #(str w1 % w2-0) alphabet)))

; Insert every letter into the word at the split.
(defn insert-split [[w1 w2]]
  (map #(str w1 % w2) alphabet))

(defn edits-1 [word]
  (let [splits (map (partial split-word word)
                    (range (inc (count word))))
        long-splits (filter #(> (count (second %)) 1) splits)
        deletes (map delete-char long-splits)
        transposes (map transpose-split long-splits)
        replaces (mapcat replace-split long-splits)
        inserts (remove nil?
                        (mapcat insert-split splits))]
    (set (concat deletes transposes replaces inserts))))

(defn known-edits-2 [word]
  (set (filter (partial contains? n-words)
               (apply union
                      (map #(edits-1 %)
                           (edits-1 word))))))

(defn known [words]
  (set (filter (partial contains? n-words) words)))

(defn correct [word]
  (let [candidate-thunks [#(known (list word))
                          #(known (edits-1 word))
                          #(known-edits-2 word)
                          #(list word)]]
    (->>
      candidate-thunks
      (map (fn [f] (f)))
      (filter #(> (count %) 0))
      first
      (map (fn [w] [(get n-words w 1) w]))
      (reduce (partial max-key first))
      second)))

(defn correct-candidates
  ([word]
   (let [candidate-thunks [#(known (list word))
                           #(known (edits-1 word))
                           #(known-edits-2 word)
                           #(list word)]]
     (->>
       candidate-thunks
       (map (fn [f] (f)))
       (reduce into #{})
       (map (fn [w] [(get n-words w 1) w]))
       sort
       reverse))))

