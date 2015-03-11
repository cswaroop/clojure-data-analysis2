
(ns web-viz.web
  (:require [compojure.route :as route]
            [compojure.handler :as handler])
  (:use compojure.core
        ring.adapter.jetty
        [ring.middleware.content-type :only (wrap-content-type)]
        [ring.middleware.file :only (wrap-file)]
        [ring.middleware.file-info :only (wrap-file-info)]
        [ring.middleware.stacktrace :only (wrap-stacktrace)]
        [ring.util.response :only (redirect)]
        [hiccup core element page]
        [hiccup.middleware :only (wrap-base-url)]
        web-viz.core))

(defn chart-li [title url]
  [:li [:a {:href url} title]
   " (" [:a {:href (str url "/data.json")} "data"] ")"])

(defn index-page []
  (html5
    [:head
     [:title "Web Charts"]]
    [:body
     [:h1 {:id "web-charts"} "Web Charts"]
     [:ol
      #_ [:li [:a {:href "/ibm.json"} "IBM Data"]]
      [:li [:a {:href "/data/census-race.json"} "2010 Census Race Data"]]
      (chart-li "Scatter Chart" "/scatter")
      (chart-li "Bar Chart" "/barchart")
      (chart-li "Histogram" "/histogram")
      (chart-li "Force-Directed Layout" "/force")
      (chart-li "Interactive Force" "/int-force")
      [:li [:a {:href "/ibm-stock"} "IBM Stock Data"]
       " (" [:a {:href "/ibm-stock/data.csv"} "data"] ")"]
      (comment
      (chart-li "Line Plot" "/lineplot")
      (chart-li "Box Plot" "/boxplot")
      (chart-li "Scatter Line" "/scatterline")
        )]
     (include-js "js/script.js")
     #_
     (javascript-tag
       "webviz.core.hello('from ClojureScript!');")]))

  (comment
  (GET "/ibm.json" [] (ibm-json))

  (GET "/lineplot" [] (line-plot))
  (GET "/lineplot/data.json" [] (ibm-json))

  (GET "/boxplot" [] (box-plot))
  (GET "/boxplot/data.json" [] (ibm-json))

  (GET "/scatterline" [] (scatter-line-plot))
  (GET "/scatterline/data.json" [] (scatter-data))

    )

(defroutes site-routes
  (GET "/scatter" [] (scatter-charts))
  (GET "/scatter/data.json" []
       (redirect "/data/census-race.json"))

  (GET "/barchart" [] (bar-chart))
  (GET "/barchart/data.json" []
       (redirect "/data/chick-weight.json"))

  (GET "/histogram" [] (hist-plot))
  (GET "/histogram/data.json" []
       (redirect "/data/abalone.json"))

  (GET "/force" [] (force-layout-plot))
  (GET "/force/data.json" []
       (redirect "/data/clusters.json"))

  (GET "/int-force" [] (interactive-force-plot))
  (GET "/int-force/data.json" []
       (redirect "/data/clusters.json"))

  (GET "/ibm-stock" [] (time-series))
  (GET "/ibm-stock/data.csv" [] (redirect "/data/ibm.csv"))

  (GET "/" [] (index-page))
  (route/resources "/")
  (route/not-found "Page not found"))

(def app
  (-> (handler/site site-routes)
    (wrap-file "resources")
    (wrap-file-info)
    (wrap-content-type)))

(defn -main
  []
  (run-jetty app {:port 3000}))

