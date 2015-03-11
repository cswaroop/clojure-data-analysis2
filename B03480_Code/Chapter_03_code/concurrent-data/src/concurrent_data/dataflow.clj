
(ns concurrent-data.dataflow
  (:refer-clojure :exclude [map filter reduce])
  (:import [java.util.concurrent LinkedBlockingQueue]))

;;; do-action: 
;;; "This calls the wrapped operation on the input and
;;; returns a list of output data to pass to the next
;;; node in the graph.
;;;
;;; This should return a vector. The first item is the
;;; node, in case there are changes, and the second is
;;; the output data stream."
;;;
;;; close-node:
;;; "This lets the node know that the input stream is
;;; empty, so it can perform any clean up and return
;;; a sequence of final output data.
;;;
;;; Like `do-step`, this should return a vector pair,
;;; although `nil` is fine for this function also."
;;;
;;; launch-node:
;;; "This does any last-minute initialization and constructs
;;; the agent and launches it on the first step."
;;;
;;; step-node:
;;; "This pulls one piece of datum from the input queue and
;;; executes the node on it. This does not implement the
;;; actual node's action, but instead it gets the input, calls
;;; the node's function on it by calling `do-action`, and then
;;; takes the output and pushes it to the next node."
;;;
;;; update-node:
;;; "This returns the latest copy of the node. If processing
;;; has happened, the agent may contain the latest state for
;;; the node. This returns that."
(defprotocol Flowable
  (do-action [node datum])
  (close-node [node])
  (launch-node [node])
  (step-node [node])
  (update-node [node]))

(def EOS (Object.))

(defn- finish-node
  ([n q]
   (let [output (close-node n)
         [n1 out-seq] (cond (nil? output) [n '()]
                            :else output)]
     (doseq [out (seq out-seq)]
       (.put q out))
     (.put q EOS)
     n1)))

(defn- continue-node
  ([n s q]
   (let [[n-1 output-seq] (do-action n s)]
     (doseq [out (seq output-seq)]
       (.put q out))
     (send *agent* step-node)
     n-1)))

(defn- def-update-node
  ([n]
   (if-let [a (:flow-agent n)]
     @a
     n)))

(def default-flowable
  {:launch-node (fn [n]
                  (let [a (agent nil)
                        next-n (assoc n :flow-agent a)]
                    (send a (fn [_] n))
                    (send a step-node)
                    next-n))
   :step-node (fn [n]
                (let [s (.take (:in-q n))]
                  (if (identical? s EOS)
                    (finish-node n (:out-q n))
                    (continue-node n s (:out-q n)))))
   :do-action (fn [n d])
   :close-node (fn [n])
   :update-node def-update-node})

(defrecord MapNode
  [flow-fn flow-agent in-q out-q])

(extend MapNode
  Flowable
  (assoc default-flowable
         :do-action (fn [n d]
                      [n (list ((:flow-fn n) d))])))

(defn map
  ([f]
   (MapNode. f nil nil nil)))

(defrecord FilterNode
  [flow-fn flow-agent in-q out-q])

(extend FilterNode
  Flowable
  (assoc default-flowable
         :do-action (fn [n d]
                      (if ((:flow-fn n) d)
                        (list d)
                        '()))))

(defn filter
  ([f]
   (FilterNode. f nil nil nil)))

(defrecord ReduceNode
  [flow-fn flow-state flow-agent in-q out-q])

(extend ReduceNode
  Flowable
  (assoc default-flowable
         :do-action
         (fn [n d]
           [(assoc n
                   :flow-state
                   ((:flow-fn n) (:flow-state n) d))
            '()])
         :close-node (fn [n]
                       [(assoc n :flow-state nil)
                        (list (:flow-state n))])))

(defn reduce
  ([f s]
   (ReduceNode. f s nil nil nil)))

(defrecord SeqNode
  [flow-nodes])

(extend SeqNode
  Flowable
  {:launch-node
   (fn [n]
     (assoc n :flow-nodes
            (vec (mapv launch-node (:flow-nodes n)))))
   :step-node (fn [n]
                (step-node (first (:flow-nodes n))))
   :close-node (fn [n]
                 (close-node (first (:flow-nodes n))))
   :do-action (fn [n d])
   :update-node (fn [n]
                  (->>
                    n
                    :flow-nodes
                    (clojure.core/mapv update-node)
                    (assoc n :flow-nodes)))})

(defn- connect-seqs
  ([x y q]
   (let [xnodes (:flow-nodes x)
         ynodes (:flow-nodes y)
         xlindex (dec (count xnodes))
         xlast (nth xnodes xlindex)
         xynodes (vec (concat (assoc-in xnodes [xlindex :out-q] q)
                              [(assoc (first ynodes) :in-q q)]
                              (rest ynodes)))]
     (assoc x :flow-nodes xynodes))))

(defn- connect-seq-to
  ([x y q]
   (let [xnodes (:flow-nodes x)
         xlindex (dec (count xnodes))
         xlast (nth xnodes xlindex)
         xynodes (conj (assoc-in xnodes [xlindex :out-q] q)
                       (assoc y :in-q q))]
     (assoc x :flow-nodes xynodes))))

(defn- connect-to-seq
  ([x y q]
   (let [ynodes (:flow-nodes x)
         xynodes (vec (concat [(assoc x :out-q q)
                               (assoc (first ynodes) :in-q q)]
                              (rest ynodes)))]
     (assoc y :flow-nodes xynodes))))

(defn- connect-nodes
  ([x y q]
   (SeqNode. [(assoc x :out-q q) (assoc y :in-q q)])))

(defn =>
  ([] (map identity))
  ([x] x)
  ([x y & xs]
   (let [q (LinkedBlockingQueue. 100)]
     (apply =>
            (cond
              (and (instance? SeqNode x)
                   (instance? SeqNode y)) (connect-seqs x y q)
              (instance? SeqNode x) (connect-seq-to x y q)
              (instance? SeqNode y) (connect-to-seq x y q)
              :else (connect-nodes x y q))
            xs))))

(defrecord InputNode
  [coll flow-agent out-q])

(extend InputNode
  Flowable
  {:close-node (fn [n])
   :launch-node (fn [n]
                  (let [a (agent nil)
                        n1 (assoc n :flow-agent a)]
                    (send a (fn [_] n1))
                    (send a step-node)
                    n1))
   :step-node (fn [n]
                (if-let [[x & xs] (seq (:coll n))]
                  (do
                    (.put (:out-q n) x)
                    (send *agent* step-node)
                    (assoc n :coll xs))
                  (do
                    (.put (:out-q n) EOS)
                    (assoc n :coll '()))))
   :update-node def-update-node})

(defn input
  ([ins]
   (InputNode. ins nil nil)))

(defn flow
  ([df-graph]
   (launch-node df-graph)))

(defn wait
  ([df-graph]
   (if (instance? SeqNode df-graph)
     (wait (last (:flow-nodes df-graph)))
     (await (:flow-agent df-graph)))
   (update-node df-graph)))

(comment

(require '[concurrent-data.dataflow :as df])
(require '[clojure.pprint :as pp])
(def graph (df/=> (df/input df/input-data) (df/map identity) (df/reduce conj [])))
(def flown (df/flow graph))
(def done (df/wait flown))

(defn map!
  ([f]))

(defn mapcat
  ([f]))

(defn mapcat!
  ([f]))

(defn reduce-close!
  ([f-step init f-close]))

(defn reduce-seq
  ([f init]))

(defn spread
  ([f-src])
  ([f-src f])
  ([f-src f & fs]))

(defn to-matrix
  ([]))
  )

(def input-data
  [{:given-name "Susan", :surname "Forman", :doctors [1]}
   {:given-name "Barbara", :surname "Wright", :doctors [1]}
   {:given-name "Ian", :surname "Chesterton", :doctors [1]}
   {:given-name "Vicki", :surname nil, :doctors [1]}
   {:given-name "Steven", :surname "Taylor", :doctors [1]}
   {:given-name "Katarina", :surname nil, :doctors [1]}
   {:given-name "Sara", :surname "Kingdom", :doctors [1]}
   {:given-name "Dodo", :surname "Chaplet", :doctors [1]}
   {:given-name "Polly", :surname nil, :doctors [1 2]}
   {:given-name "Ben", :surname "Jackson", :doctors [1 2]}
   {:given-name "Jamie", :surname "McCrimmon", :doctors [2]}
   {:given-name "Victoria", :surname "Waterfield", :doctors [2]}
   {:given-name "Zoe", :surname "Heriot", :doctors [2]}
   {:given-name nil, :surname "Lethbridge-Stewart", :doctors [2]}
   {:given-name "Liz", :surname "Shaw", :doctors [3]}
   {:given-name "Jo", :surname "Grant", :doctors [3]}
   {:given-name "Sarah Jane", :surname "Smith", :doctors [3 4 10]}
   {:given-name "Harry", :surname "Sullivan", :doctors [4]}
   {:given-name "Leela", :surname nil, :doctors [4]}
   {:given-name "K-9 Mark I", :surname nil, :doctors [4]}
   {:given-name "K-9 Mark II", :surname nil, :doctors [4]}
   {:given-name "Romana", :surname nil, :doctors [4]}
   {:given-name "Adric", :surname nil, :doctors [4 5]}
   {:given-name "Nyssa", :surname nil, :doctors [4 5]}
   {:given-name "Tegan", :surname "Jovanka", :doctors [4 5]}
   {:given-name "Vislor", :surname "Turlough", :doctors [5]}
   {:given-name "Kamelion", :surname nil, :doctors [5]}
   {:given-name "Peri", :surname "Brown", :doctors [5 6]}
   {:given-name "Melanie", :surname "Bush", :doctors [6 7]}
   {:given-name "Ace", :surname nil, :doctors [7]}
   {:given-name "Grace", :surname "Holloway", :doctors [8]}
   {:given-name "Rose", :surname "Tyler", :doctors [9 10]}
   {:given-name "Adam", :surname "Mitchell", :doctors [9]}
   {:given-name "Jack", :surname "Harkness", :doctors [9 10]}
   {:given-name "Mickey", :surname "Smith", :doctors [10]}
   {:given-name "Donna", :surname "Noble", :doctors [10]}
   {:given-name "Martha", :surname "Jones", :doctors [10]}
   {:given-name "Astrid", :surname "Peth", :doctors [10]}
   {:given-name "Jackson", :surname "Lake", :doctors [10]}
   {:given-name "Rosita", :surname "Farisi", :doctors [10]}
   {:given-name "Christina", :surname "de Souza", :doctors [10]}
   {:given-name "Adelaide", :surname "Brooke", :doctors [10]}
   {:given-name "Wilfred", :surname "Mott", :doctors [10]}
   {:given-name "Amy", :surname "Pond", :doctors [11]}
   {:given-name "Rory", :surname "Williams", :doctors [11]}
   {:given-name "River", :surname "Song", :doctors [11]}
   {:given-name "Craig", :surname "Owens", :doctors [11]}])


(comment
(defn ->csv
  ([file-name]
   (df/reduce-close!
     (fn [fout row]
       (csv/write-csv fout [row])
       fout)
     (io/writer file-name)
     #(.close fout))))

(defn invert-index
  ([index [file-path token]]
   (assoc index token (conj (get index token []) file-path))))

(def file-graph
  (df/=>
    (df/map #(File. %))
    (df/mapcat! file-seq)
    (df/filter #(.isFile %))))

(def freq-graph
  (df/=>
    (df/map second)
    (df/reduce-seq (fn [freqs x]
                     (assoc freqs x (inc (get freqs x 0))))
                   {})
    df/to-matrix
    (->csv "frequencies.csv")))

(def index-graph
  (df/reduce invert-index {}))

(def index-output-graph
  (df/=> to-matrix (->csv "index.csv")))

(def search-graph
  (df/=>
    (df/map #(vector %1 (get %2 %1 [])) (df/input (list :committee)))
    (->csv "committee-hits.csv")))

(def from-index-graph
  (df/spread index-graph index-output-graph search-graph))

(def token-graph
  (df/=>
    (df/map! #(vector % (io/reader %)))
    (df/mapcat! (fn [[file-name fin]]
                 (map #(vector file-name %)
                      (line-seq fin))))
    (df/mapcat (fn [[file-name line]]
                 (map #(vector file-name %)
                      (tokenize-brown line))))))

(def from-token-graph
  (df/spread token-graph freq-graph index-graph))

(def data-flow
  (df/=> file-graph from-token-graph))

(df/flow (df/=> (df/input '("./data/brown/")) data-flow))
  )

