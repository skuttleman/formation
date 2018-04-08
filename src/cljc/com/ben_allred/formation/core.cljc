(ns com.ben-allred.formation.core
  (:require [com.ben-allred.formation.validations.core :as v]
            [com.ben-allred.formation.validations.validators :as vs]
            [com.ben-allred.formation.transformations.core :as t]
            [com.ben-allred.formation.transformations.transformers :as ts]))

;; Transformations

(defn t:make
  "Makes a transforming function by passing a nested config map. The map should
  have a similar shape to the data you intend to transform. Values of the map should
  be transforming functions.

  (t:make {:person {:address    {:street string/upper-case
                                 :city   string/upper-case
                                 :state  string/upper-case}}
           :flags  (comp set (t:coll-of keyword))})"
  [config]
  (t/make config))

(defn t:m->fn
  "Utility wrapper for maps when you want to use them as a transforming function
  because t:make treats maps as nested transformations."
  [m]
  (ts/m->fn m))

(defn t:when-some?
  "Utility wrapper for transforming functions that you only want called when the
  value is not nil.

  (t:make {:something (t:when-some? string/upper-case)})"
  [f]
  (ts/when-some? f))

(defn t:map-of
  "Transforms a map by passing it's keys and values to a key-fn and val-fn respectively."
  [key-fn val-fn]
  (ts/map-of key-fn val-fn))

(defn t:coll-of
  "Transforms a collection of values into a collection of the same type. May lazily
  evaluate transforming lists and seqs."
  [f]
  (ts/coll-of f))

;; Validations
