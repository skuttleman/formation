(ns com.ben-allred.formation.shared.core)

(defn ifn->fn [ifn]
  (fn [key]
    (get ifn key)))

(defn when-somep [pred]
  (fn [value]
    (some-> value pred)))
