(ns com.ben-allred.formation.transformations.transformers
  (:require [com.ben-allred.formation.transformations.core :as t]))

(defn map-of [key-cfg val-cfg]
  (let [key-f (t/make key-cfg)
        val-f (t/make val-cfg)]
    (fn [m]
      (->> m
           (map (fn [[k v]] [(key-f k) (val-f v)]))
           (into {})))))

(defn coll-of [config]
  (let [f (t/make config)]
    (fn [value]
      (if (seq? value)
        (map f value)
        (into (empty value) (map f) value)))))
