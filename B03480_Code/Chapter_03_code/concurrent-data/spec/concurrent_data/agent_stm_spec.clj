
(ns concurrent-data.agent-stm-spec
  (:use [concurrent-data.agent-stm]
        [concurrent-data.agents :only (data-file)]
        [concurrent-data.spec-utils]
        [speclj.core]))

(describe
  "concurrent-data.agent-stm/main"
  (it "should return approximately the correct value."
      (should (approx= 0.001 (/ 1 1.6981832) (main data-file)))))

(run-specs)

