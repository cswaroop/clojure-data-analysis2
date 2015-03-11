
(ns statim.summary
  (:require [incanter.core :as i]
            incanter.io
            [incanter.stats :as s]))

#_
(require '[incanter.core :as i]
         'incanter.io
         '[incanter.stats :as s])

(def data-file "data/all_160.P3.csv")

(comment

(def census (incanter.io/read-dataset data-file :header true))
(require '[incanter.charts :as c])
(i/with-data census
             (i/view (c/histogram :POP100 :sbins 100)))
(i/$rollup :mean :POP100 :STATE census)
(i/$rollup s/sd :POP100 :STATE census)
(i/$rollup s/skewness :POP100 :STATE census)
(i/$rollup s/median :POP100 :STATE census)

  )

