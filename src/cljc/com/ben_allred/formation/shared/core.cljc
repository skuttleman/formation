(ns com.ben-allred.formation.shared.core)

(defn ifn->fn [m]
  (fn [key]
    (get m key)))

(defn when-some? [f]
  (fn [value]
    (some-> value f)))
