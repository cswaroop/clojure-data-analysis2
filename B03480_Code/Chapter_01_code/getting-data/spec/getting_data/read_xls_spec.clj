
(ns getting-data.read-xls-spec
  (:use [speclj.core]
        [incanter.core]
        [incanter.excel]))

(def data-file "data/small-sample-header.xls")
(def ds (read-xls data-file))

(describe
  "read-xls"
  (it "should pull column names from the first row."
      (should= ["given-name" "surname" "relation"] (col-names ds)))
  (it "should read eleven rows of data."
      (should= 11 (nrow ds)))
  (it "should read three columns."
      (should= 3 (ncol ds))))

(run-specs)

