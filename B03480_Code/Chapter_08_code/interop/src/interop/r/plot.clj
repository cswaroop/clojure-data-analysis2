
(ns interop.r.plot
  (:use interop.r.core
        interop.r.matrixes)
  (:import [java.io File]))

(comment
(use 'interop.r.core
     'interop.r.matrixes)
(import [java.io File])
  )

(defn r-plot
  ([data filename] (r-plot data filename *r-cxn*))
  ([data filename r-cxn]
   (.. r-cxn
     (eval (str "png(filename=\""
                (.getAbsolutePath (File. filename))
                "\", height=300, width=500, bg=\"white\")\n"
                "plot(" (->r data) ")\n"
                "dev.off()\n")))))

#_
(r-plot [1.0 1.0 2.0 3.0 5.0 8.0] "fib.png")
