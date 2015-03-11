
;; The pmap version is slower unless the data is very large and you use the
;; version that includes partitioning (and set the chunk size large enough).
;;
;; This will be good to come back and revisit for the recipe on chunking data.

(ns parallel-data.pmap
  (:require [clojure.core.reducers :as r]
            [clojure.java.io :as io])
  (:import [java.lang Math]))

(comment
(import [java.lang Math])
(require '[clojure.core.reducers :as r])
(use 'criterium.core)
  )

;;; Dot-product example
(defn dot-prod-map
  ([xs ys]
   (reduce + 0 (map * xs ys))))

(defn dot-prod-pmap
  ([xs ys]
   (reduce + 0
           (pmap * xs ys))))

(defn dot-prod-pmap-part
  ([xs ys] (dot-prod-pmap-part 512 xs ys))
  ([n xs ys]
   (reduce + 0
           (pmap dot-prod-map
                 (partition-all n xs)
                 (partition-all n ys)))))

(defn dot-prod-rmap
  ([xs ys]
   (r/reduce + 0
           (r/map (fn [[x y]] (* x y)) (map vector xs ys)))))

;;; Monte Carlo PI estimation example
(defn rand-point [] [(rand) (rand)])

(defn center-dist [[x y]]
  (Math/sqrt (+ (* x x) (* y y))))

(defn mc-pi-base [get-in-circle]
  (fn [n]
    (double (* 4.0 (/ (get-in-circle (repeatedly n rand-point))
                      n)))))

(def mc-pi
  (mc-pi-base (fn [points]
                (->>
                  points
                  (pmap center-dist)
                  (filter #(<= % 1.0))
                  count))))

(def mc-pi-seq
  (mc-pi-base (fn [points]
                (->>
                  points
                  (map center-dist)
                  (filter #(<= % 1.0))
                  count))))

(defn count-items [c _] (inc c))

(defn count-in-circle-r [n]
  (->> (repeatedly n rand-point)
       vec
       (r/map center-dist)
       (r/filter #(<= % 1.0))
       (r/fold + count-items)))

(defn mc-pi-r [n]
  (* 4.0 (/ (count-in-circle-r n) n)))

(comment
(quick-bench (mc-pi 1000000))
(quick-bench (mc-pi-r 1000000))
  )

;;; Mandelbrot set example
(defn get-escape-point
  [scaled-x scaled-y max-iterations]
  (loop [x 0, y 0, iteration 0]
    (let [x2 (* x x)
          y2 (* y y)]
      (if (and (< (+ x2 y2) 4)
               (< iteration max-iterations))
        (recur (+ (- x2 y2) scaled-x)
               (+ (* 2 x y) scaled-y)
               (inc iteration))
        iteration))))

(defn scale-to [pixel maximum [lower upper]]
  (+ (* (/ pixel maximum) (Math/abs (- upper lower))) lower))

(defn scale-point
  [pixel-x pixel-y max-x max-y set-range]
  [(scale-to pixel-x max-x (:x set-range))
   (scale-to pixel-y max-y (:y set-range))])

(defn output-points [max-x max-y]
  (let [range-y (range max-y)]
    (mapcat (fn [x] (map #(vector x %) range-y))
            (range max-x))))

(defn mandelbrot-pixel
  ([max-x max-y max-iterations set-range]
   (partial mandelbrot-pixel
            max-x max-y max-iterations set-range))
  ([max-x max-y max-iterations set-range [pixel-x pixel-y]]
   (let [[x y] (scale-point pixel-x pixel-y max-x max-y
                            set-range)]
     (get-escape-point x y max-iterations))))

(defn mandelbrot
  [mapper max-iterations max-x max-y set-range]
  (doall
    (mapper (mandelbrot-pixel
              max-x max-y max-iterations set-range)
            (output-points max-x max-y))))

;; Write the Mandelbrot set to an image.
(defn save-mandelbrot-set [max-iter max-x max-y m-set png-file]
  (let [img (java.awt.image.BufferedImage.
              max-x max-y
              java.awt.image.BufferedImage/TYPE_INT_RGB)
        g (.createGraphics img)
        by-row (partition max-x m-set)
        js (range max-x)
        colors (vec
                 (reverse
                   (take max-iter
                         (iterate (memfn brighter)
                                  java.awt.Color/black))))]
    (loop [i 0
           rows by-row]
      (when-let [[r & rs] (seq rows)]
        (doseq [[j p] (map vector js r)]
          (doto g
            (.setColor (nth colors p java.awt.Color/black))
            (.draw (java.awt.geom.Line2D$Float.
                     j i j i))))
        (recur (inc i) rs)))
    (let [file (java.io.File. png-file)]
      (javax.imageio.ImageIO/write img "png" file)
      img)))

(comment
(def mandelbrot-range
     {:x [-2.5, 1.0], :y [-1.0, 1.0]})

(quick-bench (mandelbrot map 500 1000 1000 mandelbrot-range))
(quick-bench (mandelbrot pmap 500 1000 1000 mandelbrot-range))
(quick-bench (mandelbrot map 1000 1000 1000 mandelbrot-range))
(quick-bench (mandelbrot pmap 1000 1000 1000 mandelbrot-range))

(def m (mandelbrot pmap 1000 1000 1000 mandelbrot-range))
(save-mandelbrot-set 1000 1000 1000 m "mandelbrot.png")
  )
