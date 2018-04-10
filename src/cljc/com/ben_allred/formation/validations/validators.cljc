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

(defn required [keys & [msg]]
  (collect-errors some? (or msg "required") keys))

(defn pred [keys pred & [msg]]
  (collect-errors (some-wrapper pred) (or msg "invalid") keys))

(defn = [keys & [msg]]
  (fn [m]
    (when-not (and (seq keys)
                   (apply clojure.core/= (map (partial get m) keys)))
      ((collect-errors not (or msg "not equal") keys) m))))

(defn matches [keys re & [msg]]
  (collect-errors (some-wrapper (partial re-matches re)) (or msg "invalid") keys))

(defn min-length [keys length & [msg]]
  (collect-errors (some-wrapper (comp #(>= % length) count)) (or msg (str "minimum length " length)) keys))

(defn max-length [keys length & [msg]]
  (collect-errors (some-wrapper (comp #(<= % length) count)) (or msg (str "maximum length " length)) keys))

(defn map-of [key-fn val-fn & [msg]]
  (fn [m]
    (collect-errors* (fn [[k v]] (and (key-fn k) (val-fn v)))
                     (juxt key (constantly [(or msg "invalid")]))
                     m)))

(defn coll-of [f & [msg]]
  (fn [c]
    (when-not (every? f c)
      [(or msg "invalid")])))
