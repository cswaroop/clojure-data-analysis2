(defproject cleaning-data "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [valip "0.2.0"]
                 ; [speclj "3.0.2"]

                 ;; 2.03
                 [clj-diff "1.0.0-SNAPSHOT"]

                 ;; 2.06
                 [clj-time "0.9.0-beta1"]

                 ;; 2.07
                 [org.clojure/data.csv "0.1.2"]

                 ;; 2.11
                 [parse-ez "0.3.6"]

                 ;; 2.12
                 ]
  :plugins [[speclj "2.1.2"]]
  :jvm-opts ["-Xmx768M"]
  :test-paths ["spec/"])
