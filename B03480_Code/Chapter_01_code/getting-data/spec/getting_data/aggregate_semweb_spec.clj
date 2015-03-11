
(ns getting-data.aggregate-semweb-spec
  (:use [speclj.core]
        [incanter.core]
        [getting-data.aggregate-semweb]
        [getting-data.read-rdf :only (init-kb kb-memstore)]))

;; Currency data.
(def data-url-ttl "http://telegraphis.net/data/currencies/currencies.ttl")
(def data-url-xml "http://telegraphis.net/data/currencies/currencies.rdf")
(def data-file "data/currencies.ttl")

;; DBpedia data.
(def col-map {'?/name :fullname
              '?/iso :iso
              '?/shortName :name
              '?/symbol :symbol
              '?/country :country
              '?/minorName :minor-name
              '?/minorExponent :minor-exp
              '?/peggedWith :pegged-with
              '?/usedBanknotes :used-banknotes
              '?/usedCoins :used-coins})

(def q '((?/c rdf/type money/Currency)
           (?/c owl/sameAs ?/d)
           (?/c money/name ?/name)
           (?/c money/shortName ?/shortName)
           (?/c money/isoAlpha ?/iso)
           (?/c money/minorName ?/minorName)
           (?/c money/minorExponent ?/minorExponent)
           (:optional ((?/d dbpedia-prop/symbol ?/symbol)))
           (:optional ((?/d dbpedia-ont/usingCountry ?/country)))
           (:optional ((?/d dbpedia-prop/peggedWith ?/peggedWith)))
           (:optional ((?/d dbpedia-prop/usedBanknotes ?/usedBanknotes)))
           (:optional ((?/d dbpedia-prop/usedCoins ?/usedCoins)))))

(describe
  "Aggregating semantic web data"
  (let [ds (aggregate-dataset (init-kb (kb-memstore)) data-file q col-map)]
    (it "should have the right columns."
        (should= [:country :fullname :iso :minor-exp :minor-name :name
                  :pegged-with :symbol :used-banknotes :used-coins]
                 (sort (col-names ds))))))

(run-specs)

