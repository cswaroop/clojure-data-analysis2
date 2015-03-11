
(ns com.ericrochester.text-data.bayes
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [cc.mallet.util.*]
           [cc.mallet.types InstanceList]
           [cc.mallet.pipe
            Input2CharSequence TokenSequenceLowercase
            CharSequence2TokenSequence SerialPipes
            SaveDataInSource Target2Label
            TokenSequence2FeatureSequence
            TokenSequenceRemoveStopwords
            FeatureSequence2AugmentableFeatureVector]
           [cc.mallet.pipe.iterator FileIterator]
           [cc.mallet.classify NaiveBayesTrainer]))

#_
(require '[clojure.java.io :as io])
#_
(import [cc.mallet.util.*]
        [cc.mallet.types InstanceList]
        [cc.mallet.pipe
         Input2CharSequence TokenSequenceLowercase
         CharSequence2TokenSequence SerialPipes
         SaveDataInSource Target2Label
         TokenSequence2FeatureSequence
         TokenSequenceRemoveStopwords
         FeatureSequence2AugmentableFeatureVector]
        [cc.mallet.pipe.iterator FileIterator]
        [cc.mallet.classify NaiveBayesTrainer])

(defn make-pipe-list []
  (SerialPipes.
    [(Target2Label.)
     (SaveDataInSource.)
     (Input2CharSequence. "UTF-8")
     (CharSequence2TokenSequence. #"\p{L}[\p{L}\p{P}]+\p{L}")
     (TokenSequenceLowercase.)
     (TokenSequenceRemoveStopwords.)
     (TokenSequence2FeatureSequence.)
     (FeatureSequence2AugmentableFeatureVector. false)]))

(defn add-input-directory [dir-name pipe]
  (doto (InstanceList. pipe)
    (.addThruPipe
      (FileIterator. (io/file dir-name)
                     #".*/([^/]*?)/\d+\..*$"))))

(defn train [instance-list]
  (.train (NaiveBayesTrainer.) instance-list))
(defn classify [bayes instance-list]
  (.classify bayes instance-list))

(defn validate1 [bayes instance]
  (let [c (.classify bayes instance)
        expected (.. c getInstance getTarget toString)
        actual (.. c getLabeling getBestLabel toString)]
    [expected actual]))

(defn confusion-matrix [classifier instances]
  (frequencies (map #(validate1 classifier %) instances)))

(comment

(reset)
(def pipe (make-pipe-list))
(def instance-list (add-input-directory "email" pipe))
(def bayes (train instance-list))

(def test-list (add-input-directory "test-data" pipe))
(def classes (classify bayes test-list))
(pprint (confusion-matrix bayes test-list))

  )
