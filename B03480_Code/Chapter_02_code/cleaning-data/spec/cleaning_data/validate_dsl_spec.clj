
(ns cleaning-data.validate-dsl-spec
  (:use [speclj.core]
        [valip.core]
        [valip.predicates]))

#_
(use 'valip.core
     'valip.predicates)

(defn number-present? [x]
  (and (present? (str x))
       (or (instance? Integer x)
           (instance? Long x))))

(defn valid-badge [n]
  (not (nil? (re-find #"[A-Z]{3}\d+" n))))

(def user
  {:given-name "Fox"
   :surname "Mulder"
   :age 51
   :badge "JTT047101111"})

(defn validate-user [user]
  (validate user
    [:given-name present? "Given name required."]
    [:surname present? "Surname required."]
    [:age number-present? "Age required."]
    [:age (over 0) "Age should be positive."]
    [:age (under 150) "Age should be under 150."]
    [:badge present?
     "The badge number is required."]
    [:badge valid-badge
     "The badge number is invalid."]))

(describe
  "validation"
  (it "should pass valid data."
      (should (nil? (validate-user user))))
  (it "should catch missing data."
      (should=
        {:state ["Should have a state."]
         :height ["Should have a height."]}
        (validate-user user)))
  (it "should catch negative ages."
      (should=
        {:age ["Age should be positive."]}
        (validate-user (assoc user :age -42))))
  (it "should catch too-old ages."
      (should=
        {:age ["Age should be under 150."]}
        (validate-user (assoc user :age 200)))))

(run-specs)

