(ns com.ben-allred.formation.transformations
  (:require
    [com.ben-allred.formation.shared :as shared]))

(defn ^:private combine* [result-a result-b]
  (comp result-b result-a))

(defmulti ^:private transform #'shared/dispatch)

(defmethod ^:private transform :coll
  [[config] model args]
  (cond->> model
    :always (map #(transform config % args))
    (set? model) (set)
    (vector? model) (vec)
    (map? model) (into {})))

(defmethod ^:private transform :map
  [config model args]
  (let [[key-config val-config] (first config)]
    (some->> model
             (map (fn [[k v]] [(transform key-config k args) (transform val-config v args)]))
             (into {}))))

(defmethod ^:private transform :tuple
  [config model args]
  (let [i (if (:com.ben-allred.formation.core/limit-to (meta config))
            (count config)
            (max (count config) (count model)))]
    (some->> (range i)
             (map (fn [i] (transform (nth config i nil) (nth model i nil) args)))
             (vec))))

(defmethod ^:private transform :record
  [config model args]
  (when model)
  (-> model
      (cond->
        (:com.ben-allred.formation.core/limit-to (meta config))
        (select-keys (keys config)))
      (->>
        (map (fn [[k v]] [k (transform (get config k) v args)]))
        (into {}))))

(defmethod ^:private transform :combine
  [[config & configs] model args]
  (loop [[cfg :as more] configs f #(transform config % args)]
    (if (empty? more)
      (transform f model args)
      (recur (rest more) (comp #(transform cfg % args) f)))))

(defmethod ^:private transform :fn
  [config model args]
  (apply config model args))

(defmethod ^:private transform :ifn
  [config model _]
  (config model))

(defmethod ^:private transform :default
  [_ model _]
  model)

(defn transformer [config]
  (fn [model & args]
    (transform config model args)))
