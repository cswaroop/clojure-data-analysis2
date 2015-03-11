(defproject web-viz "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring/ring-core "1.3.1"]
                 [ring/ring-jetty-adapter "1.3.1"]
                 [compojure "1.2.0"]
                 [hiccup "1.0.5"]
                 [org.clojure/clojurescript "0.0-2371"]
                 [org.clojure/data.csv "0.1.2"]
                 #_[
                 [org.clojure/data.json "0.2.5"]
                 [nz.ac.waikato.cms.weka/weka-dev "3.7.11"]
                 [speclj "3.1.0"]]
                 ]
  :plugins [[lein-ring "0.8.3"]
            [lein-cljsbuild "1.0.3"]
            #_[speclj "2.5.0"]]
  ; :test-paths ["spec"]
  :cljsbuild {:builds
              [{:source-paths ["src-cljs"],
                :compiler
                {:pretty-print true,
                 :output-to "resources/js/script.js",
                 :optimizations :whitespace}}]}
  :ring {:handler web-viz.web/app}
  ; ; :ring {:handler web-viz.tmp-web/app}
            )

