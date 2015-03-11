
;;; To get mongo-java-driver to install:
;;; > mvn archetype:generate -DarchetypeGroupId=org.apache.maven.archetypes -DgroupId=com.ericrochester -DartifactId=distrib-data
;;; > cd distrib-data/
;;; > # edit pom.xml to have mongo-java-driver as a dependency.
;;; > mvn install

(ns distrib-data.mongo-data
  (:use (incanter core io mongodb)
        somnium.congomongo))

(comment
  (use '(incanter core io mongodb)
       'somnium.congomongo)
  )

(def data-file "data/all_160_in_51.P35.csv")
(def data (read-dataset data-file :header true))
(mongo! :db "va-census")
(insert-dataset
  :db ($ [:GEOID :STATE :NAME :POP100 :HU100 :P035001] data))

;; In a new session.
(def data (fetch-dataset :db))
(col-names data)


