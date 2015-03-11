
(ns parallel-data.pmap-spec
  (:require [parallel-data.utils :as utils]
            [parallel-data.pmap :as pm])
  (:use [speclj.core]
        [parallel-data.pmap]
        [parallel-data.spec-utils]
        [clojure.pprint :only (pprint)])
  (:import [java.lang Math]))

(def pop-size 500000)
(def xs (vec (repeatedly pop-size rand)))
(def ys (vec (repeatedly pop-size rand)))

(def mc-size 100000)

;; Warm things up.
(print "map  ") (time (pm/dot-prod-map xs ys))
(print "pmap ") (time (pm/dot-prod-pmap xs ys))
(print "rmap ") (time (pm/dot-prod-rmap xs ys))

(describe
  "pmap monte-carlo-pi"
  (it "should approximately equal pi."
      (should (approx= 0.1 Math/PI (pm/mc-pi mc-size))))
  (it "should return approximately equal results in parallel or sequentially."
      (should (approx= 0.1
                       (pm/mc-pi mc-size)
                       (pm/mc-pi-seq mc-size))))
  #_
  (it "should be faster in parallel (pmap isn't; reducer is)."
      (let [[time-par p] (utils/raw-time pm/mc-pi mc-size)
            [time-seq s] (utils/raw-time pm/mc-pi-seq mc-size)
            [time-r r] (utils/raw-time pm/mc-pi-r mc-size)]
        (println "mc pi times:" time-par time-seq time-r)
        (println "mc pi:" p s r)
        (should (< time-par time-seq)))))

(describe
  "pmap dot-product"
  #_
  (it "should be faster than map (not actually true without playing with the chunk sizes)."
      (let [[t-map _] (utils/raw-time pm/dot-prod-map xs ys)
            [t-pmap _] (utils/raw-time pm/dot-prod-pmap xs ys)
            [t-rmap _] (utils/raw-time pm/dot-prod-rmap xs ys)]
        (println (str t-rmap " / " t-pmap " / " t-map))
        (should (< t-pmap t-map))))
  (it "should not return nil."
      (let [r-map (pm/dot-prod-map xs ys)
            r-pmap (pm/dot-prod-pmap xs ys)]
        (should-not (or (nil? r-map) (nil? r-pmap)))))
  (it "should return the same results."
      (let [r-map (pm/dot-prod-map xs ys)
            r-pmap (pm/dot-prod-pmap xs ys)]
        (should (approx= 0.0000001 r-pmap r-map)))))

(def max-iterations 1000)
(def max-x 1000)
(def max-y 1000)
(def mandelbrot-range {:x [-2.5, 1.0], :y [-1.0, 1.0]})

(let [[t-map r-map] (utils/raw-time
                      pm/mandelbrot map
                      max-iterations max-x max-y mandelbrot-range)
      [t-pmap r-pmap] (utils/raw-time
                        pm/mandelbrot pmap
                        max-iterations max-x max-y mandelbrot-range)]
  (print "mandelbrot set: ")
  (pprint {:map t-map, :pmap t-pmap})
  (describe
    "pmap Mandelbrot set"
    (it "should be faster than map."
        (should (< t-pmap t-map)))
    (it "should not return nil."
        (should-not (or (nil? r-map) (nil? r-pmap))))
    (it "should return the same results."
        (should= r-map r-pmap))))

(run-specs)

