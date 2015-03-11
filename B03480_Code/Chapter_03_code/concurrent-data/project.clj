(defproject concurrent-data "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 ; [speclj "3.0.2"]

                 ;; Miscellaneous Libraries.
                 ; [org.clojure/data.json "0.2.5"]
                 ; [org.clojure/java.classpath "0.2.1"]

                 ;; 03.01
                 [org.clojure/data.csv "0.1.2"]

                 ;; 03.12
                 ; [org.clojure/data.csv "0.1.2"]
                 ]
  :plugins [[speclj "2.1.2"]]
  :test-paths ["spec/"])
