
(ns concurrent-data.watch-debug
  (:use concurrent-data.utils
        [concurrent-data.agents :only (data-files ->int)]
        concurrent-data.watcher))

#_
(use 'concurrent-data.utils
     'concurrent-data.watcher
     '[concurrent-data.agents :only '(->int data-files)])

(def c (ref 0))

(defn debug-watch [watch-key watch-agent old-state new-state]
  (when (< @c 100)
    (let [output (str watch-key
                      ": "
                      (pr-str old-state)
                      " => "
                      (pr-str new-state)
                      \newline)]
      (print output))
    (dosync (send-off c inc))))

(defn watch-debugging [input-files]
  (let [reader (agent
                 (seque
                   (mapcat
                     lazy-read-csv
                     input-files)))
        caster (agent nil)
        sink (agent [])
        counter (ref 0)
        done (ref false)]
    (add-watch caster :counter
               (partial watch-caster counter))
    (add-watch caster :debug debug-watch)
    (send reader read-row caster sink done)
    (wait-for-it 250 done)
    {:results @sink
     :count-watcher @counter}))

#_
(:count-watcher (watch-debugging (take 2 data-files)))

