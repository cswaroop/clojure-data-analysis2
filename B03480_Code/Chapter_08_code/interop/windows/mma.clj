(ns mma)

(use 'clojuratica)
(import [com.wolfram.jlink MathLinkFactory])
(defn init-mma
  ([mma-command]
   (defonce math-evaluate
     (math-evaluator
       (doto
         (MathLinkFactory/createKernelLink mma-command)
         (.discardAnswer))))))
(init-mma
  (str "-linkmode launch -linkname "
       "\"C:/Program Files/Wolfram Research/Mathematica/9.0/MathKernel.exe\""))
(def-math-macro math math-evaluate)