
(ns concurrent-data.io
  (:import [java.lang Thread]))

#_
(import [java.lang Thread])

(def counter (ref 0))
(def a1 (agent :a1))
(def a2 (agent :a2))

(defn start-agents [msg a1-sleep a2-sleep]
  (send a1 msg a1-sleep)
  (send a2 msg a2-sleep))

;; Hmm. A set-up like this might be good to illustrate `commute` also.

;; We're going to simulate starving a thread out here.
(defn debug [msg]
  (print (str msg \newline))
  (flush))

(defn starve-out [tag sleep-for]
  (let [retries (atom 0)]
    (dosync
      (let [c @counter]
        (when-not (zero? @retries)
          (debug (str ":starve-out " tag
                      ", :try " @retries
                      ", :counter " c)))
        (swap! retries inc)
        (Thread/sleep sleep-for)
        (ref-set counter (inc c))
        (send *agent* starve-out sleep-for)
        tag))))

;; Some sampled, sorted output for (start-agents starve-out 1000 50)
;; :starve-out :a1, :try 0, :counter 0
;; :starve-out :a1, :try 1, :counter 19
;; :starve-out :a1, :try 2, :counter 39
;; :starve-out :a1, :try 3, :counter 59
;; :starve-out :a1, :try 4, :counter 78
;;
;; This isn't so bad here, but imagine we're pushing data to a database in
;; `debug`. We might end up with a lot of repeated data or errors caused by
;; index clashes. Here's a safer version:

(defn debug! [msg]
  (io! (debug msg)))

(defn starve-errors [tag sleep-for]
  (let [retries (atom 0)]
    (dosync
      (let [c @counter]
        (when-not (zero? @retries)
          (debug! (str ":error-starve " tag ", :try " @retries ", :counter " c)))
        (swap! retries inc)
        (Thread/sleep sleep-for)
        (ref-set counter (inc c))
        (send *agent* starve-out sleep-for)
        tag))))

;; Well, that at least lets us know we're creating problems. (We can see the
;; problems by calling `(mapcat agent-errors [a1 a2])`.
;;
;; How do we fix it?
(defn starve-safe [tag sleep-for]
  (let [retries (atom 0)]
    (dosync
      (let [c @counter]
        (swap! retries inc)
        (Thread/sleep sleep-for)
        (ref-set counter (inc c))))
    (when-not (zero? @retries)
      (debug! (str ":safe-starve " tag
                   ", :try " @retries
                   ", " @counter)))
    (send *agent* starve-safe sleep-for)
    tag))

;; This can take settings that would otherwise produce more retries, like
;; `(start-agents starve-out 5000 5)`.
#_
(do
  (start-agents starve-safe 5000 5)
  (shutdown-agents)
  )

