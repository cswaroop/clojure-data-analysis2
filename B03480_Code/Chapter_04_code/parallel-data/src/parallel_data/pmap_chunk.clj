
;; The pmap version is slower unless the data is very large and you use the
;; version that includes partitioning (and set the chunk size large enough).
;;
;; This will be good to come back and revisit for the recipe on chunking data.

(ns parallel-data.pmap-chunk
  (:use [parallel-data.pmap :only (dot-prod-map
                                    center-dist
                                    rand-point
                                    output-points
                                    mandelbrot-pixel)]))

(defn dot-prod-pmap-part
  ([xs ys] (dot-prod-pmap-part 512 xs ys))
  ([n xs ys]
   (reduce + 0
           (pmap dot-prod-map
                 (partition-all n xs)
                 (partition-all n ys)))))

(defn sum
  ([xs] (reduce + 0 xs)))

#_
(do
(import [java.lang Math])
(use 'criterium.core)
  )

(defn count-in-circle [n]
  (->> (repeatedly n rand-point)
       (map center-dist)
       (filter #(<= % 1.0))
       count))

(defn mc-pi [n]
  (* 4.0 (/ (count-in-circle n) n)))

(defn in-circle-flag [p]
  (if (<= (center-dist p) 1.0) 1 0))

(defn mc-pi-pmap [n]
  (let [in-circle (->> (repeatedly n rand-point)
                       (pmap in-circle-flag)
                       (reduce + 0))]
    (* 4.0 (/ in-circle n))))

(defn mc-pi-part
  ([n] (mc-pi-part 512 n))
  ([chunk-size n]
   (let [step (int (Math/floor (float (/ n chunk-size))))
         remainder (mod n chunk-size)
         parts (lazy-seq
                 (cons remainder (repeat step chunk-size)))
         in-circle (reduce + 0
                           (pmap count-in-circle parts))]
     (* 4.0 (/ in-circle n)))))

(defn mandelbrot-part
  ([max-iterations max-x max-y set-range chunk-size]
   (doall
     (flatten
       (pmap #(map (mandelbrot-pixel max-x max-y max-iterations set-range) %)
             (partition-all chunk-size
                            (output-points max-x max-y)))))))

