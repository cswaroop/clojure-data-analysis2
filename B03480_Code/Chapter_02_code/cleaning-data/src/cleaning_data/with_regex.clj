
(ns cleaning-data.with-regex
  (:require [clojure.string :as string]))

#_
(require '[clojure.string :as string])

(def ^:dynamic *phone-regex*
  #"(?x)
  (\d{3})     # Area code.
  \D{0,2}     # Separator. Probably one of \(, \), \-, \space.
  (\d{3})     # Prefix.
  \D?         # Separator.
  (\d{4})
  ")

(def ^:dynamic *email-regex*
  #"(?x)
  (
    [^\s<]+   # Anything except whitespace or \<, really.
    @
    [^\s>]+   # Anything except whitespace or \>, really.
  )
  ")


(defn clean-us-phone
  "This cleans a US phone number to the form (999)999-9999."
  [phone]
  (when-let [[_ area-code prefix post]
             (re-find *phone-regex* phone)]
    (str \( area-code \) prefix \- post)))

(defn clean-email
  "This cleans email addresses."
  [email]
  (second (re-find *email-regex* email)))

(defn company-name
  "This abbreviates and subordinates 'Incorporated'."
  [co-name]
  (-> co-name
    (string/replace #"Incorporated" "Inc.")
    (string/replace #"([^,]) Inc." "$1, Inc.")))


