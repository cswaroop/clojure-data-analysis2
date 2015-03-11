
(ns concurrent-data.agent-errors
  (:require [clojure.stacktrace :as st]))

#_
(require '[clojure.stacktrace :as st])

(defn print-exc [exc]
  (st/print-cause-trace (.getStackTrace exc)))

;; Standard (error-mode :fail).
(def agent-99 (agent 0))
(send agent-99 #(/ 100 %))
(agent-error agent-99)
@agent-99
(restart-agent agent-99 0)

;; With error-mode :continue.
(def agent-99 (agent 0 :error-mode :continue))
(send agent-99 #(/ 100 %))
(agent-error agent-99)
@agent-99

;; With error-handler.
(def agent-99 (agent 0 :error-handler #(prn %2)))
(send agent-99 #(/ 100 %))
(agent-error agent-99)
@agent-99

