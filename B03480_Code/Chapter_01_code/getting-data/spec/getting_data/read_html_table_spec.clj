
(ns getting-data.read-html-table-spec
  (:use [speclj.core :exclude [before after]]
        [incanter.core]
        [getting-data.read-html-table]))

(def data-url "http://people.virginia.edu/~err8n/clj/small-sample-table.html")

(def ds (load-data data-url))

(describe
  "to-keyword"
  (it "should return a keyword."
      (should (keyword? (to-keyword "hi there"))))
  (it "should lower-case all letters."
      (should= :hithere (to-keyword "HiThere")))
  (it "should replace spaces with dashes."
      (should= :hi-there (to-keyword "Hi There"))))

(describe
  "The web table scraper"
  (it "should pull column names from the table columns."
      (should= [:given-name :surname :relation] (col-names ds)))
  (it "should read eleven data rows."
      (should= 11 (nrow ds)))
  (it "should read three columns."
      (should= 3 (ncol ds))))

(run-specs)

