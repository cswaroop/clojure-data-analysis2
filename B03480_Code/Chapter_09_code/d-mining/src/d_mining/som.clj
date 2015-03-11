
(ns d-mining.som
  (:require [incanter.core :as i]
            [incanter.som :as som]))

(comment
(require '[incanter.core :as i]
         '[incanter.som :as som]
         'incanter.datasets)
(def iris (incanter.datasets/get-dataset :iris))
  ; :from-repo false :incanter-home "./incanter"))

;;; This might be the best.
(def iris-clusters
  (som/som-batch-train
    (i/to-matrix
      (i/sel iris
             :cols [:Sepal.Length :Sepal.Width
                    :Petal.Length :Petal.Width]))))

(doseq [[pos rws] (:sets iris-clusters)]
  (println pos \:
           (frequencies (i/sel iris :cols :Species :rows rws))))

  (defn try-som [alpha beta cycles]
    (let [iris-clusters
          (som/som-batch-train
            (i/to-matrix
              (i/sel iris
                     :cols [;:Sepal.Length :Sepal.Width
                            :Petal.Length :Petal.Width]))
            :cycles cycles :alpha alpha :beta beta)]
      (doseq [[pos rws] (:sets iris-clusters)]
        (println pos \:
                 (frequencies (i/sel iris :cols :Species :rows rws))))))


  (i/$rollup :count :Sepal.Length :Species
      (i/sel iris :rows (get (:sets iris-clusters) [4 1])))

  (require '[clojure.data.json :as json]
           '[clojure.java.io :as io])

  (defn get-data-points [dataset cols pair]
    (let [[[x y] rows] pair]
      (map #(assoc % :x x :y y)
           (:rows
             (i/col-names (i/sel dataset :rows rows) cols)))))

  (let [[dim-x dim-y] (:dims iris-clusters)
        output {:dimensions {:x dim-x :y dim-y}
                :data (mapcat (partial get-data-points iris
                                       [:sepal_length :sepal_width
                                        :petal_length :petal_width :class])
                              (:sets iris-clusters))}]
    (with-open [w (io/writer "charts/iris-soms.json")]
      (json/write output w)))

(doseq [[pos rws] (:sets iris-clusters)]
  (println pos \:
           (i/sel iris :cols :Species :rows rws)
           \newline))

(use 'd-mining.hierarchical)
(def data-file "data/all_160_in_51.P3.csv")
(def dataset (incanter.io/read-dataset data-file :header true))
(def va-data
  (doall
    (map #(apply ->VaRacialLocation %)
         (i/to-list
           (i/sel dataset
                  :cols
                  [:GEOID :STATE :NAME :POP100 :P003002 :P003003 :P003004
                   :P003005 :P003006 :P003007 :P003008])))))
(def va-matrix
  (i/to-matrix
    (i/dataset
      [:white :black :indian :asian :hawaii :other :multiple]
      (map get-point va-data))))
(def som-out (som/som-batch-train va-matrix))
(doseq [rws (vals (:sets som-out))]
  (println (sort (i/sel dataset :cols :NAME :rows rws)) \newline))

;; Probably the best one. Clustering them based on their income distribution.
(def data-file "data/world-bank-filtered.csv")
(def dataset (incanter.io/read-dataset data-file :header true))
(def som-out
  (som/som-batch-train
    (i/to-matrix
      (i/sel dataset
             :cols [:SI.DST.04TH.20 :SI.POV.GINI :SI.DST.FRST.20
                    :SI.DST.05TH.20 :SI.DST.10TH.10 :SI.DST.02ND.20
                    :SI.DST.FRST.10 :SI.DST.03RD.20]))
    :cycles 5))
(doseq [rws (vals (:sets som-out))]
  (println (i/sel dataset :cols :cname :rows rws) \newline))

  )

