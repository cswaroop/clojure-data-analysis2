
(ns cleaning-data.rescaling-spec
  (:use [speclj.core]
        [cleaning-data.rescaling]))

(def word-counts
  [{:word 'the, :freq 92, :total-words 250, :doc 'a}
   {:word 'a, :freq 76, :total-words 250, :doc 'a}
   {:word 'jack, :freq 4, :total-words 250, :doc 'a}
   {:word 'the, :freq 3, :total-words 10, :doc 'b}
   {:word 'a, :freq 2, :total-words 10, :doc 'b}
   {:word 'mary, :freq 1, :total-words 10, :doc 'b}])

(def groupless-counts (map #(dissoc % :total-words) word-counts))

(describe
  "rescale-by-total"
  (it "should rescale the values of :freq by the total of :freq."
      (let [total (reduce + (map :freq word-counts))]
        (should= (list (/ 92 total) (/ 76 total) (/ 4 total) (/ 3 total)
                       (/ 2 total) (/ 1 total))
                 (map :scaled-freq (rescale-by-total :freq :scaled-freq
                                                     groupless-counts))))))

(describe
  "rescale-by-group"
  (it "should rescale the values of :freq by the total for the document."
      (should= (list (/ 92 (+ 92 76 4)) (/ 76 (+ 92 76 4)) (/ 4 (+ 92 76 4))
                     (/ 3 (+ 3 2 1)) (/ 2 (+ 3 2 1)) (/ 1 (+ 3 2 1)))
               (map :scaled-freq (rescale-by-group :freq :doc :scaled-freq
                                                   groupless-counts)))))

(describe
  "rescale-by-key"
  (it "should rescale the values of :freq by :total-words."
      (should= '(92/250 76/250 4/250 3/10 2/10 1/10)
               (map :scaled-freq (rescale-by-key :freq :total-words :scaled-freq
                                                 word-counts)))))

(run-specs)


