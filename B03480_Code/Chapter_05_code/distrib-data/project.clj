(defproject distrib-data "0.1.0"
  :dependencies [[org.clojure/clojure "1.6.0"]

                 ;; 05.01
                 ; [cascalog "2.1.1"]
                 ; [org.slf4j/slf4j-api "1.7.7"]

                 ; ; ;; 05.02
                 ; [incanter "1.5.5"]
                 ; [congomongo "0.4.4"]
                 ; [org.mongodb/mongo-java-driver "2.12.3"]

                 ; ; ;; 05.05
                 ; [cascading "1.0.17-SNAPSHOT"]

                 ; ; ;; 05.11
                 ; [avout "0.5.4"]
                 ; [commons-codec/commons-codec "20041127.091804"]

                 ; [org.clojure/data.csv "0.1.2"]
                 ]
  ; :plugins [[speclj "2.3.1"]]
  ; :repositories [["conjars.org" "http://conjars.org/repo"]]
  ; :profiles {:dev {:dependencies [[org.apache.hadoop/hadoop-core "1.1.1"]]}}
  :test-paths ["spec/"])
