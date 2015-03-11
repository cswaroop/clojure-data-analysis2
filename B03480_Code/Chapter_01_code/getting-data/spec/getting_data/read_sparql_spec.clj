
(ns getting-data.read-sparql
  (:use [speclj.core]
        [incanter.core]
        [getting-data.read-sparql]))

(def rdfs "http://www.w3.org/2000/01/rdf-schema#")
(def dbpedia "http://dbpedia.org/resource/")
(def dbpedia-ont "http://dbpedia.org/ontology/")
(def dbpedia-prop "http://dbpedia.org/property/")

(def col-map {(str rdfs 'label) :name,
              (str dbpedia-prop 'usingCountries) :country
              (str dbpedia-prop 'peggedWith) :pegged-with
              (str dbpedia-prop 'symbol) :symbol
              (str dbpedia-prop 'usedBanknotes) :used-banknotes
              (str dbpedia-prop 'usedCoins) :used-coins
              (str dbpedia-prop 'inflationRate) :inflation})

(def subject (symbol "http://dbpedia.org/resource//United_Arab_Emirates_dirham"))

(def endpoint "http://dbpedia.org/sparql")

(def ds (load-data endpoint subject col-map))
(print ds)

(describe
  "The SPARQL reader"
  (it "should have the right columns."
      (should= [:country :inflation :name :pegged-with :symbol
                :used-banknotes :used-coins]
               (sort (col-names ds))))
  (it "should have the 1 row."
      (should= 1 (nrow ds)))
  (it "should have the right values."
      (should= "United Arab Emirates dirham" ($ :name ds))
      (should= "United Arab Emirates" ($ :country ds))
      (should= "U.S. dollar = 3.6725 dirhams" ($ :pegged-with ds))
      (should= "د.إ" ($ :symbol ds))
      (should= "9223372036854775807" ($ :used-banknotes ds))
      (should= "2550" ($ :used-coins ds))
      (should= "14" ($ :inflation ds))))

(run-specs)

