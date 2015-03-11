
(ns interop.r.call-fns
  (:import [org.rosuda.REngine REngine]
           [org.rosuda.REngine.Rserve RConnection]))

#_
(import '[org.rosuda.REngine REngine]
        '[org.rosuda.REngine.Rserve RConnection])

(def ^:dynamic *r-cxn* (RConnection.))
(def ds (into [] (.. *r-cxn* (eval "rnorm(10)") (asDoubles))))
(pprint ds)
(pprint (first ds))
(pprint (rest ds))


