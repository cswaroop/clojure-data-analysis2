
(ns getting-data.read-sparql
  (:require [clojure.java.io :as io]
            [clojure.xml :as xml]
            [clojure.pprint :as pp]
            [clojure.zip :as zip])
  (:use [incanter.core]
        [edu.ucdenver.ccp.kr.kb]
        [edu.ucdenver.ccp.kr.rdf]
        [edu.ucdenver.ccp.kr.sparql]
        [edu.ucdenver.ccp.kr.sesame.kb]
        [getting-data.read-rdf :only (kb-memstore init-kb rekey)])
  (:import [java.io File]
           [java.net URL URLEncoder]))
#_
(require '[clojure.java.io :as io]
         '[clojure.xml :as xml]
         '[clojure.zip :as zip])
#_
(import [java.io File]
        [java.net URL URLEncoder])

(defn tee
  ([tag] (fn [x] (tee tag x)))
  ([tag x] (println tag) (pp/pprint x) (println) x))
(defn cp
  ([tag] (fn [x] (cp tag x)))
  ([tag x] (println tag) x))

(defn open-sesame
  "This opens a remote sesame KB."
  [sparql-uri]
  (open (new-sesame-server :server sparql-uri :repo "")))

(defn make-query
  "This creates a query that returns all the triples related to a subject URI.
  It does filter out non-English strings."
  ([subject kb]
   (binding [*kb* kb
             *select-limit* 200]
     (sparql-select-query
       (list `(~subject ?/p ?/o)
             '(:or (:not (:isLiteral ?/o))
                   (!= (:datatype ?/o) rdf/langString)
                   (= (:lang ?/o) ["en"])))))))

(defn make-query-uri
  "This constructs a URI for the query."
  ([base-uri query]
   (URL. (str base-uri
              "?format=" (URLEncoder/encode "text/xml")
              "&query=" (URLEncoder/encode query)))))

(defn result-seq
  "This takes the first result and returns a sequence of this node, plus all
  the nodes to the right of it."
  ([first-result]
   (cons (zip/node first-result)
         (zip/rights first-result))))

(defn binding-str
  "This takes a binding, pulls out the first tag's content, and concatenates it
  into a string."
  ([b]
   (apply str (:content (first (:content b))))))

(defn result-to-kv
  "This takes a result node and creates a key-value vector pair from it."
  ([r]
   (let [[p o] (:content r)]
     [(binding-str p) (binding-str o)])))

(defn accum-hash
  "This takes a map and key-value vector pair and adds the pair to the map. If
  the key is already in the map, the current value is converted to a vector and
  the new value is added to it."
  ([m [k v]]
   (if-let [current (m k)]
     (assoc m k (str current \space v))
     (assoc m k v))))

(defn query-sparql-results
  "This queries a SPARQL endpoint and returns a sequence of result nodes."
  ([sparql-uri subject kb]
   (->>
     kb
     ;; Build the URI query string.
     (make-query subject)
     (make-query-uri sparql-uri)
     ;; Get the results, parse the XML, and return the zipper.
     io/input-stream
     xml/parse
     zip/xml-zip
     ;; Find the first child.
     zip/down
     zip/right
     zip/down
     ;; Convert all children into a sequence.
     result-seq)))

(defn load-data
  "This loads the data about a currency for the given IRI."
  [sparql-uri subject col-map]
  (->>
    ;; Initialize the triple store.
    (kb-memstore)
    init-kb
    ;; Get the reults
    (query-sparql-results sparql-uri subject)
    ;; Generate a mapping.
    (map result-to-kv)
    (reduce accum-hash (array-map))
    ;; Translate the keys in the map.
    (rekey col-map)
    ;; And create a dataset.
    to-dataset))

(comment

(def rdfs "http://www.w3.org/2000/01/rdf-schema#")
(def dbpedia "http:///dbpedia.org/resource/")
(def dbpedia-ont "http://dbpedia.org/ontology/")
(def dbpedia-prop "http://dbpedia.org/property/")

(def col-map {(str rdfs 'label) :name,
              (str dbpedia-prop 'usingCountries) :country
              (str dbpedia-prop 'peggedWith) :pegged-with
              (str dbpedia-prop 'symbol) :symbol
              (str dbpedia-prop 'usedBanknotes) :used-banknotes
              (str dbpedia-prop 'usedCoins) :used-coins
              (str dbpedia-prop 'inflationRate) :inflation})

(def d (load-data "http://dbpedia.org/sparql" 
           (symbol (str dbpedia "United_Arab_Emirates_dirham"))
           col-map))
(sel d :cols [:country :name :symbol])
  )

