
(ns getting-data.read-html-text-spec
  (:use [speclj.core :exclude [before after]]
        [incanter.core]
        [getting-data.read-html-text])
  (:import [java.net URL]))

(def data-url "http://people.virginia.edu/~err8n/clj/small-sample-list.html")

(def ds (load-data data-url))

(describe
  "The web page scraper"
  (it "should include the right column names."
      (should= [:family :name :relationship] (col-names ds)))
  (it "should have 17 rows."
      (should= 17 (nrow ds)))
  (it "should include 2 families."
      (should= 2 (count (get-categories :family ds))))
  (it "should include 11 relationships."
      (should= 11 (count (get-categories :relationship ds))))
  (it "should not wrap the first field into a list."
      (should-not (seq? ($ 0 0 ds)))))

(run-specs)

