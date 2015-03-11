
(ns cleaning-data.with-regex-spec
  (:use [speclj.core]
        [cleaning-data.with-regex]))

(describe
  "US phone numbers."
  (it "should standardize to '(999)999-9999'."
      (should= "(123)456-7890" (clean-us-phone "123-456-7890"))
      (should= "(987)654-3210" (clean-us-phone "987.654.3210"))
      (should= "(434)555-1212" (clean-us-phone "4345551212"))
      (should= "(404)000-5555" (clean-us-phone "404 000-5555"))
      (should= "(123)456-0987" (clean-us-phone "(123) 456-0987")))
  (it "should return nil for invalid numbers."
      (should (nil? (clean-us-phone "1 2 3 4 5 6 7 8 9 0")))))

(describe
  "Email addresses."
  (it "should not have the name."
      (should= "gomez@addams.com" (clean-email "Gomez Addams <gomez@addams.com>")))
  (it "should not change if it's OK."
      (should= "morticia@addams.com" (clean-email "morticia@addams.com")))
  (it "should be nil if it's blatantly not an email address."
      (should (nil? (clean-email "not an email")))))

(describe
  "Company names."
  (it "should abbreviate 'Incorporated.'"
      (should= "Acme, Inc." (company-name "Acme, Incorporated"))
      (should= "Acme, Inc." (company-name "Acme, Inc.")))
  (it "should add a comma before 'Inc.'"
      (should= "Acme, Inc." (company-name "Acme Incorporated"))
      (should= "Acme, Inc." (company-name "Acme Inc.")))
  (it "should leave non-corporations alone."
      (should= "Acme Co." (company-name "Acme Co."))))

(run-specs)

