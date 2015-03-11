
(ns interop.mma.call-fns
  (:use interop.mma.core)
  (:use clojuratica)
  (:import [com.wolfram.jlink MathLinkFactory]))

(comment
(use 'interop.mma.core)
(use 'clojuratica)
  )

;;; Calling a function.
#_
(math (FactorInteger 24))
;;
;; FindRoot[{Exp[x-2] == y, y^2 == x}, {{x, 1}, {y, 1}}]
#_
(math (FindRoot [(== (Exp (- x 2)) y) (== (Power y 2) x)] [[x 1] [y 1]]))
;; [(-> x 0.019026016103714054) (-> y 0.13793482556524314)]
