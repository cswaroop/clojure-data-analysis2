
(ns concurrent-data.commute-spec
  (:use [concurrent-data.commute]
        [concurrent-data.agents :only (data-file)]
        [concurrent-data.spec-utils]
        [speclj.core]))

(describe
  "concurrent-data.commute/main"
  (it "should return approximately the correct value."
      (should (approx= 0.001 (/ 1 1.6981832) (main data-file)))))

(run-specs)

