
(ns interop.r.core
  (:import [org.rosuda.REngine REngine]
           [org.rosuda.REngine.Rserve RConnection]))

;;;; To set up interop with R. I'm basing this on
;;;; http://lmf-ramblings.blogspot.com/2011/06/calling-r-from-clojure.html
;;;
;;; 1. Download and install R.
;;; 2. Download and install RServe and REngine using lein localrepo.
;;;    http://www.rforge.net/Rserve/
;;;    > mvn install:install-file -Dfile=./REngine.jar -DartifactId=REngine -Dversion=1.7.0 -DgroupId=local.repo -Dpackaging=jar
;;;    > mvn install:install-file -Dfile=./Rserve.jar -DartifactId=Rserve -Dversion=1.7.0 -DgroupId=local.repo -Dpackaging=jar
;;; 3. R (This has to be done before using it every time.)
;;;    > install.packages("Rserve")
;;;    > library(Rserve)
;;;    > Rserve()

#_
(import '[org.rosuda.REngine REngine]
        '[org.rosuda.REngine.Rserve RConnection])

(def ^:dynamic *r-cxn* (RConnection.))

