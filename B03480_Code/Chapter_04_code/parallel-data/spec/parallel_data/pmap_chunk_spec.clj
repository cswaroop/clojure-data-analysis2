
(ns parallel-data.pmap-chunk-spec
  (:require [parallel-data.utils :as utils]
            [parallel-data.pmap :as pm]
            [parallel-data.pmap-chunk :as pmc])
  (:use [speclj.core]
        [parallel-data.spec-utils]
        [clojure.pprint :only (pprint)])
  (:import [java.lang Math]))

(def chunk-size 4096)
(def pop-size 1000000)
(def xs (vec (repeatedly pop-size rand)))
(def ys (vec (repeatedly pop-size rand)))

;; Warm things up.
(print "map   ") (time (pm/dot-prod-map xs ys))
(print "pmap  ") (time (pm/dot-prod-pmap xs ys))
(print "pmapc ") (time (pmc/dot-prod-pmap-part chunk-size xs ys))
(print "rmap  ") (time (pm/dot-prod-rmap xs ys))

(defn tee ([tag x] (println \[ tag \] x) x))

(describe
  "chunked pmap monte-carlo-pi"
  (it "should approximately equal pi."
      (should (approx= 0.1 Math/PI (tee :pi (pmc/mc-pi-part chunk-size pop-size)))))
  (it "should return approximately equal results in parallel or sequentially."
      (should (approx= 0.1
                       (pmc/mc-pi-part chunk-size pop-size)
                       (pm/mc-pi-seq pop-size))))
  #_
  (it "should be faster in parallel (pmap isn't; reducer is)."
      (let [[time-ch c] (utils/raw-time pmc/mc-pi-part chunk-size pop-size)
            [time-par p] (utils/raw-time pm/mc-pi pop-size)
            [time-seq s] (utils/raw-time pm/mc-pi-seq pop-size)
            [time-r r] (utils/raw-time pm/mc-pi-r pop-size)]
        (println "mc pi times:" time-ch time-par time-seq time-r)
        (println "mc pi:" c p s r)
        (should (< time-ch time-seq)))))

(describe
  "chunked pmap"
  #_
  (it "should be faster than map (not actually true without playing with the chunk sizes)."
      (let [[t-map _] (utils/raw-time pm/dot-prod-map xs ys)
            [t-cmap _] (utils/raw-time pmc/dot-prod-pmap-part chunk-size xs ys)
            [t-pmap _] (utils/raw-time pm/dot-prod-pmap xs ys)
            [t-rmap _] (utils/raw-time pm/dot-prod-rmap xs ys)]
        (pprint {:t-rmap t-rmap, :t-cmap t-cmap, :t-pmap t-pmap, :t-map t-map})
        (should (< t-cmap t-map))))
  (it "should not return nil."
      (let [r-map (pm/dot-prod-map xs ys)
            r-pmap (pmc/dot-prod-pmap-part chunk-size xs ys)]
        (should-not (and (nil? r-map) (nil? r-pmap)))))
  (it "should return the same results."
      (let [r-map (pm/dot-prod-map xs ys)
            r-pmap (pmc/dot-prod-pmap-part chunk-size xs ys)]
        (should (approx= 0.0000001 r-pmap r-map)))))

(def max-iterations 1000)
(def max-x 1000)
(def max-y 1000)
(def mandelbrot-range {:x [-2.5, 1.0], :y [-1.0, 1.0]})

(let [[t-map r-map] (utils/raw-time
                      pm/mandelbrot map
                      max-iterations max-x max-y mandelbrot-range)
      [t-pmap r-pmap] (utils/raw-time
                        pmc/mandelbrot-part
                        max-iterations max-x max-y mandelbrot-range chunk-size)]
  (print "chunked mandelbrot set: ")
  (pprint {:map t-map, :pmap t-pmap})
  (describe
    "partitioned pmap Mandelbrot set"
    #_
    (it "should be faster than map."
        (should (< t-pmap t-map)))
    (it "should not return nil."
        (should-not (or (nil? r-map) (nil? r-pmap))))
    (it "should return the same results."
        (should= r-map r-pmap))))


(run-specs)

