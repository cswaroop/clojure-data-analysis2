
(ns webviz.force)

(defn create-force [width height]
  (-> js/d3 .-layout
    (.force)
    (.charge -120)
    (.linkDistance 30)
    (.size (array width height))))

(defn create-svg [width height]
  (-> js/d3
    (.select "#force svg")
    (.attr "width" width)
    (.attr "height" height)))

(defn start-force [force graph]
  (-> force
    (.nodes (aget graph "nodes"))
    (.links (aget graph "links"))
    .start))

(defn create-links [svg graph]
  (-> svg
    (.selectAll "line.link")
    (.data (aget graph "links"))
    (.enter)
    (.append "line")
    (.attr "class" "link")
    (.style "stroke-width" #(.sqrt js/Math (inc (aget % "value"))))))
(defn create-nodes [svg force color graph]
  (-> svg
    (.selectAll "circle.node")
    (.data (aget graph "nodes"))
    (.enter)
    (.append "circle")
    (.attr "class" "node")
    (.attr "r" 5)
    (.attr "data-n" #(aget % "n"))
    (.style "fill" #(color (aget % "group")))
    (.call (aget force "drag"))))

(defn on-tick-handler [link node]
  (fn []
    (-> link
      (.attr "x1" #(-> % .-source .-x))
      (.attr "y1" #(-> % .-source .-y))
      (.attr "x2" #(-> % .-target .-x))
      (.attr "y2" #(-> % .-target .-y)))
    (-> node
      (.attr "cx" #(aget % "x"))
      (.attr "cy" #(aget % "y")))))

(defn set-title [node]
  (-> node
    (.append "title")
    (.text #(aget % "name"))))

(def census-graph (atom nil))
(defn render-graph [color force svg graph]
  (swap! census-graph (fn [] graph))
  (start-force force graph)
  (let [link (create-links svg graph)
        node (create-nodes svg force color graph)]
    (set-title node)
    (.on force "tick" (on-tick-handler link node))))

(defn ^:export force-layout []
  (let [width 650, height 500]
    (.json js/d3 "force/data.json"
           (partial
             render-graph
             (.category20c (aget js/d3 "scale"))
             (create-force width height)
             (create-svg width height)))))

; vim: set filetype=clojure:
