
(ns com.ericrochester.text-data.topic-modeling
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [cc.mallet.util.*]
           [cc.mallet.types InstanceList]
           [cc.mallet.pipe
            Input2CharSequence TokenSequenceLowercase
            CharSequence2TokenSequence SerialPipes
            TokenSequenceRemoveStopwords
            TokenSequence2FeatureSequence]
           [cc.mallet.pipe.iterator FileListIterator]
           [cc.mallet.topics ParallelTopicModel]
           [java.io FileFilter]))

#_
(require '[clojure.java.io :as io])
#_
(import [cc.mallet.util.*]
        [cc.mallet.types InstanceList]
        [cc.mallet.pipe
         Input2CharSequence TokenSequenceLowercase
         CharSequence2TokenSequence SerialPipes
         TokenSequenceRemoveStopwords
         TokenSequence2FeatureSequence]
        [cc.mallet.pipe.iterator FileListIterator]
        [cc.mallet.topics ParallelTopicModel]
        [java.io FileFilter])

(defn make-pipe-list []
  (InstanceList.
    (SerialPipes.
      [(Input2CharSequence. "UTF-8")
       (CharSequence2TokenSequence.
         #"\p{L}[\p{L}\p{P}]+\p{L}")
       (TokenSequenceLowercase.)
       (TokenSequenceRemoveStopwords. false false)
       (TokenSequence2FeatureSequence.)])))

(defn add-directory-files [instance-list corpus-dir]
  (.addThruPipe
    instance-list
    (FileListIterator.
      (.listFiles (io/file corpus-dir))
      (reify FileFilter
        (accept [this pathname] true))
      #"/([^/]*).txt$"
      true)))

(defn train-model
  ([instances] (train-model 100 4 50 instances))
  ([num-topics num-threads num-iterations instances]
   (doto (ParallelTopicModel. num-topics 1.0 0.01)
     (.addInstances instances)
     (.setNumThreads num-threads)
     (.setNumIterations num-iterations)
     (.estimate))))

(defn make-topic-info
  "Creates a map of information about the instance and topic.
  This should pull out everything needed for the basic
  visualizations."
  ([model n topic-n]
   (make-topic-info model (.getTopicProbabilities model n)
                    n topic-n))
  ([model topic-dist n topic-n]
   (let [path (.. model getData (get n) instance
                  getName getPath)]
     {:instance-n n
      :path (str/replace path #".*/([^/]*).txt" "$1")
      :topic-n topic-n
      :distribution (aget topic-dist topic-n)})))

(comment

(def pipe-list (make-pipe-list))
(add-directory-files pipe-list "sotu/")
(def tm (train-model 10 4 50 pipe-list))
(def tis (map #(make-topic-info tm % 3) (range (.size pipe-list))))

  )
