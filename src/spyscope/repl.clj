(ns spyscope.repl
  "This contains the query functions suitable for inspecting traces
  from the repl."
  (:require [clojure.string :as str]) 
  (:use [spyscope.core :only [trace-storage]]))

(defn- marker-predicate [marker]
  (cond
    (nil? marker)     (constantly true)
    (string? marker)  #{marker}
    (keyword? marker) #{(name marker)}
    (coll? marker)    (set (map name marker))
    :else             (constantly true)))

(defn trace-query-impl [{:keys [re generations marker print?] :or {re #".*" generations 1 print? true}}]
  (let [{:keys [generation trace]} @trace-storage
        generation-min (- generation generations)]
    (let [traces (->> trace
                      (filter #(re-find re (:frame1 %)))
                      (filter #(> (:generation %) generation-min))
                      (filter (comp (marker-predicate marker) :marker)))]
      (if-not print?
        (map #(select-keys % [:message :value]) traces)
        (->> traces
             (map :message)
             (interpose (str/join (repeat 40 "-")))
             (str/join "\n")
             (println))))))

(defn trace-query
  "Prints information about trace results.
  
  With no arguments, this prints every trace from the current generation.
  
  With one numeric argument `generations`, this prints every trace from the previous
  `generations` generations.
  
  With one regex argument `re`, this prints every trace from the current generation
  whose first stack frame matches the regex.
  
  With two arguments, `re` and `generations`, this matches every trace whose stack frame
  matches `re` from the previosu `generations` generations."
  ([]
   (trace-query-impl {}))
  ([map-or-re-or-generations]
   (cond
     (map? map-or-re-or-generations)    (trace-query-impl map-or-re-or-generations)
     (number? map-or-re-or-generations) (trace-query-impl {:generations map-or-re-or-generations})
     :else                              (trace-query-impl :re map-or-re-or-generations)))
  ([re generations]
   (trace-query-impl :re re :generations generations)))

(defn trace-next
  "Increments the generation of future traces."
  []
  (send trace-storage update-in [:generation] inc)
  nil)

(defn trace-clear
  "Deletes all trace data so far (used to reduce memory consumption)"
  []
  (send trace-storage (fn [_] {:trace [] :generation 0}))
  nil)
