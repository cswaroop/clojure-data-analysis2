
(ns parallel-data.optimal-chunk-spec
  (:require [parallel-data.utils :as utils])
  (:use [speclj.core]
        [clojure.pprint :only (pprint)]
        [parallel-data.optimal-chunk]
        [parallel-data.pmap :only (mandelbrot)]
        [parallel-data.pmap-chunk :only (mandelbrot-part)]))

(def max-iter 250)
(def max-x 500)
(def max-y 500)
(def m-range {:x [-2.5, 1.0], :y [-1.0, 1.0]})
(def trials 3)

(def cost-fn (partial get-mandelbrot-cost max-iter max-x max-y m-range trials))

(let [[map-time _] (utils/raw-time mandelbrot map max-iter max-x max-y m-range)
      sa-output (annealing (rand-int 20) 10 map-time
                           get-neighbor cost-fn should-move get-temp)]
  (println "annealing")
  (println map-time)
  (pprint sa-output)
  (println)
  (describe
    "parallel-data.optimal-chunk"
    (it "should try more than one solution, but at most 10."
        (should (and (> (count sa-output) 1)
                     (<= (count sa-output) 10))))
    (it "should return the lowest cost last."
        (should= (apply min (map :cost sa-output))
                 (:cost (last sa-output))))
    (it "should improve the cost over the course of the run."
        (should (< (:cost (last sa-output))
                   (:cost (first sa-output)))))))

(run-specs)

