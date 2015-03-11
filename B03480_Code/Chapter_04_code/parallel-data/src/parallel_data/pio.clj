
;;; Hmm. I'm going to have to figure out what to do about this.
;;; Here's one thought why: http://stackoverflow.com/questions/7200690/why-people-say-mmap-by-mappedbytebuffer-is-faster?rq=1
;;;
;;; user=> (def susers (time (serial-process "comments.xml")))
;;; "Elapsed time: 14211.196 msecs"
;;; #'user/susers
;;; user=> (def susers (time (serial-process "comments.xml")))
;;; "Elapsed time: 13956.926 msecs"
;;; #'user/susers
;;; user=> (def musers (time (mmap-process "comments.xml")))
;;; "Elapsed time: 14808.8 msecs"
;;; #'user/musers
;;; user=> (def musers (time (mmap-process "comments.xml")))
;;; "Elapsed time: 14755.478 msecs"
;;; #'user/musers

(ns parallel-data.pio
  (:require [nio.core :as nio]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.core.reducers :as r])
  (:use [clj-time.core :exclude (extend)]
        [clj-time format coerce])
  (:import [java.io File RandomAccessFile]
           [java.nio.channels FileChannel]
           [java.nio.charset Charset]
           [org.apache.commons.lang3 StringEscapeUtils])
  )

(comment
(import '[java.io File RandomAccessFile]
        '[java.nio.charset Charset]
        '[org.apache.commons.lang3 StringEscapeUtils])
(use '[clj-time.core :exclude (extend)]
     '[clj-time format coerce]
     'criterium.core)
(require '[nio.core :as nio]
         '[clojure.java.io :as io]
         '[clojure.string :as string]
         '[clojure.core.reducers :as r])
  )

;;;; First, take care of the model and what we do with the data.
(defrecord UserPost
  [user-id earliest-date latest-date])

(defn combine-user-posts
  ([up1 up2]
   (UserPost.
     (:user-id up1)
     (:earliest-date
       (from-long (min (to-long (:earliest-date up1))
                       (to-long (:earliest-date up2)))))
     (:latest-date
       (from-long (max (to-long (:latest-date up1))
                       (to-long (:latest-date up2))))))))

(def combiner
  (r/monoid (partial merge-with combine-user-posts) hash-map))

;;;; Now general parsing functions
(defn split-lines
  ([input] (split-lines input 0))
  ([input start]
   (let [end (.indexOf input 10 start)]
     (when-not (= end -1)
       (lazy-seq
         (cons (String. (.trim (.substring input start end)))
               (split-lines input (inc end))))))))

(defn data-line?
  ([line]
   (.startsWith (string/trim line) "<row ")))

(defn parse-pair
  ([[k v]]
   [(keyword k)
    (StringEscapeUtils/unescapeXml v)]))

(defn parse-line
  ([line]
   (->> line
     (re-seq #"(\w+)=\"([^\"]*)\"")
     (map next)
     (map parse-pair)
     flatten
     (apply hash-map))))

(def ^:dynamic *date-formatter*
  (formatters :date-hour-minute-second-ms))

(defn line->user-post
  ([line]
   (let [user-id (if-let [uid (:UserId line)]
                   (read-string uid)
                   nil)
         cdate (parse *date-formatter* (:CreationDate line))]
     (UserPost. user-id cdate cdate))))

;;; Parse lines and accumulate the results.
(defn process-user-map
  ([] {})
  ([user-post-map line]
   (let [user-post (line->user-post line)
         user-id (:user-id user-post)]
     (assoc user-post-map
            user-id
            (if-let [current (get user-post-map user-id)]
              (combine-user-posts current user-post)
              user-post)))))

(defn process-lines
  ([lines]
   (->>
     lines
     (r/map parse-line)
     (r/filter data-line?)
     (r/fold combiner process-user-map))))

;;;; First the first attempt, we work sequentially.
(defn serial-process
  ([file-name]
   (with-open [reader (io/reader file-name)]
     (process-lines (line-seq reader)))))

;;;; Now we include settings for memory maps.
(def ^:dynamic *chunk-size* (* 10 1024 1024))
(def ^:dynamic *charset* (Charset/forName "UTF-8"))

;;; Processing chunks of the file into a mmap.
(defn get-chunk-offsets
  ([f pos offsets chunk-size]
   (let [skip-to (+ pos chunk-size)]
     (if (>= skip-to (.length f))
       (conj offsets (.length f))
       (do
         (.seek f skip-to)
         (while (not= (.read f) (int \newline)))
         (let [new-pos (.getFilePointer f)]
           (recur f new-pos (conj offsets new-pos)
                  chunk-size)))))))

(defn get-chunks
  ([file-name] (get-chunks file-name *chunk-size*))
  ([file-name chunk-size]
   (with-open [f (RandomAccessFile. file-name "r")]
     (doall
       (partition 2 1 (get-chunk-offsets f 0 [0] chunk-size))))))

(defn read-chunk
  ([channel [from to]]
   (let [chunk-mmap
         (.map channel
               java.nio.channels.FileChannel$MapMode/READ_ONLY
               from
               (- to from))
         decoder (.newDecoder *charset*)]
     (doall 
       (split-lines (str (.decode decoder chunk-mmap)))))))

;;;; And a mmap process.
(defn mmap-process
  ([file-name]
   (let [chan (nio/channel file-name)]
     (->>
       file-name
       get-chunks
       (r/mapcat (partial read-chunk chan))
       (r/map parse-line)
       (r/filter data-line?)
       (r/fold combiner process-user-map)))))

;;;; Finally, some general functions and settings.

;; (def data-file "data/092011 Stack Overflow/posthistory.xml")    ;;;; 10  G
;; (def data-file "data/092011 Stack Overflow/posts.xml")          ;;;;  7.0G
;; (def data-file "data/092011 Stack Overflow/comments.xml") ;  1.8G
(def data-file "data/comments.xml")
; 22M

(defn time-all
  ([]
   (println 'serial-process)
   (.flush *out*)
   (let [susers (time (serial-process data-file))]
     (println 'map-process)
     (.flush *out*)
     (let [musers (time (mmap-process data-file))]
       {:serial susers
        :mmap musers}))))

