
(ns getting-data.read-jdbc
  (:require [incanter.core :as i]
            [clojure.java.jdbc :as j]))

#_
(require '[incanter.core :as i]
         '[clojure.java.jdbc :as j])

(defn load-table-data
  "This loads the data from a database table."
  [db table-name]
  (i/to-dataset (j/query db (str "SELECT * FROM " table-name ";"))))

#_
(def db {:subprotocol "sqlite"
         :subname "data/small-sample.sqlite"
         :classname "org.sqlite.JDBC"})

#_
(load-table-data db 'people)
