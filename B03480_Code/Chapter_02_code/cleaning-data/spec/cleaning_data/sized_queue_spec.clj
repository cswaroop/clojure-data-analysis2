
(ns cleaning-data.sized-queue-spec
  (:use [speclj.core]))

(def seen (agent 0))

(defn reset-seen
  ([]
   (send seen (constantly 0))))

(defn inc-seen
  ([]
   (send seen inc)))

(defn next-value
  ([]
   (await (inc-seen))
   @seen))

(def count-seq (repeatedly next-value))
(def size-queue (seque 5 count-seq))

(defn create-sized-queue
  ([] (create-sized-queue 5))
  ([n]
   (let [seen (agent 0)
         next-value (fn [x]
                      (send seen inc)
                      x)
         size-queue (seque n (map next-value (range 10000)))]
     [seen size-queue])))

(def buffer-size 25)

(describe
  "seque"
  (it "should not take more than 5+buffer values immediately."
      (let [[seen q] (create-sized-queue)]
        (should (>= (+ 5 25) @seen))))
  (it "should consume a new value once one is taken off the front."
      (let [[seen q] (create-sized-queue)
            last-seen @seen]
        (should= [0] (take 1 q))
        (should (and (< last-seen @seen) (>= (+ 1 5 buffer-size) @seen)))))
  (it "should consume 5 new values once five are taken off the front."
      (let [[seen q] (create-sized-queue)
            last-seen @seen]
        (should= [0 1 2 3 4] (take 5 q))
        (should (and (< last-seen @seen) (>= (+ 5 5 buffer-size) @seen))))))

(run-specs)

