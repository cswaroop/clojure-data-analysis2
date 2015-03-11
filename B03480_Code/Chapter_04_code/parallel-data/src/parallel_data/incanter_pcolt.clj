
(ns parallel-data.incanter-pcolt
  (use [incanter.core]
       [parallel-data.pmap :only (rand-point
                                   center-dist
                                   output-points
                                   scale-point
                                   get-escape-point)]))

(defn dot-prod-imap
  ([ds]
   (reduce + 0 ($map * [:x :y] ds))))

(defn mc-pi
  ([n]
   (let [points (dataset [:x :y]
                         (repeatedly n rand-point))]
     (double
       (* 4.0
          (/ (nrow
               ($where {:col-0 {:$lte 1.0}}
                  (conj-cols points
                             ($map #(center-dist [%1 %2]) [:x :y]
                                points))))
             n))))))

;;; Mandelbrot set example

(defn ->scaled-ds
  ([max-x max-y set-range pixel-x pixel-y]
   (zipmap [:scaled-x :scaled-y]
           (scale-point pixel-x pixel-y max-x max-y set-range))))

(defn ->escape-ds
  ([max-iterations scaled-x scaled-y]
   {:escaped-at (get-escape-point scaled-x scaled-y
                                  max-iterations)}))

(defn mandelbrot-imap
  ([max-iterations max-x max-y mandelbrot-range]
   (let [points (dataset [:pixel-x :pixel-y]
                         (output-points max-x max-y))
         scaled (to-dataset
                  ($map (partial ->scaled-ds
                                 max-x max-y mandelbrot-range)
                     [:pixel-x :pixel-y]
                     points))
         mset (to-dataset
                ($map (partial ->escape-ds max-iterations)
                   [:scaled-x :scaled-y]
                   scaled))
         full-set (conj-cols points scaled mset)
         _ (nrow full-set)]
     full-set)))

;;;; Using data from https://explore.data.gov/Foreign-Commerce-and-Aid/U-S-Overseas-Loans-and-Grants-Greenbook-/5gah-bvex.
;;;; U.S. Overseas Loans and Grants.
(comment
(use '(incanter core datasets io optimize charts stats))
(def data-file "data/all_160_in_51.P35.csv")
(def data (to-matrix
            (sel (read-dataset data-file :header true)
                 :cols [:POP100 :HU100])))
(def population (sel data :cols 0))
(def housing-units (sel data :cols 1))

(def lm (linear-model housing-units population))

(def plot (scatter-plot population housing-units :legend true))
(add-lines plot population (:fitted lm))
(view plot)
  )

