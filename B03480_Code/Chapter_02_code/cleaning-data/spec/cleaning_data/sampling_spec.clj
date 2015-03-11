
(ns cleaning-data.sampling-spec
  (:use [speclj.core]
        [cleaning-data.sampling])
  (:import [java.lang Math]))

(defn tee
  ([v]
   (println ">>>" v)
   v))

(defn should-be-in-range
  [rng delta value]
  (should (<= (Math/abs (- rng value)) delta)))

(describe
  "sample-percent"
  (it "should sample 1 out of 10 for 10%."
      (should-be-in-range 1 1 (count (sample-percent 0.1 (range 10)))))
  (it "should sample 5 out of 50 for 10%."
      (should-be-in-range 5 5 (count (sample-percent 0.1 (range 50)))))
  (it "should sample 25 out of 50 for 50%."
      (should-be-in-range 25 10 (count (sample-percent 0.5 (range 50)))))
  (it "should sample 10 out of 1000 for 1%."
      (should-be-in-range 10 15 (count (sample-percent 0.01 (range 1000))))))

(describe
  "sample-amount"
  (it "should sample 8 out of 10 for 8."
      (should= 8 (count (sample-amount 8 (range 10)))))
  (it "should sample 8 out of 50 for 8."
      (should= 8 (count (sample-amount 8 (range 50)))))
  (it "should sample 25 out of 50 for 25."
      (should= 25 (count (sample-amount 25 (range 50)))))
  (it "should sample 25 out of 1000 for 25."
      (should= 25 (count (sample-amount 25 (range 1000)))))
  (it "should not pick the first 25 to sample 25."
      (let [population (range 1000)
            first-25 (take 25 population)]
        (should-not= first-25 (sample-amount 25 population)))))

(run-specs)

