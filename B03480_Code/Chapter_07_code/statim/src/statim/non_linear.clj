
;;;; For Fitting Non-Linear Models (07.12).
(ns statim.non-linear
  (:require [incanter.core :as i]
            incanter.io
            [incanter.optimize :as o]
            [incanter.stats :as s]
            [incanter.charts :as c])
  (:import [java.lang Math]))

(comment
(require
  '[incanter.core :as i]
  'incanter.io
  '[incanter.optimize :as o]
  '[incanter.stats :as s]
  '[incanter.charts :as c])
(import [java.lang Math])
  )

;;; From *Introduction to Statistics and Applications*, pg 656.
;;;
;;; This is a hypothetical study of traffic flow (in vehicles per
;;; hour) and vehicle speed (in miles per hour). The data was
;;; collected during rush hour.
(def data
  (i/dataset
    [:flow :speed]
    [[1256 35]
     [1329 40]
     [1226 30]
     [1335 45]
     [1349 50]
     [1124 25]]))

(def flow (i/sel data :cols :flow))
(def speed (i/sel data :cols :speed))

(def chart
  (c/scatter-plot speed flow
                  :title "Traffic Flow v Speed"
                  :x-label "Vehicle Speed"
                  :y-label "Traffic Flow"
                  :legend true))
(i/view chart)

(defn f
  ([theta x]
   (let [[b0 b1 b2] theta]
     (i/plus b0 (i/mult b1 x) (i/mult b2 x x)))))

(def start [25.0 60.0 -0.65])
(c/add-lines chart speed (f start speed))
(def nlm (o/non-linear-model f flow speed start))
(c/add-lines chart speed (:fitted nlm))

;; user=> (:coefs nlm)
;; (432.57142848933603 37.42857143288293 -0.38285714291004713)

;;;; Using data from
;;;; http://www-fars.nhtsa.dot.gov/QueryTool/QuerySection/CaseListingReport.aspx
;;;; make sure to get state and speed limit.
(def data-file "data/accident-fatalities.tsv")
(def data
  (incanter.io/read-dataset data-file
                            :header true
                            :delim \tab))
(def fatalities
  (->> data
    (i/$rollup :count :Obs. :spdlim)
    (i/$where {:spdlim {:$ne "."}})
    (i/$where {:spdlim {:$ne 0}})
    (i/$order :spdlim :asc)
    (i/to-list)
    (i/dataset [:speed-limit :fatalities])))

(def speed-limit (i/sel fatalities :cols :speed-limit))
(def fatality-count (i/sel fatalities :cols :fatalities))

(def chart
  (doto
    (c/scatter-plot speed-limit fatality-count
                    :title "Fatalities by Speed Limit (2010)"
                    :x-label "Speed Limit"
                    :y-label "Fatality Count"
                    :legend true)
    i/view))

;;; Normal distribution PDF
;;; TODO: Check Greek letter-names.
(def sqrt-2-pi (Math/sqrt (* 2 Math/PI)))
(defn f
  ([theta x]
   (let [[mu rho] theta]
     (i/mult (i/div 1.0 (i/mult sqrt-2-pi rho))
             (i/pow Math/E (i/minus (i/div (i/pow (i/minus x mu) 2)
                                           (i/mult 2.0 (i/pow rho 2)))))))))

;;; F-distribution PDF
(defn f
  ([theta x]
   (let [[d1 d2 amp] theta]
     (i/mult amp (s/pdf-f x :df1 d1 :df2 d2)))))

(defn f
  ([theta x]
   (let [[a b c d e] theta]
     (i/plus (i/mult a x x x x)
             (i/mult b x x x)
             (i/mult c x x)
             (i/mult d x)
             e))))

(defn f
  ([theta x]
   (let [[b0 b1 b2 b3 x-offset] theta
         x' (i/plus x x-offset)]
     (i/plus (i/mult b0 x' x' x')
             (i/mult b1 x' x')
             (i/mult b2 x')
             b3))))
     ; (i/plus b0 (i/mult b1 x) (i/mult b2 x x)))))
; (c/add-lines chart speed-limit (f [1000.0 1000.0 2500.0] speed-limit))

;;; General sine wave
(defn sine-wave [theta x]
  (let [[amp ang-freq phase shift] theta]
    (i/plus
      (i/mult amp (i/sin (i/plus (i/mult ang-freq x)
                                 phase)))
      shift)))

(def start [3500.0 0.07 Math/PI 2500.0])
(def nlm (o/non-linear-model sine-wave fatality-count speed-limit start))

(def chart
  (doto
    (c/scatter-plot speed-limit fatality-count
                    :title "Fatalities by Speed Limit (2010)"
                    :x-label "Speed Limit"
                    :y-label "Fatality Count"
                    :legend true)
    (c/add-lines speed-limit (sine-wave start speed-limit))
    (c/add-lines speed-limit (:fitted nlm))
    i/view))

(-> chart
  (c/add-lines speed-limit (sine-wave start speed-limit))
  (c/add-lines speed-limit (:fitted nlm)))

;;; TODO: Find out what :rss and some of the other outputs are. Do
;;; any of them provide some measure of significance?
;;
;; user=> (:coefs nlm)
;; (3383.215658388778 0.08305070816729472 2.130481425095923E12 3.773551788382381 2869.621034522808)
;; user=> (:rss nlm)
;; 9.546606415963958E7

