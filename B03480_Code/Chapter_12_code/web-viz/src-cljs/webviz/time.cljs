
(ns webviz.time)

(defn get-dimensions [margin]
  [(- 960 (:left margin) (:right margin))
   (- 500 (:top margin) (:bottom margin))])

(defn get-scales [width height]
  [(.. js/d3 -time scale (range #js [0 width]))
   (.. js/d3 -scale linear (range #js [height 0]))])

(defn get-axes [x y]
  [(.. js/d3 -svg axis (scale x) (orient "bottom"))
   (.. js/d3 -svg axis (scale y) (orient "left"))])

(defn get-line [x y]
  (.. js/d3 -svg line
      (x #(x (.-date %)))
      (y #(y (.-close %)))))

(defn get-svg [margin width height]
  (.. js/d3 (select "svg")
      (attr "width" (+ width (:left margin)
                       (:right margin)))
      (attr "height" (+ height (:top margin)
                        (:bottom margin)))
      (append "g")
      (attr "transform" (str "translate(" (:left margin)
                             \, (:top margin) \)))))

(defn coerce-datum [parse-date d]
  (aset d "date" (parse-date (.-date d)))
  (aset d "close" (js/parseFloat (.-close d))))

(defn set-domains [x y data]
  (.domain x (.extent js/d3 data #(.-date %)))
  (.domain y (.extent js/d3 data #(.-close %))))

(defn build-x-axis [height svg x-axis]
  (.. svg (append "g")
      (attr "class" "x axis")
      (attr "transform" (str "translate(0," height \)))
      (call x-axis)))

(defn build-y-axis [svg y-axis]
  (.. svg (append "g")
      (attr "class" "y axis")
      (call y-axis)
      (append "text")
      (attr "transform" "rotate(-90)")
      (attr "y" 6)
      (attr "dy" ".71em")
      (style "text-anchor" "end")
      (text "Price ($)")))

(defn add-line [svg line data]
  (.. svg (append "path")
      (datum data)
      (attr "class" "line")
      (attr "d" line)))

(defn ^:export ibm-stock []
  (let [margin {:top 20, :right 20, :bottom 30, :left 50}
        [width height] (get-dimensions margin)
        parse-date (.. js/d3 -time (format "%d-%b-%y") -parse)
        [x y] (get-scales width height)
        [x-axis y-axis] (get-axes x y)
        line (get-line x y)
        svg (get-svg margin width height)]

    (.csv js/d3
      "/ibm-stock/data.csv"
      (fn [error data]
        (.forEach data
          #(coerce-datum parse-date %))

        (set-domains x y data)

        (build-x-axis height svg x-axis)
        (build-y-axis svg y-axis)
        (add-line svg line data)))))

