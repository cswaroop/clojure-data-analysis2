
(ns getting-data.read-rdf-spec
  (:use [speclj.core]
        [incanter.core]
        [getting-data.read-rdf])
  (:import [java.io File]))

(def data-url-ttl "http://telegraphis.net/data/currencies/currencies.ttl")
(def data-url-xml "http://telegraphis.net/data/currencies/currencies.rdf")
(def data-file "data/currencies.ttl")

(def q '((?/c rdf/type money/Currency)
           (?/c money/name ?/full_name)
           (?/c money/shortName ?/name)
           (?/c money/symbol ?/symbol)
           (?/c money/minorName ?/minor_name)
           (?/c money/minorExponent ?/minor_exp)
           (?/c money/isoAlpha ?/iso)
           (?/c money/currencyOf ?/country)))

(def ds (load-data (init-kb (kb-memstore)) (File. data-file) q))

(describe
  "The RDF reader"
  (it "should have the right columns."
      (should= [:c :country :full-name :iso :minor-exp :minor-name :name :symbol]
               (sort (col-names ds))))
  (it "should have 227 rows."
      (should= 227 (nrow ds))))

(run-specs)

