(ns interop.r.eval-file
  (:use interop.r.core)
  (:import [java.io File]))

(comment
(use 'interop.r.core)
(import '[java.io File])
  )

(defn r-source
  ([filename] (r-source filename *r-cxn*))
  ([filename r-cxn]
   (.eval r-cxn (str "source(\""
                     (.getAbsolutePath (File. filename))
                     "\")"))))

;;; This monstrosity is to delve down into the result and get the
;;; actual value. For some reason getKey and getValue appear to
;;; perform the opposite functions. WTH.

(..
  (r-source "r-norm.R")
  asList
  (at 0)
  asDouble)

(def x-2 (r-source "chisqr-example.R"))
(def x-sqr (.asList (r-source "chisqr-example.R")))

;; X2
(.. x-sqr (at 0) asList (at 0) asDouble)

;; df
(.. x-sqr (at 0) asList (at 1) asInteger)

;; p-value
(.. x-sqr (at 0) asList (at 2) asDouble)


