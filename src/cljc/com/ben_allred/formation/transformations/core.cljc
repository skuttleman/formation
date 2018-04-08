(ns com.ben-allred.formation.transformations.core)

(declare make)

(defn ^:private map-config [config [k v]]
  (let [config-v (get config k)]
    (cond
      (map? config-v) [k ((make config-v) v)]
      config-v [k (config-v v)]
      :else [k v])))

(defn make [config]
  (fn [m]
    (some->> m
             (map (partial map-config config))
             (into {}))))
