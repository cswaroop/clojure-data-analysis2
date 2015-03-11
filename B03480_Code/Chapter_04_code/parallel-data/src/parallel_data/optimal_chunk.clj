
;; It may be better to move this later in the chapter and do it with the chunk
;; size for reducers.
;; 
;; Also, I should play with the functions and parameters to make it less likely
;; to move to a bad position
;; (http://en.wikipedia.org/wiki/Simulated_annealing).
;;
;; Use complex numbers
;; (http://commons.apache.org/math/apidocs/org/apache/commons/math/complex/Complex.html
;; and http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22commons-math3%22).

(ns parallel-data.optimal-chunk
  (:use [parallel-data.pmap-chunk :only (mandelbrot-part mc-pi-part)]
        [parallel-data.utils :only (raw-time)]
        criterium.core))

(comment
  (import [java.lang Math])
  (use 'criterium.core)
  (use '[parallel-data.pmap-chunk :only (mc-pi-part)])
  )

;; Can I make this sequence based?
(defn annealing
  [initial max-iter max-cost neighbor-fn cost-fn p-fn temp-fn]
  (let [get-cost (memoize cost-fn)
        cost (get-cost initial)]
    (loop [state initial
           cost cost
           k 1
           best-seq [{:state state, :cost cost}]]
      (println '>>> 'sa k \. state \$ cost)
      (if (and (< k max-iter)
               (or (nil? max-cost) (> cost max-cost)))
        (let [t (temp-fn (/ k max-iter))
              next-state (neighbor-fn state)
              next-cost (get-cost next-state)
              next-place {:state next-state, :cost next-cost}]
          (if (> (p-fn cost next-cost t) (rand))
            (recur next-state next-cost (inc k)
                   (conj best-seq next-place))
            (recur state cost (inc k) best-seq)))
        best-seq))))

(defn get-neighbor [state]
  (max 0 (min 20 (+ state (- (rand-int 11) 5)))))

(defn get-pi-cost [n state]
  (let [chunk-size (long (Math/pow 2 state))]
    (first (:mean (quick-benchmark
                    (mc-pi-part chunk-size n)
                    {})))))

(defn get-temp [r] (- 1.0 (float r)))

(defn should-move [c0 c1 t]
  (* t (if (< c0 c1) 0.25 1.0)))

#_
(annealing 12 10 nil get-neighbor
           (partial get-pi-cost 1000000) should-move get-temp)

