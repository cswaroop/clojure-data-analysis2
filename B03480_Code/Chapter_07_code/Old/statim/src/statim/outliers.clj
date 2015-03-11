
;;;; 07.01. Finding Outliers
;;;; See
;;;; * http://en.wikipedia.org/wiki/Chauvenet%27s_criterion (see especially the
;;;;   section on criticisms)
;;;; * http://en.wikipedia.org/wiki/Normal_distribution
;;;; * http://en.wikipedia.org/wiki/Error_function

(ns statim.outliers
  (:require [incanter.distributions :as d])
  (:use (incanter core stats)))

#_
(do
(use '(incanter core stats))
(require '[incanter.distributions :as d])
  )

;;; This finds the z-score for x, given mean m and standard
;;; deviation s.
;;;
;;; This statistic assumes a normal distribution."
(defn z-score ([x m s] (/ (- x m) s)))

;;; This is an approximation of the error function taken from
;;; http://en.wikipedia.org/wiki/Error_function.
(defn err-fn
  ([x] (let [d (+ 1
                  (* 0.278393 x)
                  (* 0.230289 (Math/pow x 2))
                  (* 0.000972 (Math/pow x 3))
                  (* 0.078108 (Math/pow x 4)))]
         (- 1 (/ 1 (Math/pow d 4))))))

;;; This gives the odds that a z-score occurred naturally.
(defn p ([z] (err-fn (/ z (Math/sqrt 2)))))

;;; This uses Chauvenet's criterion
;;; (http://en.wikipedia.org/wiki/Chauvenet%27s_criterion) to
;;; determine if a value is an outlier.
;;;
;;; This assumes a normal distribution and looks at the probability
;;; that the item would occur naturally (calculated from the
;;; z-score), relatively to the size of the sample collection. If
;;; the probability multiplied by the size of the sample is less
;;; than 1/2, then the item is an outlier."
(defn is-outlier?
  ([mean sd count] (partial is-outlier? mean sd count))
  ([mean sd count x]
   (< (* count (- 1 (p (z-score x mean sd)))) 0.5)))

;;; This removes outliers from an input collection using Chauvenet's
;;; criterion
;;; (http://en.wikipedia.org/wiki/Chauvenet%27s_criterion).
(defn rm-chauvenet-outlier
  ([xs]
   (filter (complement (is-outlier? (mean xs) (sd xs) (count xs)))
           xs)))

