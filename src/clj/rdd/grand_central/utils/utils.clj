(ns rdd.grand-central.utils.utils)

(defmacro for-indexed [[item index coll] & body]
  `(for [i# (range (count ~coll))]
     (let [~item (nth ~coll i#)
           ~index i#]
       ~@body)))

(defn spread-across-space
  "Creates a lazy seq of position values based on the max space and the total items to spread across the space
   Example: (spread-across-space 1000 10) => (90 181 272 363 454 545 636 727 818 909)"
  [max-space total]
  (for [i (range (inc total))
        :when (> i 0)]
    (long (* i (/ max-space (inc total))))))