
(ns d-mining.hierarchy-viz
  (:gen-class)
  (:use d-mining.weka)
  (:import [weka.gui.explorer ClustererPanel VisualizePanel]
           weka.gui.hierarchyvisualizer.HierarchyVisualizer
           [weka.gui.visualize PlotData2D]
           javax.swing.JFrame
           [java.awt Container GridLayout]))

(defn -main [& args]
  (let [iris (doto (load-arff "data/UCI/iris.arff")
               (.setClassIndex 4))
        iris-petal (filter-attributes iris [:sepallength :sepalwidth :class])
        iris-sepal (filter-attributes iris [:petallength :petalwidth :class])
        clusters (hierarchical iris-petal :k 3 :print-newick true)
        _ (println clusters)
        _ (println (reduce (fn [m n]
                             (let [c (.clusterInstance clusters (.get iris-petal n))]
                               (assoc m c (inc (m c 0)))))
                           {}
                           (range (.numInstances iris-petal))))

        j-frame (doto (JFrame. "Iris Hierarchy")
                  (.setSize 600 400)
                  (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE))
        content (doto (.getContentPane j-frame)
                  (.setLayout (GridLayout. 1 1)))
        hcv (HierarchyVisualizer. (.graph clusters))]
    (.add content hcv)
    (.setVisible j-frame true)))


