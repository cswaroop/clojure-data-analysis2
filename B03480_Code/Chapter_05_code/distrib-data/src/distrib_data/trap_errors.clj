
(ns distrib-data.trap-errors
  (:require (cascalog
              [workflow :as w]
              [ops :as c]
              [vars :as v]
              [tap :as tap]))
  (:use cascalog.api
        distrib-data.parse-csv))

(comment
(require '(cascalog [workflow :as w]
                    [ops :as c]
                    [vars :as v]
                    [tap :as tap]))
(use 'cascalog.api
     'distrib-data.parse-csv)
  )

; (def families-file "hdfs:///user/hadoop/census/all_160_in_51.P35.csv")
; (def race-file "hdfs:///user/hadoop/census/all_160_in_51.P3.csv")
(def families-file "data/all_160_in_51.P35.csv")
(def race-file "data/all_160_in_51.P3.csv")

(def family-data
  (<- [?GEOID ?SUMLEV ?STATE
       ?NAME ?POP100 ?HU100 ?P035001]
      ((hfs-text-delim families-file
                       :has-header true)
         ?GEOID ?SUMLEV ?STATE _ _ _ _ _
         ?NAME ?spop100 ?shu100 _ _ ?sp035001 _)
      (->long ?spop100  :> ?POP100)
      (->long ?shu100   :> ?HU100)
      (->long ?sp035001 :> ?P035001)))

(def race-data
  (<- [?GEOID ?SUMLEV ?STATE ?NAME ?POP100 ?HU100
       ?P003001 ?P003002 ?P003003 ?P003004 ?P003005 ?P003006
       ?P003007 ?P003008]
      ((hfs-text-delim race-file :has-header true)
         ?GEOID ?SUMLEV ?STATE _ _ _ _ _
         ?NAME ?spop100 ?shu100 _ _
         ?sp003001 _ ?sp003002 _ ?sp003003 _ ?sp003004 _
         ?sp003005 _ ?sp003006 _ ?sp003007 _ ?sp003008 _)
      (->long ?spop100  :> ?POP100)  (->long ?shu100   :> ?HU100)
      (->long ?sp003001 :> ?P003001) (->long ?sp003002 :> ?P003002)
      (->long ?sp003003 :> ?P003003) (->long ?sp003004 :> ?P003004)
      (->long ?sp003005 :> ?P003005) (->long ?sp003006 :> ?P003006)
      (->long ?sp003007 :> ?P003007) (->long ?sp003008 :> ?P003008)))

(def census-joined
  (<- [?name ?pop100 ?hu100 ?families
       ?white ?black ?indian ?asian ?hawaiian ?other ?multiple]
      (family-data ?geoid _ _
                   ?name ?pop100 ?hu100 ?families)
      (race-data ?geoid _ _ _ _ _ _
                 ?white ?black ?indian ?asian
                 ?hawaiian ?other ?multiple)))

(def small-towns
  (<- [?name ?pop100 ?hu100
       ?pop100.2000 ?hu100.2000
       ?p035001 ?p035001.2000]
      (all-160-in-51 _ _ _
                     ?name ?pop100 ?hu100
                     ?pop100.2000 ?hu100.2000
                     ?p035001 ?p035001.2000)
      (<= ?pop100 1000)))

(defmapop
  throw-error
  [value]
  (if (< value 125)
    (div value 0)
    0))

(def families-per-hu
  (<- [?name ?pop100 ?hu100
       ?pop100.2000 ?hu100.2000
       ?p035001 ?p035001.2000
       ?fam-hu]
      (census-joined ?name ?pop100 ?hu100
                     ?families ?white ?black ?indian
                     ?asian ?hawaiian ?other ?multiple)
      (throw-error ?families :> ?err)
      (div ?families ?hu100 :> ?fam-hu)
      (:trap (hfs-textline "data/trap"))))

(comment
(use 'distrib-data.trap-errors :reload)
(?- (stdout) families-per-hu)
  )


