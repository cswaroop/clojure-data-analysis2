
;; TODO: Improve the data model here. Make the comparison explicit (i.e., not
;; implicitly the USD) and make it configurable.

(ns getting-data.aggregate-sources-spec
  (:use [speclj.core]
        [incanter.core]
        [getting-data.read-rdf :only (kb-memstore init-kb)]
        [getting-data.aggregate-sources]))

;; Currency data.
(def data-url-ttl "http://telegraphis.net/data/currencies/currencies.ttl")
(def data-url-xml "http://telegraphis.net/data/currencies/currencies.rdf")
(def data-file "data/currencies.ttl")

(def q 
  '((?/c rdf/type money/Currency)
      (?/c money/name ?/name)
      (?/c money/shortName ?/shortName)
      (?/c money/isoAlpha ?/iso)
      (?/c money/minorName ?/minorName)
      (?/c money/minorExponent ?/minorExponent)
      (:optional
        ((?/c err/exchangeRate ?/exchangeRate)
           (?/c err/exchangeWith ?/exchangeWith)
           (?/c err/exchangeRateDate ?/exchangeRateDate)))))

(def col-map {'?/name :fullname
              '?/iso :iso
              '?/shortName :name
              '?/minorName :minor-name
              '?/minorExponent :minor-exp
              '?/exchangeRate :exchange-rate
              '?/exchangeWith :exchange-with
              '?/exchangeRateDate :exchange-date})

;; HTML data
(def data-url "http://www.x-rates.com/table/?from=USD&amount=1.00")

(let [kb (init-kb (kb-memstore))
      ds (aggregate-data kb data-file data-url q col-map)]
  (describe
    "aggregating data from multiple sources."
    (it "should have the right fields"
        (should= [:exchange-date :exchange-rate :exchange-with :fullname
                  :iso :minor-exp :minor-name :name]
                 (sort (col-names ds))))
    (it "should have data"
        (should (> (nrow ds) 0)))
    (it "should have currency data from the data file."
        (should
          (> (count (filter #(not (nil? %)) (get-categories :iso ds))) 0)))
    (it "should have exchange data from the web page."
        (should
          (> (count (filter #(not (nil? %)) (get-categories :exchange-with ds)))
             0)))))

(run-specs)

