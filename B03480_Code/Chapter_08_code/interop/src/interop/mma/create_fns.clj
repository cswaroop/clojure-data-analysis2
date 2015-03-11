
(ns interop.mma.create-fns
  (:use interop.mma.core)
  (:use clojuratica)
  (:import [com.wolfram.jlink MathLinkFactory]))

(comment
(use 'interop.mma.core)
(use 'clojuratica)
  )

;;; Creating a function.
(def factor-int
  (math
    (Function [x] (FactorInteger x))))
;; (factor-int 24)


