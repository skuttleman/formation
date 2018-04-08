(ns com.ben-allred.formation.tranformations)

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

(defn make [config]
  (fn [m]
    (when m
      (->> m
           (map (fn [[k v]]
                  (if-let [v' (get config k)]
                    (if (map? v')
                      [k ((make v') v)]
                      [k (v' v)])
                    [k v])))
           (into {})))))
