
(ns cleaning-data.normalize-dates-spec
  (:use [speclj.core]
        [clj-time.core :exclude (extend)]
        [cleaning-data.normalize-dates]))

(describe
  "normalize-datetime"
  (it "should take either slashes or dashes between date parts."
      (should= (normalize-datetime "2012-09-12")
               (normalize-datetime "2012/09/12")))
  (it "should normalize dates without times at midnight."
      (let [dt (normalize-datetime "2012-09-12")]
        (should (and (= 0 (hour dt))
                     (= 0 (minute dt))
                     (= 0 (sec dt))))))
  (it "should include the time if given"
      (let [dt (normalize-datetime "2012-09-28T10:11")]
        (should (and (= 10 (hour dt))
                     (= 11 (minute dt))
                     (= 0 (sec dt))))))
  (it "should allow military time"
      (should= 13 (hour (normalize-datetime "2012-09-28T13:14"))))
  (it "should allow a variety of formats"
      (let [expected-date (date-time 2012 9 28)
            expected-time (date-time 2012 9 28 13 45 17)]
        (should= expected-date (normalize-datetime "2012-09-28"))
        (should= expected-date (normalize-datetime "28 Sep 2012"))
        (should= expected-time (normalize-datetime "2012-09-28T13:45:17"))
        (should= expected-time (normalize-datetime "2012-09-28 13:45:17"))
        (should= expected-time
                 (normalize-datetime "Fri, 28 Sep 2012 13:45:17 +0000")))))

(run-specs)


