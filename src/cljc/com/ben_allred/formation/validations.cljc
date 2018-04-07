(ns com.ben-allred.formation.validations)

(defn ^:private deep-into [val-1 val-2]
  (if (or (map? val-1) (map? val-2))
    (merge-with deep-into val-1 val-2)
    (distinct (concat val-1 val-2))))

(defn nest [k validator]
  (fn [m]
    (let [msgs (validator (get m k))]
      (when (seq msgs)
        {k msgs}))))

(defn combine [& validators]
  (fn [m]
    (->> validators
         (map #(% m))
         (apply merge-with deep-into))))

(defn ^:private assoc-msgs [path msgs]
  (when (seq msgs)
    (assoc-in {} path msgs)))

(defmulti ^:private ->pvalidator (comp fn? second list))

(defmethod ^:private ->pvalidator true
  [required? validator]
  (fn [path m]
    (let [value (get-in m path)
          msgs (if (and required? (nil? value))
                 ["required"]
                 (try (when (some? value) (validator m))
                      (catch #?(:clj Throwable :cljs js/Object) _
                        ["error"])))]
      (assoc-msgs path msgs))))

(defmethod ^:private ->pvalidator false
  [required? v]
  (let [msg (or (:tag (meta v)) "invalid")
        msgs (if (coll? msg) msg [msg])
        [pred & args] v]
    (fn [path m]
      (let [value (get-in m path)
            msgs (if (and required? (nil? value))
                   ["required"]
                   (try (when (and (some? value) (not (apply pred value args)))
                          msgs)
                        (catch #?(:clj Throwable :cljs js/Object) _
                          msgs)))]
        (assoc-msgs path msgs)))))

(defn ^:private pcombine [& pvalidators]
  (fn [path m]
    (->> pvalidators
         (map #(% path m))
         (apply merge-with deep-into))))

(defn ^:private make* [path config]
  (->> config
       (map (fn [[k v]]
              (let [path' (conj path k)]
                (if (map? v)
                  (make* path' v)
                  (-> pcombine
                      (apply (map (partial ->pvalidator (:required! (meta v))) v))
                      (partial path'))))))
       (apply combine)))

(defn make [config]
  (make* [] config))
