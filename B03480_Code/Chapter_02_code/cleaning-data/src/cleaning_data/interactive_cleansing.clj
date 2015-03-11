
(ns cleaning-data.interactive-cleansing
  (:require [clojure.java.io :as io]
            [clojure.data.xml :as xml]
            [clojure.string :as string])
  (:import [java.util.concurrent LinkedBlockingQueue]))

#_
(do
  (require '(clojure.java [io :as io]))
  (require '(clojure.data [xml :as xml]))
  (import '(java.util.concurrent LinkedBlockingQueue))
  )

;; IO
(def input-file "data/092011 Stack Overflow/posts.xml")
(def xml (xml/parse (io/input-stream input-file)))
(def input-reader (agent (seque (:content xml))))

(def output-file "/tmp/output.log")
(def output-q (LinkedBlockingQueue. 10))
(def output-writer (agent (io/writer output-file)))

(def error-file "/tmp/error.log")
(def error-q (LinkedBlockingQueue. 10))
(def error-writer (agent (io/writer error-file)))

(defn qoutput
  "This takes an item of output and adds it to the output queue. This blocks if
  there is no room on the queue."
  ([out]
   (.put output-q out)))

(defn qerror
  "This takes an item of error output and adds it to the error queue. This
  blocks if there is no room on the queue."
  ([err]
   (.put error-q err)))

;; Data Types
(defprotocol Outputable
  (->output [o] "Sends something to an output sink."))

(extend java.lang.Object
  Outputable
  {:->output qoutput})

(defrecord ErrorInfo [input-record output-record pair-name exc]
  Outputable
  (->output [error-info]
    (qerror (str "ERROR:" pair-name \: input-record \newline
                 '-> output-record \newline
                 exc \newline))))

(defrecord FnInfo [performed-at action process-fn validation-fn])
(defrecord FnPair [pair-name process-fn validation-fn])

;; State
(def processing-fns (ref []))
(def fn-log (ref []))

(def review-good (ref []))
(def review-bad (ref []))

(def records-seen (ref 0))

;; IO Functions
(defn process-output-q
  "This processes an output queue, writing anything that comes into it to the
  writer.

  If this seems `:eos`, it stops processing. Otherwise, it `sends` itself to
  the writer.

  This returns the writer."
  ([writer queue]
   (let [output (.take queue)]
     (if (= output :eos)
       (.close writer)
       (do 
         (.write writer (str output))
         (.newLine writer)
         (.flush writer)
         (send-off *agent* process-output-q queue)
         writer)))))

;; Functions to work with the review queue.
(defn get-errors
  ([]
   @review-bad))

(defn clear-errors
  ([]
   (dosync
     (ref-set review-bad []))))

(defn concat-input
  "This inserts something at the beginning of the input."
  ([coll prefix]
   (concat prefix coll)))

(defn requeue-errors
  ([]
   (dosync
     (let [errors (map :input-record @review-bad)]
       (send input-reader concat-input errors)
       (clear-errors)))))

(defn get-to-review
  ([]
   (dosync
     (let [to-review @review-good]
       (ref-set review-good [])
       to-review))))

;; Functions to work with the processing functions.
(defn clear-fns
  "This clears the current set of processing functions."
  ([]
   (dosync
     (alter fn-log #(conj % (FnInfo. @records-seen 'clear-fns nil nil)))
     (ref-set processing-fns []))))

(defn add-fn-vars
  "This takes a var pointing to the processing fn and one referencing a
  validation function and adds those functions to the list."
  ([pair-name proc-var valid-var]
   (dosync
     (alter fn-log
            #(conj % (FnInfo. @records-seen
                              ['add-fn-vars pair-name]
                              (meta proc-var)
                              (meta valid-var))))
     (alter processing-fns
            #(conj % (FnPair. pair-name
                              (var-get proc-var)
                              (var-get valid-var))))
     (requeue-errors))))

(defmacro add-fns
  "This takes the names of two functions and inserts them into the processing
  function queue."
  ([pair-name proc-fn valid-fn]
   `(add-fn-vars ~pair-name (var ~proc-fn) (var ~valid-fn))))

(defn rm-fns
  "This removes the indicated processing functions."
  ([pair-name]
   (let [filter-fns (fn [fns]
                      (filter #(not= pair-name (:pair-name %)) fns))]
     (dosync
       (alter fn-log
              #(conj % (FnInfo. @records-seen
                                '[rm-fn-vars pair-name]
                                nil
                                nil)))
       (alter processing-fns filter-fns)
       (requeue-errors)))))

;; Functions to work with the processing pipeline.
;;
;; This is messy. Poor separation of concerns. The code to process an item
;; knows how to handle failures. Need to let them flow downstream.
(defn process-exc
  "This processes an item that threws an exception. This should be called in a
  transaction."
  ([exc input fn-pair-name]
   (qerror (str "ERROR:" fn-pair-name \: input \newline exc))
   (commute review-bad #(conj % (ErrorInfo. input nil fn-pair-name exc)))))

(defn process-fail
  "This processes an item that fails validation. This should be called in a
  transaction."
  ([input output fn-pair-name]
   (commute review-bad #(conj % (ErrorInfo. input output fn-pair-name nil)))))

(defn proc-pair
  ([inp fn-pair]
   (if (instance? ErrorInfo inp)
     inp
     (try
       (let [inp-next ((:process-fn fn-pair) inp)]
         (if ((:validation-fn fn-pair) inp-next)
           inp-next
           (ErrorInfo. inp inp-next (:pair-name fn-pair) nil)))
       (catch Exception exc
         (ErrorInfo. inp nil (:pair-name fn-pair) exc))))))

(defn process-input
  "This processes one input item."
  ([input]
   ; (print '>>> input)
   (reduce proc-pair input @processing-fns)))

(defn process-seq
  "This is the agent function. It processes one item of input from the sequence
  and re-sends itself."
  ([coll]
   (when-let [[current & coll-rest] coll]
     (dosync
       (->output (process-input current))
       (commute records-seen inc))
     (send-off *agent* process-seq)
     coll-rest)))

(defn start
  ([]
   (send-off input-reader process-seq)
   (send-off output-writer process-output-q output-q)
   (send-off error-writer process-output-q error-q)))

(start)
(await input-reader
       output-writer
       error-writer)


