
(ns getting-data.read-csv-spec
  (:use [speclj.core]
        [getting-data.read-csv]
        [incanter.core]))

(def ds0 (load-data data-file))
(def dsh (load-data-headers data-file-header))

(describe
  "CSV Reader without headers"
  (it "should assign column names if missing"
      (should= [:col0 :col1 :col2] (col-names ds0)))
  (it "should read eleven data rows"
      (should= 11 (nrow ds0)))
  (it "should read three columns"
      (should= 3 (ncol ds0))))

(describe
  "CSV Reader with headers"
  (it "should read the column names"
      (should= [:given-name :surname :relation] (col-names dsh)))
  (it "should read eleven data rows"
      (should= 11 (nrow dsh)))
  (it "should read three columns"
      (should= 3 (ncol dsh))))

(run-specs)

