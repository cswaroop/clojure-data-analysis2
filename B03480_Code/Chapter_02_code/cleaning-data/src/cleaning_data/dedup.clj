
(ns cleaning-data.dedup
  (:use [clj-diff.core]))

#_
(use 'clj-diff.core)

(def fuzzy-max-diff 2)
(def fuzzy-percent-diff 0.1)
(def fuzzy-dist edit-distance)

(defn fuzzy=
  "This returns a fuzzy match."
  [a b]
  (let [dist (fuzzy-dist a b)]
    (or (<= dist fuzzy-max-diff)
        (<= (/ dist (min (count a) (count b)))
            fuzzy-percent-diff))))

(defn records-match
  "This returns true if two records match on one or more key functions."
  [key-fn a b]
  (let [kfns (if (sequential? key-fn) key-fn [key-fn])
        rfn (fn [prev next-fn]
              (and prev (fuzzy= (next-fn a) (next-fn b))))]
    (reduce rfn true kfns)))

(defn pair-all
  "This pairs each element in a sequence with every other element in it.

  Each element is only matched once with the other elements."
  [coll]
  (when-let [[x & xs] (seq coll)]
    (lazy-cat (map #(vector x %) xs) (pair-all xs))))

(defn merge-item
  "This takes an item and a sequence of items. It collapses the single item
  with all of the fuzzy duplicates, as determined by records-match, found in
  the sequence. It returns the merged item and the items from the sequence that
  were not found to be duplicates."
  [key-fn item coll]
  (let [merge-fn (fn [old value]
                   (conj (if (set? old) old #{old}) value))
        reduce-fn (fn [[y ys] z]
                    (if (records-match key-fn item z)
                      [(merge-with merge-fn (if (nil? y) item y) z) ys]
                      [y (conj ys z)]))
        result (reduce reduce-fn [nil []] coll)]
    (if (nil? (first result))
      [item (second result)]
      result)))

(defn merge-duplicates
  "This takes the key function and collapses those that return true for
  records-match."
  [key-fn items]
  (if-let [[x & xs] (seq items)]
    (let [[y ys] (merge-item key-fn x xs)
          [non-dups dups] (merge-duplicates key-fn ys)]
      (if (not (= x y))
        [non-dups (conj dups y)]
        [(conj non-dups y) dups]))
    [[] []]))

