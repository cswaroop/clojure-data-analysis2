
(ns getting-data.read-xml
  (:require [incanter.core :as i]
            [clojure.xml :as xml]
            [clojure.zip :as zip]))

#_
(require '[incanter.core :as i]
         '[clojure.xml :as xml]
         '[clojure.zip :as zip])

(defn load-xml-data
  "This loads data from an XML file.

  The parameters are

  * the file name;
  * a zipper function to move to the first data element;
  * a zipper function to move to the next data element.
  "
  [xml-file first-data next-data]
  (let [data-map (fn [node]
                   [(:tag node) (first (:content node))])]
    (->>
      (xml/parse xml-file)
      zip/xml-zip
      first-data
      (iterate next-data)
      (take-while #(not (nil? %)))
      (map zip/children)
      (map #(mapcat data-map %))
      (map #(apply array-map %))
      i/to-dataset)))

#_
(def d
  (load-xml-data "data/crime_incidents_2013_plain.xml"
                 zip/down zip/right))
#_
(i/col-names d)
#_
(i/nrow d)

