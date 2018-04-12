(ns com.ben-allred.formation.validations.validators
  (:refer-clojure :exclude [=])
  (:require [com.ben-allred.formation.utils.core :as utils :include-macros true]
            [com.ben-allred.formation.validations.core :as v]
            [com.ben-allred.formation.shared.core :as s]))

(defn ^:private collect-errors* [remove-fn map-fn xs]
  (when-let [errors (->> xs
                         (remove remove-fn)
                         (map map-fn)
                         (seq))]
    (into {} errors)))

(defn ^:private collect-errors [pred msg]
  (fn [m]
    (when-not (pred m)
      [msg])))

(defn ^:private some-wrapper [pred]
  (fn [value]
    (or (nil? value)
        (utils/silent (pred value)))))

(defn required [& [msg]]
  (collect-errors some? (or msg "required")))

(defn pred [pred & [msg]]
  (collect-errors (some-wrapper pred) (or msg "invalid")))

(defn = [keys & [msg]]
  (fn [m]
    (when-not (and (seq keys)
                   (apply clojure.core/= (map (partial get m) keys)))
      (collect-errors* (comp not (or m {}))
                       (juxt identity (constantly [(or msg "not equal")]))
                       keys))))

(defn matches [re & [msg]]
  (collect-errors (some-wrapper (partial re-matches re)) (or msg "invalid")))

(defn min-length [length & [msg]]
  (collect-errors (some-wrapper (comp #(>= % length) count))
                  (or msg (str "minimum length " length))))

(defn max-length [length & [msg]]
  (collect-errors (some-wrapper (comp #(<= % length) count))
                  (or msg (str "maximum length " length))))

(defn map-of [key-cfg val-cfg]
  (let [key-v (v/make key-cfg)
        val-v (v/make val-cfg)]
    (fn [m]
      (let [xs (seq m)
            key-validations (->> xs
                                 (map (juxt first (comp key-v first)))
                                 (remove (comp nil? second))
                                 (into {}))
            val-validations (->> xs
                                 (map (juxt first (comp val-v second)))
                                 (remove (comp nil? second))
                                 (into {}))
            validations (merge-with utils/deep-into key-validations val-validations)]
        (when (seq validations)
            (into {} validations))))))

(defn coll-of [config]
  (let [v (v/make config)]
    (fn [coll]
      (let [validations (remove nil? (map v coll))]
        (when (seq validations)
          (if (map? (first validations))
            (apply merge-with utils/deep-into validations)
            (distinct (apply concat validations))))))))

(defn tuple-of [& configs]
  (s/tuple-of (map v/make configs)))
