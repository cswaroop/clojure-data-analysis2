
(ns interop.mma.core
  (:use clojuratica)
  (:import [com.wolfram.jlink MathLinkFactory]))

;;;; 08.01. Set up Clojuratica.
;;;
;;; Credit where credit is due:
;;; From http://drcabana.org/2012/10/23/installation-and-configuration-of-clojuratica/
;;
;; 1. Get Stuart Halloway's port of Clojuratica and build it.
;;    > git clone git://github.com/stuarthalloway/Clojuratica.git 
;;    > cd Clojuratica
;;    > ant jar
;; 2. Add Clojuratica to the project's source path. In project.clj:
;;    > :source-paths ["src" "Clojuratica/src/clj"]
;; 3. In Mathematica, evaluation `$Path` to see where to put the `*.m` files.
;;    Kind of randomly, I picked this:
;;    > cp src/mma/*.m ~/Library/Mathematica/Autoload/
;; 4. Find JLink.jar in the Mathematica installed files and add it to the
;;    local repo.
;;    > cd /Applications/Mathematica.app/SystemFiles/Links/JLink
;;    > mvn install:install-file -Dfile=./JLink.jar -DartifactId=JLink -Dversion=9.0 -DgroupId=local.repo -Dpackaging=jar
;; 5. And replace the JLink.jar in the Maven repository with a symlink:
;;    > cd ~/.m2/repository/local/repo/JLink/9.0/
;;    > rm JLink-9.0.jar
;;    > ln -s /Applications/Mathematica.app/SystemFiles/Links/JLink/JLink.jar JLink-9.0.jar
;; 6. Now, for dependencies, add JLink and Clojuratica:
;;    [[local.repo/JLink "9.0"]]
;; 7. Import this module.

(comment
(use 'clojuratica)
(import [com.wolfram.jlink MathLinkFactory])
  )

(defn init-mma [mma-command]
  (defonce math-evaluate
    (math-evaluator
      (doto
        (MathLinkFactory/createKernelLink mma-command)
        (.discardAnswer)))))

(init-mma
  (str "-linkmode launch -linkname "
       "/Applications/Mathematica.app/Contents/MacOS/MathKernel"))

(def-math-macro math math-evaluate)

;; user=> (use 'interop.mma.core)
;; nil
#_
(math (FactorInteger 24))
;; [[2 3] [3 1]]
;; user=> (time (math (FactorInteger 98765432123456789)))
;; "Elapsed time: 64.846 msecs"
;; [[449 1] [494927 1] [444444443 1]]
#_
(time (math (Prime (* 1000 1000 1000))))
;; "Elapsed time: 29.819 msecs"
;; 22801763489


