(ns com.ben-allred.formation.core
  (:refer-clojure :exclude [identity])
  (:require
    [com.ben-allred.formation.validations :as v]
    [com.ben-allred.formation.transformations :as t]))

;; Validations

(defn validator
  "Create a validating function from a configuration.

  (validator {:a (pred number? \"must be a number\") :b (required \"required\")}"
  [config]
  (v/validator config))

(defn required
  "Creates a validator that produces a msg when a value is nil.

  (required \"this is required\")"
  [msg]
  (v/required msg))

(defn pred
  "Creates a validator that produces a msg when a value does not satisfy the predicate.
  Does not validate nil.

  (pred string? \"needs to be a string\")"
  [p msg]
  (v/pred p msg))

(defn identity
  "Creates a validator that produces a msg when the value does not equal the result of
  passing the value through the identity function. Does not validate nil.

  (identity sort \"must be sorted\")"
  [f msg]
  (v/pred #(= % (f %)) msg))

(defn matches
  "Creates a validator that produces a msg when the value does not match a given regular
  expression or the value is not a string. Does not validate nil.

  (matches #\"[0-9]{4,}\" \"at least 4 digits\")"
  [re msg]
  (v/pred (partial re-matches re) msg))

(defn min-length
  "Creates a validator that produces a msg when the length of the value is not at least
  what is specified. Does not validate nil.

  (min-length 10 \"must be at least 10\")"
  [n msg]
  (v/pred (comp #(>= % n) count) msg))

(defn max-length
  "Creates a validator that produces a msg when the length of the value is not at least
  what is specified. Does not validate nil.

  (max-length 10 \"must be at most 10\")"
  [n msg]
  (v/pred (comp #(<= % n) count) msg))

(defn hydrate
  "Creates a validator that hydrates the stored value for validation.

  (let [validator (hydrate widget-id->widget widget-validator)]
    (validator widget-id))"
  [hydrator config]
  (comp (validator config) hydrator))

(defn whenp
  "Predicate the usage of a validator config spec. The predicate is called with the same
  args as the validator.
  (def validator (whenp < (pred (fn [lesser greater] (< (- greater lesser) 10))
                                \"Second must be within 10 of first\")))
  (validator 100 1)
  ;;=> nil
  (validator 1 5)
  ;;=> nil
  (validator 1 100)
  ;;=> [\"Second must be within 10 of first\"]"
  [pred config]
  (let [v (validator config)]
    (fn [value & args]
      (when (apply v/affirm pred value args)
        (apply v value args)))))



;; Transformations

(defn transformer
  "Creates a transforming function that transforms a value through a data config.

  (transformer {:person {:email string/lower-case :attributes ^::map-of {keyword str}}})"
  [config]
  (t/transformer config))

(defn when-somep
  "Utility wrapper for transforming functions that you only want called when the
  value is not nil (i.e. not required).

  (make-transformer {:something [(when-somep string/trim) (when-somep string/upper-case)]})"
  [p]
  (fn [value]
    (when (some? value)
      (p value))))



;; Deprecated

(defn ^:deprecated make-validator
  "Deprecated. Use `validator` instead."
  [config]
  (validator config))

(defn ^:deprecated validator-map
  "Deprecated. Use ::map-of metadata instead.

  (validator ^::map-of {[(required \"required\") (pred keyword? \"keyword\")] val-validator})"
  [key-config val-config]
  (validator ^::map-of {key-config val-config}))

(defn ^:deprecated validator-coll
  "Deprecated. Use ::coll-of metadata instead.

  (validator ^::coll-of [[(required \"required\") (pred string? \"string\")]])"
  [config]
  (validator ^::coll-of [config]))

(defn ^:deprecated validator-tuple
  "Deprecated. Use ::tuple-of metadata instead.

  (validator ^::tuple-of [(pred number? \"number\") [(required \"required\") (matches #\"[0-9]{4,}\" \"digits\")]])"
  [& configs]
  (validator (with-meta (vec configs) {::tuple-of true})))

(defn ^:deprecated make-transformer
  "Deprecated. Use `transformer` instead."
  [config]
  (transformer config))

(defn ^:deprecated ifn->fn
  "Deprecated. Use ::ifn metadata instead.

  Utility wrapper for maps when you want to use them as a transforming function
  because make-transformer treats maps as nested transformations."
  [ifn]
  (with-meta ifn {::ifn true}))
