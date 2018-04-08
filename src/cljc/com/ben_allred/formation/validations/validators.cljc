(ns com.ben-allred.formation.validations.validators
  (:refer-clojure :exclude [=])
  (:require [com.ben-allred.formation.utils.core :as utils]))

(defn ^:private collect-errors* [remove-fn map-fn s]
  (when-let [errors (->> s
                         (remove remove-fn)
                         (map map-fn)
                         (seq))]
    (into {} errors)))

(defn ^:private collect-errors [remove-fn msg keys]
  (fn [m]
    (collect-errors* (comp remove-fn (or m {}))
                     (juxt identity (constantly [msg]))
                     keys)))

(defn ^:private some-wrapper [pred]
  (fn [value]
    (or (nil? value)
        (utils/silent (pred value)))))

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

(defn required [keys & [msg]]
  (collect-errors some? (or msg "required") keys))

(defn pred [keys pred & [msg]]
  (collect-errors (some-wrapper pred) (or msg "invalid") keys))

(defn = [key-groups & [msg]]
  (fn [m]
    (let [f (->> key-groups
                 (remove (fn [ks]
                           (apply clojure.core/= (map (partial get m) ks))))
                 (mapcat identity)
                 (collect-errors nil? (or msg "not equal")))]
      (f m))))

(defn matches [keys re & [msg]]
  (collect-errors (some-wrapper (partial re-matches re)) (or msg "invalid") keys))

(defn min-length [keys length & [msg]]
  (collect-errors (some-wrapper (comp #(>= % length) count)) (or msg (str "minimum length " length)) keys))

(defn max-length [keys length & [msg]]
  (collect-errors (some-wrapper (comp #(<= % length) count)) (or msg (str "maximum length " length)) keys))

(defn map-of [key-fn val-fn & [msg]]
  (fn [m]
    (collect-errors* (fn [[k v]] (and (key-fn k) (or (nil? v) (val-fn v))))
                     (juxt key (constantly [(or msg "invalid")]))
                     m)))

(defn coll-of [f & [msg]]
  (fn [c]
    (when-not (every? (some-wrapper f) c)
      [(or msg "invalid")])))
