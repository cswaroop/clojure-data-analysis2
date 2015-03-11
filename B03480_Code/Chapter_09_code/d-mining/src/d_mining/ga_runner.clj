
(ns d-mining.ga-runner
  (:gen-class)
  (:require [clojure.pprint :as pp]
            [incanter.core :as i]
            incanter.io
            incanter.datasets
            [d-mining.k-means :as km]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:use d-mining.ga))

;;;; {{{ Utilities
(defn stop-at
  ([ga]
   (let [[_ f] (get-best-solution ga)]
     (>= f 0.999))))
; }}}

;;;; {{{ Reporting
(def history-fields
  [:date :generation :population-count :min :quantile-25 :quantile-50
   :quantile-75 :max :mean :median :sd :variance :kurtosis :skewness])

(defn write-history
  ([history filename]
   (with-open [f-out (io/writer (io/as-file filename))]
     (csv/write-csv f-out [history-fields])
     (csv/write-csv f-out (map (partial map second) history)))))

(defn write-best
  ([cluster fitness member-index filename]
   (with-open [f-out (io/writer (io/as-file filename))]
     (binding [*out* f-out]
       (println "Best Solution")
       (println "=============")
       (println)
       (println "Fitness: " fitness)
       (println)
       (println "Clusters")
       (doseq [[centroid members] member-index]
         (println centroid)
         (doseq [m members]
           (println \tab (:data m))))))))

(defn write-final
  ([solutions filename]
   (with-open [f-out (io/writer (io/as-file filename))]
     (binding [*out* f-out]
       (doseq [[c f] (sort-by second > solutions)]
         (printf "% 2.4f\t%d\t%s\n" f (count c)
                 (doall
                   (str/join \|
                             (map (partial str/join \,)
                                  c)))))))))

; }}}

;;;; {{{ -main

(defn cluster-iris
  ([]
   (let [data (incanter.datasets/get-dataset :iris
                                             :incanter.home "."
                                             :from-repo false)
         iris (i/$map km/->Iris [:Species :Sepal.Length :Sepal.Width
                                 :Petal.Length :Petal.Width] data)]
     ["iris" (ga iris 50 [2 12] 1000 :stop-fn stop-at)])))

(defn cluster-census
  ([]
   (let [data-file "data/census-data.csv"
         dataset (incanter.io/read-dataset data-file :header true)
         c-data (doall
                  (map #(apply km/->CensusLocation %)
                       (i/to-list dataset)))]
     ["census" (ga c-data 50 [4 1000] 1000 :stop-fn stop-at)])))

(defn cluster-va
  ([]
   (let [data-file "data/all_160_in_51.P3.csv"
         dataset (incanter.io/read-dataset data-file :header true)
         va-data (doall
                   (map #(apply km/->VaRacialLocation %)
                        (i/to-list
                          (i/sel dataset
                                 :cols
                                 [:GEOID :STATE :NAME :POP100 :P003002 :P003003
                                  :P003004 :P003005 :P003006 :P003007
                                  :P003008]))))]
     ["va" (ga va-data 100 [5 150] 1000 :stop-fn stop-at)])))

(defn -main
  ([& args]
   (let [[tag output] #_(cluster-iris) (cluster-census) #_(cluster-va)
         [best best-fit] (apply max-key second (:solutions output))
         pop-clusters (mapv vector (:population (:ga output)) (:cluster-index (:ga output)))
         clusters (->>
                    pop-clusters
                    (filter (comp (partial = best) first))
                    first
                    second)]

     (println "Writing history to " tag "-ga.csv.")
     (write-history (:history output) (str tag "-ga.csv"))

     (println "Writing best to " tag "-best.txt.")
     (write-best best best-fit clusters (str tag "-best.txt"))

     (println "Writing full final population to " tag "-final.txt.")
     (write-final (:solutions output) (str tag "-final.txt")))))

; }}}
