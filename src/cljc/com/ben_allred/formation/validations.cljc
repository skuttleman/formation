(ns com.ben-allred.formation.validations)

(defmacro ^:private silent [form & [else]]
  `(try ~form
        (catch #?(:clj Throwable :cljs js/Object) _#
          ~else)))

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

(defn ^:private force-coll [val-or-coll]
  (if (coll? val-or-coll) val-or-coll [val-or-coll]))

(defmulti ^:private ->pvalidator fn?)

(defmethod ^:private ->pvalidator true
  [validator]
  (fn [path m]
    (->> (silent (validator m) ["error"])
         (assoc-msgs path))))

(defmethod ^:private ->pvalidator false
  [v]
  (let [msgs (force-coll (or (:tag (meta v)) "invalid"))
        [pred & args] v]
    (fn [path m]
      (when-not (silent (apply pred (get-in m path) args))
        (assoc-msgs path msgs)))))

(defn ^:private wrap-required [pvalidator {:keys [required tag]}]
  (fn [path m]
    (let [value (get-in m path)
          msgs (force-coll (or tag "required"))]
      (if (and required (nil? value))
        (assoc-msgs path msgs)
        (when (some? value)
          (pvalidator path m))))))

(defn ^:private pcombine [& pvalidators]
  (fn [path m]
    (->> pvalidators
         (map #(% path m))
         (apply merge-with deep-into))))

(defn ^:private required->optional [m]
  (->> m
       (map (fn [[k v]]
              (if (map? v)
                [k (required->optional v)]
                [k (vary-meta v #(assoc % :required (not (:optional %))))])))
       (into {})))

(defn ^:private make* [path config]
  (->> config
       (map (fn [[k v]]
              (let [path' (conj path k)]
                (if (map? v)
                  (make* path' v)
                  (-> (apply pcombine (map ->pvalidator v))
                      (wrap-required (meta v))
                      (partial path'))))))
       (apply combine)))

(defn make-optional [config]
  (make* [] config))

(def make-required
  (comp make-optional required->optional))
