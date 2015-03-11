
(ns com.ericrochester.text-data.tokenizing
  (:require [opennlp.nlp :as nlp]
            [clojure.java.io :as io]))

#_
(require '[opennlp.nlp :as nlp]
         '[clojure.java.io :as io])

(def tokenize (nlp/make-tokenizer "models/en-token.bin"))
(def get-sentences
  (nlp/make-sentence-detector "models/en-sent.bin"))

(defn load-stopwords [filename]
  (with-open [r (io/reader filename)]
    (set (doall (line-seq r)))))

(def is-stopword (load-stopwords "stopwords/english"))

(defn normalize [token-seq]
  (map #(.toLowerCase %) token-seq))

(defn scale-by-total [freqs]
  (let [total (reduce + 0 (vals freqs))]
    (->> freqs
         (map #(vector (first %) (/ (second %) total)))
         (into {}))))

(comment

(tokenize "This is a string.")
(get-sentences "I never saw a Purple Cow.
    I never hope to see one.
    But I can tell you, anyhow.
    I'd rather see than be one.")

(def tokens
  (map #(remove is-stopword (normalize (tokenize %)))
       (get-sentences
         "I never saw a Purple Cow.
         I never hope to see one.
         But I can tell you, anyhow.
         I'd rather see than be one.")))
(pprint tokens)

(def token-freqs
  (apply merge-with + (map frequencies tokens)))
(pprint token-freqs)
(pprint (scale-by-total token-freqs))

(-> (str "Dr Brown's random number algorithm is based "
         "on the baffling floor seqeuences chosen by "
         "the Uni library elevator.")
    tokenize
    normalize
    frequencies
    scale-by-total
    (get "random")
    float)

)
