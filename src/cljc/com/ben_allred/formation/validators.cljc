(ns com.ben-allred.formation.validators)

(defn nest [k validator]
  (fn [m]
    (let [msgs (validator (get m k))]
      (when (seq msgs)
        {k msgs}))))

(defn combine [& validators]
  (fn [m]
    (->> validators
         (map #(% m))
         (apply merge-with utils/deep-into))))

