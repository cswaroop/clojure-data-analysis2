
(ns getting-data.read-jdbc-spec
  (:use [speclj.core]
        [incanter.core]
        [getting-data.read-jdbc]))

(def db {:subprotocol "sqlite"
         :subname "data/small-sample.sqlite"
         :classname "org.sqlite.JDBC"})

(def ds (load-table-data db "people"))

(describe
  "JDBC Reader"
  (it "should pull column names from the table columns."
      (should= [:given_name :relation :surname] (sort (col-names ds))))
  (it "should read eleven data rows."
      (should= 11 (nrow ds)))
  (it "should read three columns."
      (should= 3 (ncol ds))))

(run-specs)

