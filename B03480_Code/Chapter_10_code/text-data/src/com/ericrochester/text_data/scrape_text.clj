
(ns com.ericrochester.text-data.scrape-text
  (:require [net.cgrand.enlive-html :as html]))

(def blog-post-url
  (str "http://dailypost.wordpress.com"
       "/dp_assignment/writing-101-adverbs/"))

(defn download-page [url]
  (html/html-resource (java.net.URL. url)))

(comment

(def page
  (scrape-text/download-page
    scrape-text/blog-post-url))

(map html/text
     (html/select page
                  #{[:div#dp-header :h1]
                    [:div#content :div#main]}))

         )
