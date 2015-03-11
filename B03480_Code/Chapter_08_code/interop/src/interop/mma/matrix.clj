
;;;; 08.02. Copy over matrixes.
(ns interop.mma.matrix
  (:require '[incanter.core :as i]
            'incanter.io)
  (:use clojuratica
        interop.mma.core))

#_
(do
(use 'clojuratica
     'interop.mma.core)
(require '[incanter.core :as i]
         'incanter.io)
(def data-file "data/all_160.P3.csv")
(def data (incanter.io/read-dataset data-file :header true))
  )

;;; NB: math is a macro, and you have to interpolate values using ~.
(defn mma-mean [dataset col]
  (math (Mean ~(i/sel dataset :cols col))))
(defn mma-median [dataset col]
  (math (Median ~(i/sel dataset :cols col))))

#_
(mma-mean data :POP100)
;; 230766493/29439
#_
(mma-median data :POP100)
;; 1081

