(defproject com.ericrochester/text-data "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cc.mallet/mallet "2.0.7"]
                 [clojure-opennlp "0.3.2"]
                 [colt/colt "1.2.0"]
                 #_[org.apache.opennlp/opennlp-tools "1.5.3"]
                 [enlive "1.1.5"]
                 ]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.5"]]
                   :source-paths ["dev"]}})
