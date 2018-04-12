(ns com.ben-allred.formation.validations.validators
  (:refer-clojure :exclude [=])
  (:require [com.ben-allred.formation.utils.core :as utils :include-macros true]
            [com.ben-allred.formation.validations.core :as v]))

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

(defn map-of [key-fn val-fn & [msg]]
  (fn [m]
    (collect-errors* (fn [[k v]] (and (key-fn k) (val-fn v)))
                     (juxt key (constantly [(or msg "invalid")]))
                     m)))

(defn coll-of [f & [msg]]
  (fn [c]
    (when-not (every? f c)
      [(or msg "invalid")])))
