
(ns cleaning-data.normalize-dates
  (:use [clj-time.core :exclude (extend second)]
        [clj-time.format]))

#_
(use '[clj-time.core :exclude (extend second)]
     '[clj-time.format])

(def ^:dynamic *default-formats*
  [:date
   :date-hour-minute
   :date-hour-minute-second
   :date-hour-minute-second-ms
   :date-time
   :date-time-no-ms
   :rfc822
   "YYYY-MM-dd HH:mm"
   "YYYY-MM-dd HH:mm:ss"
   "dd/MM/YYYY"
   "YYYY/MM/dd"
   "d MMM YYYY"])

(defprotocol ToFormatter
  (->formatter [fmt]))

(extend-protocol ToFormatter
  java.lang.String
  (->formatter [fmt] (formatter fmt))

  clojure.lang.Keyword
  (->formatter [fmt] (formatters fmt)))

(defn parse-or-nil [fmt date-str]
  (try
    (parse (->formatter fmt) date-str)
    (catch Exception ex
      nil)))

(defn normalize-datetime [date-str]
  (first
    (remove nil?
            (map #(parse-or-nil % date-str)
                 *default-formats*))))

