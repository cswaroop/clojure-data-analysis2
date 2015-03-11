
;; TODO: Improve the data model here. Make the comparison explicit (i.e., not
;; implicitly the USD) and make it configurable.

(ns getting-data.aggregate-sources
  (:require [clojure.java.io :as io]
            [clojure.xml :as xml]
            [clojure.string :as string]
            [clojure.zip :as zip]
            [net.cgrand.enlive-html :as html])
  (:use [incanter.core]
        [clj-time.coerce]
        [clj-time.format :only (formatter formatters parse unparse)]
        [edu.ucdenver.ccp.kr.kb]
        [edu.ucdenver.ccp.kr.rdf]
        [edu.ucdenver.ccp.kr.sparql]
        [edu.ucdenver.ccp.kr.sesame.kb]
        [clojure.tools.logging :only [info error]]
        [getting-data.read-rdf :only (kb-memstore init-kb rekey)])
  (:import [java.io File]
           [java.net URL URLEncoder]))

(comment
(require '(clojure.java [io :as io]))
(require '(clojure [xml :as xml]
                   [string :as string]
                   [zip :as zip]))
(require '(net.cgrand [enlive-html :as html]))
(use 'incanter.core
     'clj-time.coerce
     '[clj-time.format :only (formatter formatters parse unparse)]
     'clojure.set
     'edu.ucdenver.ccp.kr.kb
     'edu.ucdenver.ccp.kr.rdf
     'edu.ucdenver.ccp.kr.sparql
     'edu.ucdenver.ccp.kr.sesame.kb)
(import [java.io File]
        [java.net URL URLEncoder])
)  

(defn open-sesame
  "This opens a remote sesame KB."
  ([] (open-sesame "http://localhost:8080/openrdf-sesame" "cljbook"))
  ([sparql-uri] (open-sesame sparql-uri "cljbook"))
  ([sparql-uri repo]
   (open (new-sesame-server :server sparql-uri :repo-name repo))))

(defn query-v [kb q]
  (println (binding [*kb* kb]
             (sparql-select-query q)))
  (query kb q))

;; HTML data
(defn find-time-stamp
  ([module-content]
   (second
     (map html/text
          (html/select module-content [:span.ratesTimestamp])))))

(def time-stamp-format (formatter "MMM dd, yyyy HH:mm 'UTC'"))

(defn normalize-date
  ([date-time]
   (unparse (formatters :date-time) (parse time-stamp-format date-time))))

(defn find-data
  ([module-content]
   (html/select module-content
                [:table.tablesorter.ratesTable :tbody :tr])))

(defn td->code
  ([td]
   (let [code (-> td
                (html/select [:a])
                first
                :attrs
                :href
                (string/split #"=")
                last)]
     (symbol "currency" (str code "#" code)))))

(defn get-td-a
  ([td]
   (->> td
     :content
     (mapcat :content)
     string/join
     read-string)))

(defn get-data
  ([row]
   (let [[td-header td-to td-from] (filter map? (:content row))]
     {:currency (td->code td-to)
      :exchange-to (get-td-a td-to)
      :exchange-from (get-td-a td-from)})))

(defn data->statements
  ([time-stamp data]
   (let [{:keys [currency exchange-to]} data]
     (list [currency 'err/exchangeRate exchange-to]
           [currency 'err/exchangeWith 'currency/USD#USD]
           [currency 'err/exchangeRateDate [time-stamp 'xsd/dateTime]]))))

(defn load-exchange-data
  "This downloads the HTML page and pulls the data out of it."
  [kb html-url]
  (let [html (html/html-resource html-url)
        div (html/select html [:div.moduleContent])
        time-stamp (normalize-date (find-time-stamp div))]
    (add-statements
      kb
      (mapcat (partial data->statements time-stamp)
              (map get-data (find-data div))))))

;; Aggregate the two
(defn aggregate-data
  "This controls the process and returns the aggregated data."
  [kb data-file data-url q col-map]
  (load-rdf-file kb (File. data-file))
  (load-exchange-data kb (URL. data-url))
  (to-dataset (map (partial rekey col-map) (query kb q))))


(comment
(def t-store (init-kb (kb-memstore)))

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
(def d
  (aggregate-data t-store "data/currencies.ttl" 
         "http://www.x-rates.com/table/?from=USD&amount=1.00"
         q col-map))

)
