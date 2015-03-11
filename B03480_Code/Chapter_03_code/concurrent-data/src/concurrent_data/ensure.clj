
(ns concurrent-data.ensure
  (:require [clojure.string :as string])
  (:import [java.io File]))

#_
(do
  (require '[clojure.string :as string])
  (import '[java.io File]))

;; One ref   -- total docs
;; One ref   -- total words
;; One ref   -- frequenies (ref (hash-map keywork int))
;; One agent -- running report for a term

; Parameters
;; The Brown corpus can be downloaded from
;; http://nltk.googlecode.com/svn/trunk/nltk_data/index.xml
(def input-files
  (filter #(.isFile %) (file-seq (File. "./data/brown"))))

; The processing refs.
(def finished (ref false))
(def total-docs (ref 0))
(def total-words (ref 0))
(def freqs (ref {}))
(def running-report (agent {:term nil
                            :frequency 0
                            :ratio 0.0}))

(defn reset []
  (dosync
    (ref-set finished false)
    (ref-set total-docs 0)
    (ref-set total-words 0)
    (ref-set freqs {})
    (send running-report
          (constantly
            {:term nil
             :frequency 0
             :ratio nil}))))

(defn tokenize-brown [input-str]
  (->> (string/split input-str #"\s+")
       (map #(first (string/split % #"/" 2)))
       (filter #(> (count %) 0))
       (map string/lower-case)
       (map keyword)))

(defn accum-freq [m token]
  (assoc m token (inc (m token 0))))

(defn compute-file [fs]
  (dosync
    (if-let [[s & ss] (seq fs)]
      (let [tokens (tokenize-brown (slurp s))
            tc (count tokens)
            fq (reduce accum-freq {} tokens)]
        (commute total-docs inc)
        (commute total-words #(+ tc %))
        (commute freqs #(merge-with + % fq))
        (send-off *agent* compute-file)
        ss)
      (do (alter finished (constantly true))
          '()))))

(defn compute-report [{term :term, :as report}]
  (dosync
    (when-not @finished
      (send *agent* compute-report))
    (let [term-freq (term (ensure freqs) 0)
          tc (ensure total-words)
          r (if (zero? tc)
              nil
              (float (/ term-freq tc)))]
      (assoc report
             :frequency term-freq
             :ratio r))))

(defn compute-frequencies [inputs term]
  (let [a (agent inputs)]
    (send running-report #(assoc % :term term))
    (send running-report compute-report)
    (send-off a compute-file)))

(defn get-report [term]
  (send running-report #(assoc % :term term))
  (send running-report compute-report)
  (await running-report)
  @running-report)

;; Kick this off with the term :committee, and then run this repeatedly until
;; it's done:
;;
;; [@finished @running-report]

