
(ns concurrent-data.watcher-spec
  (:use [speclj.core]
        [concurrent-data.watcher]
        [concurrent-data.validator :only (data-file)]))

(describe
  "watch-processing"
  (it "should complete processing."
      (should= 591 (count (:results (watch-processing data-file)))))
  (it "should return the number processed from the watcher."
      (should= 591 (:count-watcher (watch-processing data-file)))))

(run-specs)

