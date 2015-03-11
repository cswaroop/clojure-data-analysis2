
;; NB: Do this one for 03.04.
(ns concurrent-data.agent-stm
  (:use [concurrent-data.agents :only (data-files
                                        read-file-amounts)]
        [concurrent-data.utils]))

(defn read-update-amounts [m filename count-ref]
  (dosync
    (let [file-amounts (read-file-amounts m filename)]
      (commute count-ref
               #(merge-with + % file-amounts)))))

(defn main [data-files agent-count]
  (let [counts (ref {})
        agents (map agent (repeat agent-count {}))]
    (dorun
      (map #(send %1 read-update-amounts %2 counts)
           (cycle agents)
           data-files))
    (doseq [a agents]
      (await a))
    @counts))

#_
(def amounts (main data-files 8))
