
(comment
  (use 'd-mining.clean-up :reload)
  (in-ns 'd-mining.clean-up)
  )

(ns d-mining.clean-up
  (:require [incanter.core :as i]
            incanter.io
            [clojure.set :as set]))

(def data-files
  {:housing "data/ACS_10_1YR_B25077/ACS_10_1YR_B25077.csv",
   :economics "data/ACS_10_1YR_DP03/ACS_10_1YR_DP03_with_ann.csv",
   :demographics "data/ACS_10_1YR_DP05/ACS_10_1YR_DP05_with_ann.csv"})

(def projections
  {:economics [:GEO.id
               :HC01_VC85  ; Estimate; INCOME AND BENEFITS (IN 2010 INFLATION-ADJUSTED DOLLARS) - Median household income (dollars)
               :HC01_VC156 ; Estimate; PERCENTAGE OF FAMILIES AND PEOPLE WHOSE INCOME IN THE PAST 12 MONTHS IS BELOW THE POVERTY LEVEL - All families
               ],
   :demographics [:GEO.id
                  :HC01_VC38  ; Estimate; RACE - Total population
                  :HC01_VC72  ; Estimate; RACE - White
                  :HC01_VC73  ; Estimate; RACE - Black or African American
                  :HC01_VC74  ; Estimate; RACE - American Indian and Alaska Native
                  :HC01_VC75  ; Estimate; RACE - Asian
                  :HC01_VC76  ; Estimate; RACE - Native Hawaiian and Other Pacific Islander
                  :HC01_VC77  ; Estimate; RACE - Some other race
                  ]
   })

(def datasets
  (->>
    data-files
    (mapcat #(vector (first %) (incanter.io/read-dataset (second %) :header true)))
    (apply hash-map)))

;; Sanity check
#_
(map #(vector (first %) (i/nrow (second %))) datasets)

(def projected
  (let [proj (fn [[k ds]]
               (if-let [cols (projections k)]
                 (i/sel ds :cols cols)
                 ds))]
    (->>
      datasets
      (mapcat #(vector (first %) (proj %)))
      (apply hash-map))))

(def dataset
  (i/$join [:GEO.id :GEO.id]
      (i/$join [:GEO.id :GEO.id]
          (:housing projected)
          (:economics projected))
      (:demographics projected)))

(defn col-set-report
  ([]
   (doseq [[[ka va] [kb vb]] (for [a datasets, b datasets :when (not= a b)] [a b])]
     (let [set-a (set (i/col-names va))
           set-b (set (i/col-names vb))]
       (println ka kb)
       (println (set/intersection set-a set-b))
       (println (set/difference set-a set-b))
       (println)))))
#_
(col-set-report)

