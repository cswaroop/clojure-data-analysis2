
(ns getting-data.read-xml-spec
  (:use [speclj.core]
        [incanter.core]
        [getting-data.read-xml]
        [clojure.zip :exclude [next replace remove]]))

(def data-file "data/small-sample.xml")

(def ds (load-xml-data data-file down right))

(describe
  "XML Reader"
  (it "should pull column names from the object keys."
      (should= [:given-name :surname :relation] (col-names ds)))
  (it "should read eleven data rows."
      (should= 11 (nrow ds)))
  (it "should read three columns."
      (should= 3 (ncol ds))))

(run-specs)

