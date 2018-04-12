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
  "Transforms a map by passing it's keys and values through validators made with key-cfg
  and val-cfg via make-transformer."
  [key-cfg val-cfg]
  (ts/map-of key-cfg val-cfg))

(defn transformer-coll
  "Transforms a collection of values into a collection of the same type. May lazily
  evaluate transforming lists and seqs.

  ((transfromer-coll keyword) #{\"a\" \"b\" \"c\"})
  ;; => #{:a :b :c}"
  [config]
  (ts/coll-of config))

(defn transformer-tuple
  "Transforms a tuple though a series of transformers made via make-transformer. Length of
  return value is always the length of configs."
  [& configs]
  (apply ts/tuple-of configs))



;; Validations

(defn make-validator
  "Makes a validator which takes in a value and returns a seq of one or messages nested at
  the same point in the config tree when a value does not satisfy the validator. A validator
  function returns a sequence of one or more messages (presumably strings) when there are
  issues or else returns nil.

  (make-validator {:name #(when-not (string? %) [\"name must be a string\"])})"
  [config]
  (v/make config))

(defn required
  "Takes an optional message and returns a validator which produces
  a message for any key in the map that is nil or missing."
  ([]
   (required nil))
  ([msg]
   (vs/required msg)))

(defn pred
  "Takes a predicate and an optional message and returns a validator which produces
  a message when the value does not satisfy the the predicate. Does not validate nil."
  ([p]
   (pred p nil))
  ([p msg]
   (vs/pred p msg)))

(defn =
  "Takes a seq of keys and produces a message when the values of the keys are not equal.
  Does not validate nil."
  ([keys]
   (= keys nil))
  ([keys msg]
   (vs/= keys msg)))

(defn matches
  "Takes a regex pattern and an optional message and returns a validator
  which produces a message when the value does not match the pattern.
  Does not validate nil."
  ([re]
   (matches re nil))
  ([re msg]
   (vs/matches re msg)))

(defn min-length
  "Takes a number and an optional message and validates that the value has a count greater than
  or equal to the length. Does not validate nil."
  ([length]
   (min-length length nil))
  ([length msg]
   (vs/min-length length msg)))

(defn max-length
  "Takes a number and an optional message and validates the value has a count less than or equal
  to the length. Does not validate nil."
  ([length]
   (max-length length nil))
  ([length msg]
   (vs/max-length length msg)))

(defn validator-map
  "Takes a key-validator and a val-validator returns a validator which expects its value to be map
  where all keys pass the key-validator and all values pass the val-validator. The key and/or val
  error messages are concatenated together under each key with errors."
  [key-validator val-validator]
  (vs/map-of key-validator val-validator))

(defn validator-coll
  "Takes a validator config and returns a validator which expects its value to be a collection
  where every value satisfies the config. Returns a seq of distinct error messages or nil."
  [config]
  (vs/coll-of config))

(defn validator-tuple
  "Validates a tuple though a series of validators made via make-validator. Length of
  return value is always the length of configs."
  [& configs]
  (apply vs/tuple-of configs))



;; Utilities

(defn ifn->fn
  "Utility wrapper for maps when you want to use them as a transforming function
  because make-transformer treats maps as nested transformations."
  [ifn]
  (s/ifn->fn ifn))

(defn when-somep
  "Utility wrapper for transforming functions that you only want called when the
  value is not nil (i.e. not required).

  (make-transformer {:something (u:when-somep string/upper-case)})"
  [pred]
  (s/when-somep pred))
