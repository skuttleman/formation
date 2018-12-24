(ns com.ben-allred.formation.validations
  (:require
    [com.ben-allred.formation.shared :as shared]))

(defn ^:private meta* [config]
  (some-> config
          (meta)
          (select-keys #{:com.ben-allred.formation.core/meta})))

(defn ^:private with-meta* [empty value meta]
  (cond
    (and (map? meta) (some? value))
    (with-meta value meta)

    (some? value)
    value

    (map? meta)
    (with-meta empty meta)))

(defn ^:private seq* [coll]
  (when (some some? coll)
    (seq coll)))

(defn ^:private ->fn [p msg]
  (fn [model]
    (try (when-not (p model)
           [msg])
         (catch #?(:clj Throwable :default :default) _
           [msg]))))

(defn ^:private whenp [p]
  (fn [model]
    (if (nil? model)
      true
      (p model))))

(defmulti ^:private validate* #'shared/dispatch)

(defn ^:private combine* [result-a result-b]
  (let [meta-a (meta* result-a)
        meta-b (meta* result-b)]
    (cond
      (nil? result-a)
      result-b

      (nil? result-b)
      result-a

      (and (map? result-a) (map? result-b))
      (with-meta* {}
                  (merge-with combine* result-a result-b)
                  (merge-with combine* meta-a meta-b))

      (and (coll? result-a) (coll? result-b) (every? string? result-a) (every? string? result-b))
      (with-meta* ()
                  (seq (distinct (concat result-a result-b)))
                  (merge-with combine* meta-a meta-b))

      (and (coll? result-a) (coll? result-b))
      (with-meta* []
                  (some-> (map combine* result-a result-b) (seq) (vec))
                  (merge-with combine* meta-a meta-b))

      :else
      result-b)))

(defn ^:private validate [config model]
  (let [m (meta* config)]
    (with-meta*
      (if (coll? config) (empty config) ())
      (validate* config model)
      (when m (validate* (vary-meta m assoc :com.ben-allred.formation.core/vicinity-of true) model)))))

(defmethod ^:private validate* :vicinity-tuple
  [config model]
  (some->> config
           (map (fn [cfg] (validate cfg model)))
           (filter some?)
           (seq*)
           (vec)))

(defmethod ^:private validate* :vicinity-record
  [config model]
  (some->> config
           (map (fn [[k v]] [k (validate v model)]))
           (filter (comp some? second))
           (seq)
           (into {})))

(defmethod ^:private validate* :vicinity-combine
  [[config & configs] model]
  (transduce (map #(validate (vary-meta % assoc :com.ben-allred.formation.core/vicinity-of true) model))
             (completing combine*)
             (validate (vary-meta config assoc :com.ben-allred.formation.core/vicinity-of true) model)
             configs))

(defmethod ^:private validate* :coll
  [[config] model]
  (some->> model
           (map (partial validate config))
           (seq*)
           (vec)))

(defmethod ^:private validate* :map
  [config model]
  (let [[key-config val-config] (first config)]
    (some->> model
             (map (fn [[k v]] [k (combine* (validate key-config k) (validate val-config v))]))
             (filter (comp some? second))
             (seq)
             (into {}))))

(defmethod ^:private validate* :tuple
  [config model]
  (some->> config
           (map-indexed (fn [i cfg] (validate cfg (nth model i nil))))
           (seq*)
           (vec)))

(defmethod ^:private validate* :record
  [config model]
  (some->> config
           (map (fn [[k v]] [k (validate v (get model k))]))
           (filter (comp some? second))
           (seq)
           (into {})))

(defmethod ^:private validate* :combine
  [[config & configs] model]
  (transduce (map #(validate % model))
             (completing combine*)
             (validate config model)
             configs))

(defmethod ^:private validate* :fn
  [config model]
  (config model))

(defmethod ^:private validate* :default
  [_ _]
  nil)

(defn validator [config]
  (partial validate config))

(defn required [msg]
  (->fn some? msg))

(defn pred [p msg]
  (->fn (whenp p) msg))
