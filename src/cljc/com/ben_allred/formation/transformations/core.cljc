(ns com.ben-allred.formation.transformations.core
  (:require [com.ben-allred.formation.shared.core :as s]))

(declare make)

(defn ^:private combine [& transformers]
  (reduce #(comp (make %2) %1) identity transformers))

(defn ^:private map-config [config [k v]]
  (let [component (get config k)]
    [k (cond
         (map? component) ((make component) v)
         (coll? component) ((apply combine (map make component)) v)
         (ifn? component) (component v)
         :else v)]))

(defn make [config]
  (cond
    (map? config) (comp (partial into {})
                        (partial map #(map-config config %)))
    (coll? config) (apply combine (map make config))
    (ifn? config) config
    :else identity))
