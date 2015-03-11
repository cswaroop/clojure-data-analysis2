
(ns parallel-data.reducers-spec
  (:require [parallel-data.utils :as utils]
            [parallel-data.pmap :as pm])
  (:use [speclj.core]
        [parallel-data.reducers]
        [parallel-data.spec-utils]))

(def pop-size 500000)
(def xs (vec (repeatedly pop-size rand)))
(def ys (vec (repeatedly pop-size rand)))

(def mc-size 100000)

(describe
  "reducer monte-carlo-pi"
  (it "should approximately equal pi."
      (should (approx= 0.1 Math/PI (pm/mc-pi-r mc-size))))
  (it "should return approximately equal results in parallel or sequentially."
      (should (approx= 0.1
                       (pm/mc-pi-r mc-size)
                       (pm/mc-pi-seq mc-size))))
  (it "should be faster in parallel (reducer is)."
      (let [[time-par p] (utils/raw-time pm/mc-pi mc-size)
            [time-seq s] (utils/raw-time pm/mc-pi-seq mc-size)
            [time-r r] (utils/raw-time pm/mc-pi-r mc-size)]
        (println "mc pi times:" {:time-par time-par
                                 :time-seq time-seq
                                 :time-r time-r})
        (println "mc pi:" p s r)
        (should (< time-r time-seq)))))

(run-specs)

