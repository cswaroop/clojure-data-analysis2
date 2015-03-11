
(ns getting-data.download-concurrent
  (:require [http.async.client :as http])
  (:import [java.net URL]))

(def ^:dynamic *block-size* 3)

;; WHO mortality data from http://www.who.int/whosis/mort/download/en/index.html.
(def urls 
  (let [who-ftp "http://www.who.int/whosis/database/mort/download/ftp/"]
    '((str who-ftp "documentation.zip")
        (str who-ftp "availability.zip")
        (str who-ftp "country_codes.zip")
        (str who-ftp "notes.zip")
        (str who-ftp "Pop.zip")
        (str who-ftp "morticd07.zip")
        (str who-ftp "morticd08.zip")
        (str who-ftp "morticd09.zip")
        (str who-ftp "morticd10.zip"))))

(defn get-verbose
  "This uses http.async.client to download a URL"
  [client url]
  (println "GET" url)
  [url (http/GET client url)])

(defn get-response
  "This forces the response to download and prints out what's happening."
  [[url resp]]
  (println "awaiting" url)
  (http/await resp)
  (println "done" url)
  (http/status resp))

(defn get-block
  "This forces a block of responses to download."
  [block]
  (doall (map get-response block)))

(defn sequential
  "This downloads the resources sequentially."
  []
  (with-open [client (http/create-client :follow-redirects true)]
    (doall
      (map get-response
           (map (partial get-verbose client)
                urls)))))

(defn async
  "This downloads the resources asynchronously."
  []
  (with-open [client (http/create-client :follow-redirects true)]
    (doall
      (mapcat get-block
              (partition-all *block-size*
                             (map
                               (partial get-verbose client)
                               urls))))))

(defn main
  []
  (println "sequential")
  (time (sequential))

  (binding [*block-size* 3]
    (println "async 3")
    (time (async)))

  (binding [*block-size* 4]
    (println "async 4")
    (time (async)))

  (binding [*block-size* 5]
    (println "async 5")
    (time (async))))

; s  : 96997.266
; a3 : 57563.421 (59%)
; a4 : 73426.117 (76%)
; a5 :  9776.881 (51%)

; s  : 104472.236
; a3 :  64388.185 (62%)
; a4 :  79521.749 (76%)
; a5 :  55478.752 (53%)

; s  : 101603.85
; a3 :  65366.854 (64%)
; a4 :  76972.554 (76%)
; a5 :  53152.885 (52%)


; (main)

