
(ns inc-dsets.mongo-sets
  (:use (incanter core io mongodb stats)
        somnium.congomongo))

#_
(use '(incanter core io mongodb stats)
     'somnium.congomongo)

(def mongo-cxn (make-connection "va-census"))
(set-connection! mongo-cxn)

(defn summarize
  ([dataset]
   {:n (nrow dataset)
    :mean (mean dataset)
    :sd (sd dataset)
    :skew (skewness dataset)}))

;;;; Fetch whole dataset.
(def data (fetch-dataset :db))
(summarize ($ :POP100 data))

;;;; Fetch only part of the db.
(def small-towns
  (fetch-dataset :db :where { :POP100 { :$lte 1000 } }))
(summarize ($ :POP100 small-towns))

(def large-towns
  (fetch-dataset :db :where { :POP100 { :$gt 1000 } }))
(summarize ($ :POP100 large-towns))

;;;; Fetch only some columns.
(def col-select
  (fetch-dataset :db :only [:_id :NAME :POP100]))
(col-names col-select)

