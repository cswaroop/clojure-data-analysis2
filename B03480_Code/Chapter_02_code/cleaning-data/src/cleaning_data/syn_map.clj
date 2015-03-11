
(ns cleaning-data.syn-map
  (:use [clojure.string :only (upper-case)]))

#_
(use '[clojure.string :only (upper-case)])

(def state-synonyms
  {"ALABAMA" "AL",
   "ALASKA" "AK",
   "ARIZONA" "AZ",
   "ARKANSAS" "AR",
   "CALIFORNIA" "CA",
   "COLORADO" "CO",
   "CONNECTICUT" "CT",
   "DELAWARE" "DE",
   "FLORIDA" "FL", "FLA" "FL",
   "GEORGIA" "GA",
   "HAWAII" "HI",
   "IDAHO" "ID",
   "ILLINOIS" "IL",
   "INDIANA" "IN",
   "IOWA" "IA",
   "KANSAS" "KS",
   "KENTUCKY" "KY",
   "LOUISIANA" "LA",
   "MAINE" "ME",
   "MARYLAND" "MD",
   "MASSACHUSETTS" "MA",
   "MICHIGAN" "MI",
   "MINNESOTA" "MN",
   "MISSISSIPPI" "MS",
   "MISSOURI" "MO",
   "MONTANA" "MT",
   "NEBRASKA" "NE",
   "NEVADA" "NV",
   "NEW HAMPSHIRE" "NH",
   "NEW JERSEY" "NJ",
   "NEW MEXICO" "NM",
   "NEW YORK" "NY",
   "NORTH CAROLINA" "NC",
   "NORTH DAKOTA" "ND",
   "OHIO" "OH",
   "OKLAHOMA" "OK",
   "OREGON" "OR",
   "PENNSYLVANIA" "PA",
   "RHODE ISLAND" "RI",
   "SOUTH CAROLINA" "SC",
   "SOUTH DAKOTA" "SD",
   "TENNESSEE" "TN",
   "TEXAS" "TX",
   "UTAH" "UT",
   "VERMONT" "VT",
   "VIRGINIA" "VA",
   "WASHINGTON" "WA",
   "WEST VIRGINIA" "WV",
   "WISCONSIN" "WI",
   "WYOMING" "WY"})

(defn normalize-state
  "This normalizes states according to the synonym map.

  Output should be two-letter, upper-case abbreviations."
  [state]
  (let [uc-state (upper-case state)]
    (state-synonyms uc-state uc-state)))

