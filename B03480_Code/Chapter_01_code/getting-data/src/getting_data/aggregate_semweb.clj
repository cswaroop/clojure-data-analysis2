
(ns getting-data.aggregate-semweb
  (:require [clojure.java.io :as io]
            [clojure.xml :as xml]
            [clojure.string :as string]
            [clojure.pprint :as pp]
            [clojure.zip :as zip])
  (:use [incanter.core]
        [edu.ucdenver.ccp.kr.kb]
        [edu.ucdenver.ccp.kr.rdf]
        [edu.ucdenver.ccp.kr.sparql]
        [edu.ucdenver.ccp.kr.sesame.kb]
        [clojure.tools.logging :only [info error]]
        [getting-data.read-rdf :only (init-kb kb-memstore rekey)]
        [getting-data.read-sparql :only (query-sparql-results binding-str)])
  (:import [java.io File]
           [java.net URL URLEncoder]))

(defn open-sesame
  "This opens a remote sesame KB."
  ([] (open-sesame "http://localhost:8080/openrdf-sesame" "cljbook"))
  ([sparql-uri] (open-sesame sparql-uri "cljbook"))
  ([sparql-uri repo]
   (open (new-sesame-server :server sparql-uri :repo-name repo))))

;; DBpedia data.
(defn split-symbol
  "This splits a string on an index and returns a symbol created by using the
  first part as the namespace and the second as the symbol."
  ([kb string index]
     (if-let [ns-prefix (get (:ns-map-to-short kb) (.substring string 0 index))]
       (symbol ns-prefix (.substring string index))
       (symbol string))))

(defn str-to-ns
  "This maps a URI string to a ns and symbol, given the namespaces registered
  in the KB."
  ([uri-string] (str-to-ns *kb* uri-string))
  ([kb uri-string]
   (let [index-gens (list #(.lastIndexOf uri-string (int \#))
                          #(.lastIndexOf uri-string (int \/)))]
     (if-let [index (first (filter #(> % -1) (map (fn [f] (f)) index-gens)))]
       (split-symbol kb uri-string (inc index))
       (symbol uri-string)))))

(def xmls "http://www.w3.org/2001/XMLSchema#")

(defmulti from-xml (fn [r] [(:tag r) (:datatype (:attrs r))]))

(defmethod from-xml [:uri nil] [r]
  (str-to-ns (apply str (:content r))))
(defmethod from-xml [:literal nil] [r]
  (apply str (:content r)))
(defmethod from-xml [:literal (str xmls 'int)] [r]
  (read-string (apply str (:content r))))
(defmethod from-xml :default [r]
  (apply str (:content r)))

(defn tee
  "Print a value and pass it on."
  ([value]
   (.flush System/out)
   value))

(defn result-to-triple
  "This converts a result node into a triple vector."
  ([iri r]
   (let [{:keys [tag attrs content]} r
         [p o] content]
     [iri
      (str-to-ns (binding-str p))
      (from-xml (first (:content o)))])))

(defn load-dbpedia
  "This loads data from dbpedia for a specific IRI into a KB."
  ([kb sparql-uri iri]
   (binding [*kb* kb]
     (->>
       kb
       (query-sparql-results sparql-uri iri)
       (map #(result-to-triple iri %))
       (add-statements kb)))))

(defn query-v [kb q]
  (println (binding [*kb* kb]
             (sparql-select-query q)))
  (query kb q))

(defn load-same-as
  "This takes the results of a query for owl:sameAs and loads the object URIs
  into the triple store from DBPedia."
  ([kb [_ _ same-as]]
   (load-dbpedia kb "http://dbpedia.org/sparql" same-as)
   kb))

(defn aggregate-dataset
  [t-store data-file q col-map]
  (binding [*kb* t-store]
    ;;; Load primary data.
    (load-rdf-file t-store (File. data-file))
    ;;; Load associated data.
    (reduce load-same-as
            t-store
            (query-rdf t-store nil 'owl/sameAs nil))
    ;;; Query 
    (to-dataset (map (partial rekey col-map) (query t-store q)))))

