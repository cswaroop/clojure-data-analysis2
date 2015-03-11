
(ns parallel-data.gpu-calx
  (:use [calx])
  (:import [java.lang Math]))

(comment
(use 'calx)
(import [java.lang Math])
  )

(defn output-points
  ([max-x max-y]
   (let [range-y (range max-y)]
     (mapcat (fn [x] (map #(vector x %) range-y)) (range max-x)))))

(def src
  "// scale from -2.5 to 1.
  float scale_x(float x) {
      return (x / 1000.0) * 3.5 - 2.5;
  }

  // scale from -1 to 1.
  float scale_y(float y) {
      return (y / 1000.0) * 2.0 - 1.0;
  }

  __kernel void escape(
        __global float *out) {
      int i = get_global_id(0);
      int j = get_global_id(1);
      int index = j * get_global_size(0) + i;
      float point_x = scale_x(i);
      float point_y = scale_y(j);
      int max_iterations = 1000;
      int iteration      = 0;
      float x = 0.0;
      float y = 0.0;

      while (x*x + y*y <= 4 && iteration < max_iterations) {
          float tmp_x = (x*x - y*y) + point_x;
          y = (2 * x * y) + point_y;
          x = tmp_x;
          iteration++;
      }

      out[index] = iteration;
  }")

(defn -main
  ([]
   (let [max-x 1000, max-y 1000]
     (with-cl
       (with-program
         (compile-program src)
         (time
           (let [out (wrap (flatten
                             (output-points max-x max-y))
                           :float32-le)]
             (enqueue-kernel :escape (* max-x max-y) out)
             (let [out-seq (vec @(enqueue-read out))]
               (spit "mandelbrot-out.txt" (prn-str out-seq))
               (println
                 "Calculated on " (platform) \/ (best-device))
               (println
                 "Output written to mandelbrot-out.txt")))))))))

