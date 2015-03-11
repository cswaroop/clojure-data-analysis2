
(ns getting-data.read-json-spec
  (:use [speclj.core]
        [incanter.core]
        [clojure.java.io :exclude [copy]]
        [clojure.data.json]
        [getting-data.read-json]))

(def ds (load-data data-file))

(describe
  "JSON Reader"
  (it "should pull column names from object keys."
      (should= [:given_name :surname :relation] (col-names ds)))
  (it "should read eleven data rows"
      (should= 11 (nrow ds)))
  (it "should read three columns"
      (should= 3 (ncol ds))))

(run-specs)

