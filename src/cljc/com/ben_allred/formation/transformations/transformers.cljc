(ns com.ben-allred.formation.transformations.transformers)

(defn m->fn [m]
  (fn [key]
    (get m key)))

(defn when-some? [f]
  (fn [value]
    (some-> value (f))))

(defn map-of [key-f val-f]
  (fn [value]
    (->> value
         (map (fn [[k v]] [(key-f k) (val-f v)]))
         (into {}))))

(defn coll-of [f]
  (fn [value]
    (if (seq? value)
      (map f value)
      (into (empty value) (map f) value))))
