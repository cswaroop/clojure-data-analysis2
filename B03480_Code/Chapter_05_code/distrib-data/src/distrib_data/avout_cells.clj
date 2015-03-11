
;;;; This kind of gets the job done. The items percolating through
;;;; aren't 100% consisten.

(ns distrib-data.avout-cells
  (:use avout.core))

(def ^:dynamic *avout-client* (connect "127.0.0.1"))

(defn cell-watch
  ([cell-name cell-ref snapshot update-fn [_ k] r old state]
   (dosync!!
     *avout-client*
     (let [args (flatten
                  (seq
                    (alter!! snapshot #(assoc % k state))))]
       (alter!! cell-ref
                #(apply update-fn % args))))))

(defn make-cell
  ([name-key value update-fn & {:as deps}]
   (let [r (zk-ref *avout-client* (str "/" name-key) value)
         snapshot (local-ref *avout-client*
                             (str "/" name-key "-snapshot")
                             (apply hash-map
                                    (flatten
                                      (map (fn [[k v]] [k @v])
                                           deps))))
         watch (partial cell-watch name-key r snapshot update-fn)]
     (doseq [[k v] deps]
       (add-watch v [name-key k] watch))
     r)))

(def src-cell (zk-ref *avout-client* "/src-cell" 0))

(def cell-2 (make-cell :double 0
                       (fn [_ & {:keys [src]}]
                         (* 2 src))
                       :src src-cell))

(def cell-3 (make-cell :triple 0
                       (fn [_ & {:keys [src]}]
                         (* 3 src))
                       :src src-cell))

(def cell-4 (make-cell :quad 0
                       (fn [_ & {:keys [src]}]
                         (* 4 src))
                       :src src-cell))

(def cell-5 (make-cell :sum 0
                       (fn [_ & {:keys [src c2 c3 c4]}]
                         (+ src c2 c3 c4))
                       :src src-cell :c2 cell-2
                       :c3 cell-3 :c4 cell-4))

(defn cells
  [] [@src-cell @cell-2 @cell-3 @cell-4 @cell-5])

