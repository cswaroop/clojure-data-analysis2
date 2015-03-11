
(ns parallel-data.hints-spec
  (require [parallel-data.hints :as hints]
           [parallel-data.utils :as utils])
  (use [speclj.core]))

(def data (repeatedly 10000 rand))

(hints/without-hints data)
(hints/with-hints data)

(let [no-hints (utils/raw-time hints/without-hints data)
      hints (utils/raw-time hints/with-hints data)]
  (prn hints no-hints)
  (describe
    "type hint timings"
    (it "should be faster with hints."
        (should (< (first hints) (first no-hints))))
    (it "should give the same results."
        (should= (second hints) (second no-hints)))))

(run-specs)

