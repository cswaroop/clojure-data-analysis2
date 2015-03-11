
(ns distrib-data.parse-csv
  (:require (cascalog
              [workflow :as w]
              [ops :as c]
              [vars :as v]
              [tap :as tap]))
  (:use cascalog.api)
  (:import [cascading.tuple Fields]
           [cascading.scheme.hadoop TextDelimited]))

(comment
(require '(cascalog [workflow :as w]
                    [ops :as c]
                    [vars :as v]
                    [tap :as tap]))
(use 'cascalog.api)
(import [cascading.tuple Fields]
        [cascading.scheme.hadoop TextDelimited])
  )

(defn hfs-text-delim
  ([path & {:keys [fields has-header delim quote-str]
            :as opts
            :or {fields Fields/ALL, has-header false, delim ",",
                 quote-str "\""}}]
   (let [scheme (TextDelimited. (w/fields fields) has-header delim
                                quote-str)
         tap-opts (select-keys opts [:sinkmode
                                     :sinkparts
                                     :source-pattern
                                     :sink-template
                                     :templatefields])]
     (apply tap/hfs-tap scheme path tap-opts))))

(defmapop
  ->long
  [value]
  (Long/parseLong value))

(comment
  ;; Need to do this on the input file:
  ;; :s/,$/,0/
(?<- (stdout)
     [?companion-id ?name]
     ((hfs-text-delim "data/companions.txt") ?companion-id ?name))

(use 'distrib-data.parse-csv :reload)
(?<- (stdout)
     [?GEOID ?NAME ?pop-int]
     ((hfs-text-delim "data/all_160_in_51.P35.csv" :has-header true)
        ?GEOID _ _ _ _ _ _ _ ?NAME ?POP100 _ _ _ _ _)
     (->long ?POP100 :> ?pop-int)
     (<= ?pop-int 1000))
(?<- (stdout)
     [?avg]
     ((hfs-text-delim "data/all_160_in_51.P35.csv" :has-header true)
        _ _ _ _ _ _ _ _ _ ?POP100 _ _ _ _ _)
     (->long ?POP100 :> ?pop-int)
     (<= ?pop-int 1000)
     (c/avg ?pop-int :> ?avg))
  )

