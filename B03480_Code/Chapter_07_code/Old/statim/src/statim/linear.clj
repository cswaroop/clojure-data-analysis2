
(ns statim.linear
  (:require [incanter.core :as i]
            incanter.io
            [incanter.stats :as s]
            [incanter.charts :as c]))

#_
(require
  '[incanter.core :as i]
  'incanter.io
  '[incanter.stats :as s]
  '[incanter.charts :as c])

(defn is-year-field?
  "This tests whether the field is a year."
  ([f] (re-matches #"y\d{4}" (name f))))

(defn ->int
  "If this is an empty string, it returns the default value
  (by default, 0). Otherwise, it returns the value."
  ([s] (->int s 0))
  ([s d] (if (= s "") d s)))

(defn index-year
  ([k m [year value]]
   (assoc-in m [year k] value)))

(defn fold-row
  "This folds all the year data into the row."
  ([index-map row]
   (reduce
     (partial index-year (:icode row))
     index-map
     (filter (comp is-year-field? first) row))))

(defn empty-index
  ([sample]
   (apply assoc
          (select-keys sample [:cname :ccode])
          (mapcat #(vector % {})
                  (filter is-year-field?
                          (keys sample))))))

(defn watch
  ([tag value]
   (println '>>> tag value)
   value))

(defn pivot-rows
  "This takes a sequence of rows grouped by country that
  contain all the data for a sequence of rows based on year."
  ([rows]
   (if-let [rs (seq rows)]
     (reduce fold-row (empty-index (first rs)) rows)
     ())))

(defn ->year
  ([year-key]
   (read-string (.substring (name year-key) 1))))

(defn output-year
  ([cname ccode [year fields]]
   (apply assoc
          {:cname cname, :ccode ccode, :year (->year year)}
          (apply concat
                 (map (fn [[k v]] [(keyword k) v])
                      fields)))))

(defn index->rows
  ([index]
   (map (partial output-year (:cname index) (:ccode index))
        (filter (comp is-year-field? first)
                (seq index)))))

;;; Simplify this. Also, convert all fields to keywords.
;;; Pull out the parts that have the same country and don't
;;; include that data in the output until the last minute.
;;; Then remove all the references to is-year-field?.
(defn pull-categories
  "data is the dataset.
  cats are field keywords paired with category codes."
  ([cats] (pull-categories cats i/$data))
  ([cats data]
   (let [cat-set (set cats)]
     (->> data
       i/to-list
       (map (partial zipmap (i/col-names data)))
       (filter (comp (partial contains? cat-set) :icode))
       (group-by #(vector (:cname %) (:ccode %)))
       vals
       (map pivot-rows)
       (mapcat index->rows)
       i/to-dataset))))

(def wb-data
  (i/col-names
    (incanter.io/read-dataset "data/worldbank-data.csv"
                              :skip 1)
    (concat [:cname :ccode :iname :icode]
            (map #(keyword (str \y %))
                 (range 1960 2013)))))

#_
(let [cats ["SP.POP.TOTL"
            "NY.GDP.MKTP.KD"
            "NY.GNP.MKTP.KD"
            "VC.IHR.PSRC.P5"
            "IC.FRM.CRIM.ZS"
            "SI.POV.GINI"
            "SI.DST.02ND.20"
            "SI.DST.10TH.10"
            "SI.DST.FRST.20"
            "SI.DST.FRST.10"
            "SI.DST.04TH.20"
            "SI.DST.05TH.20"
            "SI.DST.03RD.20"]]
  (->> wb-data
    i/to-list
    (map (partial zipmap (i/col-names wb-data)))
    (filter (comp (partial contains? (set cats)) :icode))
    (group-by #(vector (:cname %) (:ccode %)))
    vals
    (map pivot-rows)
    (mapcat index->rows)
    i/to-dataset
    ))

(def ^:dynamic *pivot-categories*
  ["SP.POP.TOTL"
   "NY.GDP.MKTP.KD"
   "NY.GNP.MKTP.KD"
   "VC.IHR.PSRC.P5"
   "IC.FRM.CRIM.ZS"
   "SI.POV.GINI"
   "SI.DST.02ND.20"
   "SI.DST.10TH.10"
   "SI.DST.FRST.20"
   "SI.DST.FRST.10"
   "SI.DST.04TH.20"
   "SI.DST.05TH.20"
   "SI.DST.03RD.20"])

(def wb-pivoted (pull-categories *pivot-categories* wb-data))

(def wb-filtered
  (i/$where {:SI.POV.GINI {:$ne ""}
             :VC.IHR.PSRC.P5 {:$ne ""}
             :IC.FRM.CRIM.ZS {:$ne ""}}
      wb-pivoted))

(comment
  ;; -20.7706362841398970.8044672388955592
  (def gini (i/sel wb-filtered :cols :SI.POV.GINI))
  (def homicides (i/sel wb-filtered :cols :VC.IHR.PSRC.P5))
  (def losses-crime (i/sel wb-filtered :cols :IC.FRM.CRIM.ZS))
  (def homicides-lm (s/linear-model homicides gini))
  (def losses-lm (s/linear-model losses-crime gini))
  (def lms [homicides-lm losses-lm])
  (def murder-chart
    (doto
      (c/scatter-plot gini homicides
                      :title "GINI to Homicides"
                      :y-label "Homicide Rates"
                      :legend true)
      (c/add-lines gini (:fitted homicides-lm)
                   :series-label "Homicides Model")))
  (def losses-chart
    (doto
      (c/scatter-plot gini losses-crime
                      :title "GINI to Losses to Crime"
                      :y-label "Losses to Crime"
                      :legend true)
      (c/add-lines gini (:fitted losses-lm)
                   :series-label "Losses to Crime Model"))))

;;; The homicide match is probably better than the losses due
;;; to crime:

;; user=> (map :r-square lms)
;; (0.3328698214114437 0.1412899713081264)
;; user=> (map :f-stat lms)
;; (40.91454146698469 13.49207213163179)
;; user=> (map :f-prob lms)
;; (0.9999999907660698 0.9995742388497674)

;; Neither of these is particularly great.

(defn dump-matches
  "This takes a map and prints all key-value pairs whose
  values match the predicate p."
  ([m p]
   (doseq [pair (filter (comp p second) m)]
     (println pair))))
(defn dump-floats
  "This takes a map and dumps all the key-value pairs whose
  values are floats."
  ([m] (dump-matches float?)))

(defn ratio-of ([total & args] (float (/ (apply + args) total))))

(comment
(def census-race
  (incanter.io/read-dataset "data/all_160_in_51.P3.csv"
                            :header true))
(def population (i/sel census-race :cols :POP100))
(def afam (i/$map ratio-of [:POP100 :P003004] census-race))
(def minorities
  (let [fields [:P003003 :P003004 :P003005 :P003006 :P003007
                :P003008]]
    (i/$map ratio-of (concat [:POP100] fields) census-race)))
(def afam-lm (s/linear-model afam population))
(def minorities-lm (s/linear-model minorities population))
(def minority-chart
  (doto
    (c/scatter-plot population minorities
                    :title "Ratio of Minorities to Population"
                    :x-label "Population"
                    :y-label "All Minorities"
                    :legend true)
    (c/add-lines population (:fitted minorities-lm)
                 :series-label "All Minority Model")))
(i/view minority-chart)
  )

;;; Neither of these is a great match:

;;
;; user=> (map :r-square lms)
;; (7.622836125612382E-4 0.02927089249186006)
;; user=> (map :f-stat lms)
;; (0.4493275628363916 17.760418992649814)
;; user=> (map :f-prob lms)
;; (0.49708281445794866 0.999971012738415)

;;; Probably I should just use this:

(comment
  (def data (incanter.io/read-dataset "data/all_160_in_51.P35.csv"
                                      :header true))
  (def population (i/sel data :cols :POP100))
  (def families (i/sel data :cols :P035001))
  (def lm (s/linear-model families population))
  (def chart
    (doto (c/scatter-plot population families
                          :title "Families by Population"
                          :x-label "Population"
                          :y-label "Families"
                          :legend true)
      (c/add-lines population (:fitted lm))))
  (i/view chart)
  ;;; Just a little over-fitted this time:
  ;;
  ;; user=> (:r-square lm)
  ;; 0.9857430744123642
  ;; user=> (:f-stat lm)
  ;; 40724.25483741083
  ;; user=> (:f-prob lm)
  ;; 0.9999999999999999
  )

#_
(def event-data
  (i/col-names
    (incanter.io/read-dataset "data/pitf.world.19950101-20120630.csv"
                              :skip 2)
    ['event-type-and-reporting/event-type
     'event-type-and-reporting/campaign-identifier
     'event-type-and-reporting/event-reporting 'event-date/start-day
     'event-date/start-month 'event-date/start-year 'event-date/end-day
     'event-date/end-month 'event-date/end-year 'event-location/country
     'event-location/region 'event-location/district 'event-location/locality
     'latitude/degrees 'latitude/minutes 'latitude/seconds 'latitude/direction
     'longitude/degrees 'longitude/minutes 'longitude/seconds
     'longitude/direction 'relational-location/distance-km
     'relational-location/direction 'perpetrators/perp-state-role
     'perpetrators/perp-state-military 'perpetrators/perp-state-police
     'perpetrators/perp-state-other 'perpetrators/perp-nonstate-ideological
     'perpetrators/perp-nonstate-ethnic 'perpetrators/perp-nonstate-religious
     'perpetrators/perp-nonstate-criminal 'perpetrators/perp-nonstate-private
     'perpetrators/perp-unknownunclearother
     'victims/victim-noncombatant-asserted
     'victims/victim-noncombatant-contested 'victims/victim-identity-political
     'victims/victim-identity-ethnicnationalcitizenship
     'victims/victim-identity-religious 'victims/victim-identity-socioeconomic
     'victims/victim-identity-unarmed-combatants
     'victims/victim-identity-randomunknownunclearother
     'casualties/deaths-number 'casualties/deaths-scale
     'casualties/injured-number 'casualties/injured-scale
     'casualties/deaths-ambiguity 'casualties/deaths-contested
     'modes-of-violence/organization-of-violence 'modes-of-violence/weapons
     'perpetrator-intent/intent 'perpetrator-intent/regrets
     'perpetrator-intent/collateral-damage 'related-tactics/foodaid-as-a-weapon
     'related-tactics/scorched-earth-tactics 'related-tactics/human-shields
     'related-tactics/rape 'related-tactics/targeted-assassinations
     'related-tactics/mass-detentions 'related-tactics/siegesclosures
     'related-tactics/kidnappings 'related-tactics/disappearances
     'related-tactics/other-tactics 'description/description 'link/link
     'data-source/primary-source-type 'data-source/primary-source
     'data-source/secondary-source-type 'data-source/secondary-source
     'data-source/contesting-source-type 'data-source/contesting-source
     'data-source/citation 'comments/comments 'comments/coder 'comments/n1
     'comments/n2 'comments/n3 'comments/n4 'comments/n5 'comments/n6
     'comments/n7 'comments/n8 'comments/n9 'comments/n10 'comments/n11
     'comments/n12 'comments/n13 'comments/n14 'comments/n15 'comments/n16
     'comments/n17 'comments/n18 'comments/n19 'comments/n20 'comments/n21
     'comments/n22 'comments/n23 'comments/n24 'comments/n25 'comments/n26
     'comments/n27 'comments/n28 'comments/n29 'comments/n30 'comments/n31
     'comments/n32 'comments/n33 'comments/n34 'comments/n35 'comments/n36
     'comments/n37 'comments/n38 'comments/n39 'comments/n40 'comments/n41
     'comments/n42 'comments/n43 'comments/n44 'comments/n45 'comments/n46
     'comments/n47 'comments/n48 'comments/n49 'comments/n50 'comments/n51
     'comments/n52 'comments/n53 'comments/n54 'comments/n55 'comments/n56
     'comments/n57 'comments/n58 'comments/n59 'comments/n60 'comments/n61
     'comments/n62 'comments/n63 'comments/n64 'comments/n65 'comments/n66
     'comments/n67 'comments/n68 'comments/n69 'comments/n70 'comments/n71
     'comments/n72 'comments/n73 'comments/n74 'comments/n75 'comments/n76
     'comments/n77 'comments/n78 'comments/n79 'comments/n80 'comments/n81
     'comments/n82 'comments/n83 'comments/n84 'comments/n85 'comments/n86
     'comments/n87 'comments/n88 'comments/n89 'comments/n90 'comments/n91
     'comments/n92 'comments/n93 'comments/n94 'comments/n95 'comments/n96
     'comments/n97 'comments/n98 'comments/n99 'comments/n100 'comments/n101
     'comments/n102 'comments/n103 'comments/n104 'comments/n105 'comments/n106
     'comments/n107 'comments/n108 'comments/n109 'comments/n110 'comments/n111
     'comments/n112 'comments/n113 'comments/n114 'comments/n115 'comments/n116
     'comments/n117 'comments/n118 'comments/n119 'comments/n120 'comments/n121
     'comments/n122 'comments/n123 'comments/n124 'comments/n125 'comments/n126
     'comments/n127 'comments/n128 'comments/n129 'comments/n130 'comments/n131
     'comments/n132 'comments/n133 'comments/n134 'comments/n135 'comments/n136
     'comments/n137 'comments/n138 'comments/n139 'comments/n140 'comments/n141
     'comments/n142 'comments/n143 'comments/n144 'comments/n145 'comments/n146
     'comments/n147 'comments/n148 'comments/n149 'comments/n150 'comments/n151
     'comments/n152 'comments/n153 'comments/n154 'comments/n155 'comments/n156
     'comments/n157 'comments/n158 'comments/n159 'comments/n160 'comments/n161
     'comments/n162 'comments/n163 'comments/n164 'comments/n165 'comments/n166
     'comments/n167 'comments/n168 'comments/n169 'comments/n170 'comments/n171
     'comments/n172 'comments/n173 'comments/n174 'comments/n175 'comments/n176
     'comments/n177 'comments/n178 'comments/n179 'comments/n180 'comments/n181
     'comments/n182 'comments/n183]))

(comment
(def family-data
  (incanter.io/read-dataset "data/all_160_in_51.P35.csv"
                            :header true))
(def housing (i/sel family-data :cols :HU100))
(def families (i/sel family-data :cols :P035001))
(def families-lm
  (s/linear-model housing families :intercept true))

(defn model-sample [dset k x-col y-col]
  (let [sample (s/sample dset :size k)
        x (i/sel sample :cols :HU100)
        y (i/sel sample :cols :P035001)]
    {:sample sample
     :x x
     :y y
     :lm (s/linear-model x y :intercept false)}))

(:r-square families-lm)
(:f-prob families-lm)

(def housing-chart
  (doto
    (c/scatter-plot families housing
                    :title "Relationship of Housing to Families"
                    :x-label "Families"
                    :y-label "Housing"
                    :legend true)
    (c/add-lines families (:fitted families-lm)
                 :series-label "Linear Model")
    (i/view)))
  )

