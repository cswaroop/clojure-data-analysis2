
(ns cleaning-data.rescaling)

#_
(require '[clojure.pprint :as pp])

;; rescale by a total of everything
(defn rescale-by-total [src dest coll]
  (let [total (reduce + (map src coll))
        update #(assoc % dest (/ (% src) total))]
    (map update coll)))

;; rescale by the total of a group
(defn rescale-by-group [src group dest coll]
  (->> coll
       (sort-by group)
       (group-by group)
       vals
       (mapcat #(rescale-by-total src dest %))))

;; rescale by another column
(defn rescale-by-key [src by dest coll]
  (let [update #(assoc % dest (/ (get % src) (get % by)))]
    (map update coll)))

