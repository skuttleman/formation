(ns com.ben-allred.formation.validations.core
  (:require [com.ben-allred.formation.utils.core :as utils]))

(declare make)

(defn ^:private combine [& validators]
  (fn [value]
    (when-let [validations (->> validators
                                (map #(% value))
                                (remove nil?)
                                (seq))]
      (let [f (if (map? (first validations))
                (partial merge-with utils/deep-into)
                (comp distinct concat))
            result (reduce f validations)]
        (when (seq result)
          result)))))

(defn ^:private map-config [m [k config]]
  (let [value (get m k)
        result (cond
                 (map? config) ((make config) value)
                 (coll? config) ((apply combine (map make config)) value)
                 (ifn? config) (config value))]
    (when (seq result)
      [k result])))

(defn make [config]
  (cond
    (map? config) (fn [m]
                    (when-let [results (->> config
                                            (map (partial map-config m))
                                            (remove nil?)
                                            (seq))]
                      (into {} results)))
    (coll? config) (apply combine (map make config))
    (ifn? config) config
    :else identity))
