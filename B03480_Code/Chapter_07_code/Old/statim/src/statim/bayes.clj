
;;;; Bayesian inference of multinomial distribution parameters
;;;; (07.11).
(ns statim.bayes
  (:require [incanter.core :as i]
            [incanter.bayes :as b]
            incanter.io
            [incanter.stats :as s]
            [incanter.charts :as c]))

#_
(require
  '[incanter.core :as i]
  '[incanter.bayes :as b]
  'incanter.io
  '[incanter.stats :as s]
  '[incanter.charts :as c])

(def census-race
  (i/col-names
    (incanter.io/read-dataset
      "data/all_160_in_51.P3.csv"
      :header true)
    [:geoid :sumlev :state :county :cbsa :csa :necta
     :cnecta :name :pop :pop2k :housing :housing2k :total
     :total2k :white :white2k :black :black2k :indian
     :indian2k :asian :asian2k :hawaiian :hawaiian2k
     :other :other2k :multiple :multiple2k]))

(def census-sample (s/sample census-race :size 60))

(def race-keys
  [:white :black :indian :asian :hawaiian :other :multiple])
(def race-totals
  (into {}
        (map #(vector % (i/sum (i/$ % census-sample)))
             race-keys)))

;;; For P=1 on the categories, the totals of the race totals have to
;;; equal the population total.
(= (i/sum (vals race-totals)) (i/sum (i/$ :total census-race)))

;;; This keeps in them in order of their fields.
(def y (map second (sort race-totals)))

;;; This draws from the appropriate multinomial Dirichlet
;;; distribution.
(def theta (b/sample-multinomial-params 2000 y))

;;; Now, pull the samples for each parameter.
(def theta-params
  (into {}
         (map #(vector %1 (i/sel theta :cols %2))
              (sort race-keys)
              (range))))

(s/mean (:black theta-params))
(s/sd (:black theta-params))
(s/quantile (:black theta-params) :probs [0.025 0.975])

(def ^:dynamic *field-labels*
  {:white "White alone"
   :black "Black or African American alone"
   :indian "American Indian and Alaska Native alone"
   :asian "Asian alone"
   :hawaiian "Native Hawaiian and Other Pacific Islander alone"
   :other "Some Other Race alone"
   :multiple "Two or More Races"})

(defn summarize
  ([coll & {:keys [title] :or {:title "Summary"}}]
   (println title)
   (println 'mean (s/mean coll))
   (println 'sd (s/sd coll))
   (println 'probability-quantiles
            (s/quantile coll :probs [0.025 0.975]))
   (println)
   (i/view (c/histogram coll :title title))))

(doseq [[k params] (sort theta-params)]
  (summarize params :title (get *field-labels* k)))

(s/mean (:black theta-params))
(s/sd (:black theta-params))
(s/quantile (:black theta-params) :probs [0.025 0.975])
(i/view (c/histogram (:black theta-params)))

(def minority-distribution
  (apply i/plus (map theta-params
                     (drop 1 race-keys))))
(summarize minority-distribution :title "All Minorities")

(def white-minority-diff
  (i/minus (:white theta-params) minority-distribution))
(summarize white-minority-diff :title "White-Minority Diff")

;;; Population figures
(defn ->ratios [row-map]
  (let [total (i/sum (map row-map race-keys))]
    (into {}
          (map #(vector % (/ (row-map %) total))
               race-keys))))

(def ratios (i/to-dataset (map ->ratios (:rows census-race))))
(s/mean (i/sel ratios :cols :black))

(/ (i/sum (i/sel census-race :cols :black))
   (i/sum (i/sel census-race :cols :total)))

