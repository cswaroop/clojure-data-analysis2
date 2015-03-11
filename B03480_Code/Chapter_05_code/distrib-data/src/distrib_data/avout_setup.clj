
(ns distrib-data.avout-setup
  (:use avout.core))

(comment
  "On one machine."
(use 'avout.core)
(def client (connect "127.0.0.1"))
(def data-count (zk-ref client "/data-count" 0))
(dosync!! client (alter!! data-count inc))
@data-count

  "On another machine"
(use 'avout.core)
(def client (connect "10.0.0.9"))
(def data-count (zk-ref client "/data-count"))
@data-count
(dosync!! client (alter!! data-count inc))

  )


