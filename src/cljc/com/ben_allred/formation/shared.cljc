(ns com.ben-allred.formation.shared)

(defn dispatch [config _]
  (let [{:keys [com.ben-allred.formation.core/coll-of
                com.ben-allred.formation.core/ifn
                com.ben-allred.formation.core/map-of
                com.ben-allred.formation.core/tuple-of
                com.ben-allred.formation.core/vicinity-of]}
        (meta config)]
    (cond
      (and ifn (ifn? config)) :ifn
      (and vicinity-of (map? config)) :vicinity-record
      (and vicinity-of tuple-of (coll? config) (sequential? config) (seq config)) :vicinity-tuple
      (and vicinity-of (coll? config) (seq config)) :vicinity-combine
      (and coll-of (coll? config) (sequential? config) (= 1 (count config))) :coll
      (and map-of (map? config) (= 1 (count config))) :map
      (and tuple-of (coll? config) (sequential? config) (seq config)) :tuple
      (and (map? config) (seq config)) :record
      (and (coll? config) (seq config)) :combine
      (fn? config) :fn
      (ifn? config) :ifn)))
