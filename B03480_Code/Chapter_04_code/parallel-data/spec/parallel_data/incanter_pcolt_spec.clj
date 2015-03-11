
(ns parallel-data.incanter-pcolt-spec
  (:require [parallel-data.utils :as utils]
            [parallel-data.pmap :as pm]
            [parallel-data.incanter-pcolt :as im])
  (:use [speclj.core]
        [incanter.core]
        [parallel-data.spec-utils]
        [clojure.pprint :only (pprint)]))

(def pop-size 500000)
(def xs (vec (repeatedly pop-size rand)))
(def ys (vec (repeatedly pop-size rand)))
(def data-set (to-dataset (map #(zipmap [:x :y] [%1 %2]) xs ys)))

(def mc-size 100000)

(print "map  ") (time (pm/dot-prod-map xs ys))
(print "imap ") (time (im/dot-prod-imap data-set))

(defn tee
  ([tag x] (print (str \[ tag "] " x \newline)) x))

(describe
  "incanter monte-carlo-pi"
  (it "should approximately equal pi."
      (should (approx= 0.1 Math/PI (tee 'pi (im/mc-pi mc-size)))))
  (it "should return approximately equal results in parallel or sequentially."
      (should (approx= 0.1
                       (im/mc-pi mc-size)
                       (pm/mc-pi-seq mc-size))))
  #_
  (it "should be faster in parallel (pmap isn't; reducer is)."
      (let [[time-par p] (utils/raw-time im/mc-pi mc-size)
            [time-seq s] (utils/raw-time pm/mc-pi-seq mc-size)
            [time-r r] (utils/raw-time pm/mc-pi-r mc-size)]
        (println "mc pi times:" time-par time-seq time-r)
        (println "mc pi:" p s r)
        (should (< time-par time-seq)))))

(describe
  "incanter/parallel-colt"
  #_
  (it "should be faster than map."
      (let [[t-map _] (utils/raw-time pm/dot-prod-map xs ys)
            [t-imap _] (utils/raw-time im/dot-prod-imap data-set)]
        (should (< t-imap t-map))))
  (it "should not return nil."
      (let [r-map (pm/dot-prod-map xs ys)
            r-imap (im/dot-prod-imap data-set)]
        (should-not (or (nil? r-map) (nil? r-imap)))))
  (it "should return the same results."
      (let [r-map (pm/dot-prod-map xs ys)
            r-imap (im/dot-prod-imap data-set)]
        (should (approx= 0.0000001 r-imap r-map)))))

(def max-iterations 1000)
(def max-x 1000)
(def max-y 1000)
(def mandelbrot-range {:x [-2.5, 1.0], :y [-1.0, 1.0]})

(defn force-dataset-ctor
  ([f & args]
   (let [ds (apply f args)]
     (nrow ds)
     ds)))

(let [[t-map r-map] (utils/raw-time
                      pm/mandelbrot map
                      max-iterations max-x max-y mandelbrot-range)
      [t-imap r-imap] (utils/raw-time
                        force-dataset-ctor
                        im/mandelbrot-imap
                        max-iterations max-x max-y mandelbrot-range)]
  (print "mandelbrot set: ")
  (pprint {:map t-map, :imap t-imap})
  (describe
    "imap Mandelbrot set"
    #_
    (it "should be faster than map."
        (should (< t-imap t-map)))
    (it "should not return nil."
        (should-not (or (nil? r-map) (nil? r-imap))))
    (it "should return the same results."
        (should= (take 100 r-map) (take 100 ($ :escaped-at r-imap))))))

(run-specs)

