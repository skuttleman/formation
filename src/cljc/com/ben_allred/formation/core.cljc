(ns com.ben-allred.formation.core
  (:refer-clojure :exclude [=])
  (:require [com.ben-allred.formation.validations.core :as v]
            [com.ben-allred.formation.validations.validators :as vs]
            [com.ben-allred.formation.transformations.core :as t]
            [com.ben-allred.formation.transformations.transformers :as ts]
            [com.ben-allred.formation.shared.core :as s]))

;; Transformations

(defn make-transformer
  "Makes a transforming function by passing a nested config value. Values can be maps,
  functions, or vectors of maps and/or functions.

  (make-transformer {:person [{:address [{:street [#(string/split % #\"\\s\") (map string/capitalize %) #(string/join \" \" %)]
                                          :state  string/upper-case}
                                         {:zip   string/trim
                                          :state string/trim}]}
                              #(update % :first-name string/capitalize)
                              #(update % :last-name string/capitalize)]
                     :flags  (comp set (transformer-coll keyword))})"
  [config]
  (t/make config))

(defn transformer-map
  "Transforms a map by passing it's keys and values to a key-fn and val-fn respectively."
  [key-fn val-fn]
  (ts/map-of key-fn val-fn))

(defn transformer-coll
  "Transforms a collection of values into a collection of the same type. May lazily
  evaluate transforming lists and seqs."
  [f]
  (ts/coll-of f))



;; Validations

(defn make-validator
  "Makes a validator which takes in a value and returns a seq of one or messages nested at the same
  point in the config tree when a value does not satisfy the validator. A validator function returns
  a sequence of one or more messages (presumably strings) when there are issues or else returns nil.

  (make-validator {:name #(when-not (string? %) [\"name must be a string\"])})"
  [config]
  (v/make config))

(defn required
  "Takes a seq of keys and an optional message and returns a validator which produces
  a message for any key in the map that is nil or missing."
  ([keys]
   (required keys nil))
  ([keys msg]
   (vs/required keys msg)))

(defn pred
  "Takes a seq of keys, a predicate, and an optional message and returns a validator
  which produces a message for any specified key which does not satisfy the the predicate.
  Does not validate nil or missing values."
  ([keys p]
   (pred keys p nil))
  ([keys pred msg]
   (vs/pred keys pred msg)))

(defn =
  "Takes a seq of keys and produces a message when the values of the keys are not equal.
  Does not validate nil or missing values."
  ([keys]
   (= keys nil))
  ([keys msg]
   (vs/= keys msg)))

(defn matches
  "Takes a seq of keys, a regex pattern, and an optional message and returns a validator
  which produces a message when the values of the provided keys do not match the pattern.
  Does not validate nil or missing values."
  ([keys re]
   (matches keys re nil))
  ([keys re msg]
   (vs/matches keys re msg)))

(defn min-length
  "Takes a seq of keys, a number, and an optional message and validates the values of the
  keys have a count greater than or equal to the length.
  Does not validate nil or missing values."
  ([keys length]
   (min-length keys length nil))
  ([keys length msg]
   (vs/min-length keys length msg)))

(defn max-length
  "Takes a seq of keys, a number, and an optional message and validates the values of the
  keys have a count less than or equal to the length.
  Does not validate nil or missing values."
  ([keys length]
   (max-length keys length nil))
  ([keys length msg]
   (vs/max-length keys length msg)))

(defn validator-map
  "Takes a key-pred, val-pred, and an optional message and returns a validator which expects
  its value to be map where all keys pass the key-pred and all values pass the val-pred."
  ([key-pred val-pred]
   (validator-map key-pred val-pred nil))
  ([key-pred val-pred msg]
   (vs/map-of key-pred val-pred msg)))

(defn validator-coll
  "Takes a pred and an optional message and returns a validator which expects its value to
  be a collection where every value passes the predicate."
  ([pred]
   (validator-coll pred nil))
  ([pred msg]
   (vs/coll-of pred msg)))

;; Utilities

(defn m->fn
  "Utility wrapper for maps when you want to use them as a transforming function
  because make-transformer treats maps as nested transformations."
  [m]
  (s/ifn->fn m))

(defn when-some?
  "Utility wrapper for transforming functions that you only want called when the
  value is not nil (i.e. not required).

  (make-transformer {:something (u:when-some? string/upper-case)})"
  [f]
  (s/when-some? f))
