
(ns concurrent-data.agents-spec
  (:use [concurrent-data.agents]
        [concurrent-data.spec-utils]
        [speclj.core]))

(describe
  "concurrent-data.agents/main"
  (it "should return approximately the correct value."
      (should (approx= 0.001 (/ 1 1.6981832) (main data-file)))))

(run-specs)

