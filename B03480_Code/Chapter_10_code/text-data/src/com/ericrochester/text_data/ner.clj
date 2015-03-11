
(ns com.ericrochester.text-data.ner
  (:require [opennlp.nlp :as nlp]))

#_
(require '[opennlp.nlp :as nlp])

(def get-persons
  (nlp/make-name-finder "models/en-ner-person.bin"))
(def get-orgs
  (nlp/make-name-finder "models/en-ner-organization.bin"))
(def get-date
  (nlp/make-name-finder "models/en-ner-date.bin"))
(def get-location
  (nlp/make-name-finder "models/en-ner-location.bin"))
(def get-money
  (nlp/make-name-finder "models/en-ner-money.bin"))

(comment

(def sotu (tokenize (slurp "sotu/2013-0.txt")))

(doseq [getter [get-persons get-orgs get-date get-location get-money]]
  (prn (getter sotu)))

  )

