
(ns interop.mma.par
  (:use interop.mma.core)
  (:use clojuratica)
  (:import [com.wolfram.jlink MathLinkFactory]))

(comment
(use 'interop.mma.core)
(use 'clojuratica)
  )

(comment
  ;; TODO: What does this do? I need to check when it's necessary.
(math (LaunchKernels))
(math (ParallelMap #'(Plus % 1) [1 2 3 4 5])
(math (ParallelMap Log [1 2 3 4 5]))

;; Each task has to be kick off from a different thread to get parallel.
;; And we have to mark the task with :parallel.
;; (See http://clojuratica.weebly.com/tutorial.html)
;; TODO: I'm not sure this is working exactly right. Investigate later. :/
(let [f (math :parallel (Function [x] (Fibonacci x) $KernelID))
      agents (map (fn [_] (agent nil)) (range 10))]
  (dorun (map #(send-off % f) agents))
  (dorun (map await agents))
  (map deref agents))
  )

