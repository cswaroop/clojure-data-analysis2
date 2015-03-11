
(ns cleaning-data.syn-map-spec
  (:use [speclj.core]
        [cleaning-data.syn-map]))

(describe
  "State synonym mapping."
  (it "should force all states to upper-case."
      (should= "VA" (normalize-state "Va"))
      (should= "VA" (normalize-state "va"))
      (should= "VA" (normalize-state "VA")))
  (it "should use two-letter abbreviations."
      (should= "FL" (normalize-state "Fla")))
  (it "should abbreviate states."
      (should= "NC" (normalize-state "North Carolina"))
      (should= "SC" (normalize-state "south carolina"))))

(run-specs)

