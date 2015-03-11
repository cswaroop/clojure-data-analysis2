(defproject parallel-data "0.1.0"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [incanter "1.5.5"]

                 ;; 04.07
                 [nio "1.0.2"]
                 [org.apache.commons/commons-lang3 "3.3.2"]
                 [clj-time "0.8.0"]

                 ;; 04.08
                 [org.codehaus.jsr166-mirror/jsr166y "1.7.0"]

                 ;; 04.09
                 [org.clojure/data.csv "0.1.2"]

                 ;; 04.10
                 [calx "0.2.1"]

                 ;; 04.11
                 [criterium "0.4.3"]
                 ]
  :plugins [[speclj "2.1.2"]]
  :test-paths ["spec/"])
