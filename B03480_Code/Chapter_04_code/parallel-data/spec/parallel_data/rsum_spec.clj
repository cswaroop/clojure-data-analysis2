
(ns parallel-data.rsum-spec
  (:require [clojure.core.reducers :as r])
  (:use [speclj.core]
        [parallel-data.rsum]
        [parallel-data.spec-utils]))

(def ^:dynamic *tolerance* 0.1)

(let [sum-stats (->>
                  data-file
                  lazy-read-csv
                  with-header
                  (r/map :POP100)
                  (r/map read-string)
                  summary-statistics)]
  (prn sum-stats)
  (describe
    "parallel-data.rsum"
    (it "should compute the mean, standard deviation, and skew."
        (should= [:mean :skew :variance]
                 (sort (keys sum-stats))))
    (it "should approximate the mean for the data."
        (should (approx= *tolerance* 9193.27411167513 (:mean sum-stats))))
    (it "should approximate the variance for the data."
        (should (approx= *tolerance* 865082615.812871 (:variance sum-stats))))
    (it "should approximate the skew for the data."
        (should (approx= *tolerance* 8.61625854970373 (:skew sum-stats))))))

(let [sum-stats (->>
                  data-file
                  lazy-read-csv
                  with-header
                  (r/map :POP100)
                  (r/map read-string)
                  summary-statistics-2)]
  (prn sum-stats)
  (describe
    "parallel-data.rsum (simplified)"
    (it "should compute the mean and standard deviation."
        (should= [:mean :variance]
                 (sort (keys sum-stats))))
    (it "should approximate the mean for the data."
        (should (approx= *tolerance* 9193.27411167513 (:mean sum-stats))))
    (it "should approximate the variance for the data."
        (should (approx= *tolerance* 865082615.812871 (:variance sum-stats))))))

(run-specs)

