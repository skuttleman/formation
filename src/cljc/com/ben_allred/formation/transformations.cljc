(ns com.ben-allred.formation.transformations
  (:require
    [com.ben-allred.formation.shared :as shared]))

(defn ^:private combine* [result-a result-b]
  (comp result-b result-a))

(defmulti ^:private transform #'shared/dispatch)

(defmethod ^:private transform :coll
  [[config] model]
  (cond->> model
    :always (map (partial transform config))
    (set? model) (set)
    (vector? model) (vec)
    (map? model) (into {})))

(defmethod ^:private transform :map
  [config model]
  (let [[key-config val-config] (first config)]
    (some->> model
             (map (fn [[k v]] [(transform key-config k) (transform val-config v)]))
             (into {}))))

(defmethod ^:private transform :tuple
  [config model]
  (let [i (max (count config) (count model))]
    (some->> (range i)
             (map (fn [i] (transform (nth config i nil) (nth model i nil))))
             (vec))))

(defmethod ^:private transform :record
  [config model]
  (some->> model
           (map (fn [[k v]] [k (transform (get config k) v)]))
           (into {})))

(defmethod ^:private transform :combine
  [[config & configs] model]
  (loop [[cfg :as more] configs f (partial transform config)]
    (if (empty? more)
      (f model)
      (recur (rest more) (comp (partial transform cfg) f)))))

(defmethod ^:private transform :fn
  [config model]
  (config model))

(defmethod ^:private transform :ifn
  [config model]
  (config model))

(defmethod ^:private transform :default
  [_ model]
  model)

(defn transformer [config]
  (partial transform config))
