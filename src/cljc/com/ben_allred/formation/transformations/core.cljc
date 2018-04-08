(ns com.ben-allred.formation.transformations.core)

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
