
(ns getting-data.read-html-text
  (:require [clojure.string :as string]
            [net.cgrand.enlive-html :as html]
            [incanter.core :as i])
  (:import [java.net URL]))

#_
(require '[clojure.string :as string]
         '[net.cgrand.enlive-html :as html]
         '[incanter.core :as i])

#_
(import [java.net URL])

(defn get-family
  "This takes an article element and returns the family name."
  [article]
  (string/join (map html/text (html/select article [:header :h2]))))

(defn get-person
  "This takes a list item and returns a map of the persons' name and
  relationship."
  [li]
  (let [[{pnames :content} rel] (:content li)]
    {:name (apply str pnames)
     :relationship (string/trim rel)}))

(defn get-rows
  "This takes an article and returns the person mappings, with the family name
  added."
  [article]
  (let [family (get-family article)]
    (map #(assoc % :family family)
         (map get-person
              (html/select article [:ul :li])))))

(defn load-data
  "This downloads the HTML page and pulls the data out of it."
  [html-url]
  (let [html (html/html-resource (URL. html-url))
        articles (html/select html [:article])]
    (i/to-dataset (mapcat get-rows articles))))

#_
(def d (load-data (str "http://www.ericrochester.com/"
                       "clj-data-analysis/data/"
                       "small-sample-list.html")))

