
(ns cleaning-data.normalize-numbers-spec
  (:use [speclj.core]
        [cleaning-data.normalize-numbers]))

(describe
  "normalize-number"
  (it "should remove commas separating thousands from number literals."
      (should= 1000.0 (normalize-number "1,000.00"))
      (should= 1000000.0 (normalize-number "1,000,000.00")))
  (it "should remove periods separating thousands from number literals."
      (should= 1000.0 (normalize-number "1.000,00"))
      (should= 1000000.0 (normalize-number "1.000.000,00")))
  (it "should return a floating point number."
      (should (float? (normalize-number "3,14")))))

(run-specs)

