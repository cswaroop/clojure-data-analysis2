
(ns webviz.int-force
  (:require [clojure.browser.dom :as dom]
            [webviz.force :as force]
            [goog.events :as gevents]))

(defn dl-item [title data key]
  (let [val2000 (aget data (str key "-2000"))]
    (str "<dt>" title "</dt>"
         "<dd>" (.round js/Math (aget data key))
         " <em>(2000: " (.round js/Math val2000) ")</em>"
         "</dd>")))

(defn update-data [node]
  (let [data (aget node "data")
        content
        (str "<h2>" (aget node "name") "</h2>"
             "<dl>"
             (dl-item "Total" data "race-total")
             (dl-item "White" data "race-white")
             (dl-item "African-American" data
                      "race-black")
             (dl-item "Native American" data
                      "race-indian")
             (dl-item "Asian" data "race-asian")
             (dl-item "Hawaiian" data "race-hawaiian")
             (dl-item "Other" data "race-other")
             (dl-item "Multi-racial" data
                      "race-two-more")
             "</dl>")]
    (dom/remove-children :datapane)
    (dom/append
      (dom/get-element :datapane)
      (dom/html->dom content))))

(defn on-mouseover [ev]
  (let [target (.-target ev)]
    (if (= (.-nodeName target) "circle")
      (let [n (+ (.getAttribute target "data-n"))]
        (update-data (aget (.-nodes @force/census-graph) n))))))

(defn ^:export interactive-force-layout []
  (force/force-layout)
  (gevents/listen (dom/get-element "force")
                  (.-MOUSEOVER gevents/EventType)
                  on-mouseover))

; vim: set filetype=clojure:
