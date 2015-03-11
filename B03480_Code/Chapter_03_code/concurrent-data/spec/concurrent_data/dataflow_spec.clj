
(ns concurrent-data.dataflow-spec
  (:require [clojure.core.reducers :as r])
  (:use [speclj.core]
        [concurrent-data.spec-utils]))

#_
(require '[clojure.core.reducers :as r])

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

(defn accum-mean
  ([] {:sum 0, :n 0})
  ([{:keys [sum n]} x]
   {:sum (+ sum x)
    :n (inc n)}))

(defn join-accum
  ([] {:sum 0, :n 0})
  ([accum1 accum2]
   {:sum (+ (:sum accum1) (:sum accum2))
    :n (+ (:n accum1) (:n accum2))}))

(defn calc-mean
  ([{:keys [sum n]}] (double (/ sum n))))

(defn process-seq
  ([coll]
   (->>
     coll
     (r/map :surname)
     (r/filter #(not (nil? %)))
     (r/map count))))

(describe
  "clojure.core.reducers"
  (it "should process the data sequentially with reduce."
      (should (approx= 0.001 6.527778
                       (calc-mean (reduce accum-mean (accum-mean) (process-seq input-data)))
                       )))
  (it "should process the data in parallel with r/fold."
      (should (approx= 0.001 6.527778
                       (calc-mean (r/fold join-accum accum-mean (process-seq input-data)))
                       )))
  )

(run-specs)

