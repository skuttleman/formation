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

(defn v:make-optional
  "Makes a validator out of a config map in the shape of the data to be validated.
  Validators are vectors of either functions - which take the entire data structure
  and return nil or a sequence of error messages - or vectors of preds of the value at
  the specified point in the map and additional args.

  (v:make-optional {:important ^:required [my-validator [string?]]
                    :something {:thing  ^:required [^\"a number\" [number?]
                                                    ^\"greater than or equal to 5\" [>= 5]]
                                :nested [^{:tag [\"a map\" \"for real, though\"]} [map?]
                                         another-validator]}})

  All validators are skipped on nil or missing values unless the :required meta data
  is set to true.
  An \"invalid\" message is returned for unsatisfied predicates which can be overwritten
  with :tag meta data."
  [config]
  (v/make-optional config))

(defn v:make-required
  "Makes a validator out of a config map in the shape of the data to be validated.
  Validators are vectors of either functions - which take the entire data structure
  and return nil or a sequence of error messages - or vectors of preds of the value at
  the specified point in the map and additional args.

  (v:make-required {:important [my-validator [string?]]
                    :something {:thing  [^\"a number\" [number?]
                                       ^\"greater than or equal to 5\" [>= 5]]
                                :nested ^:optional [^{:tag [\"a map\" \"for real, though\"]} [map?]
                                                    another-validator]}})

  All validators are required unless the :optional meta data value is true and the value is
  missing or nil.
  An \"invalid\" message is returned for unsatisfied predicates which can be overwritten
  with :tag meta data."
  [config]
  (v/make-required config))

(defn v:nest
  "Returns a validator nested in a map with a key.

  (v:nest :a-key a-validator)

  is equivalent to

  (fn [m] (when-let [msgs (a-validator (:a-key m))]
            {:a-key msgs}))"
  [k validator]
  (vs/nest k validator))

(defn v:combine
  "Combines validators and deep merges their distinct messages by concatenating them."
  [& validators]
  (apply vs/combine validators))

(defn v:required
  "Takes a seq of keys and an optional message and returns a validator which produces
  a message for any key in the map that is nil or missing."
  ([keys]
   (v:required keys nil))
  ([keys msg]
   (vs/required keys msg)))

(defn v:pred
  "Takes a seq of keys, a predicate, and an optional message and returns a validator
  which produces a message for any key in the map with a value other than nil and fails
  the predicate."
  ([keys pred]
   (v:pred keys pred nil))
  ([keys pred msg]
   (vs/pred keys pred msg)))

(defn v:=
  "Takes a seq of keys and produces a message when the values of the keys are not equal.
  Errors are not added for missing keys or keys where the value is nil."
  ([keys]
   (v:= keys nil))
  ([keys msg]
   (vs/= keys msg)))

(defn v:matches
  "Takes a seq of keys, a regex pattern, and an optional message and returns a validator
  which produces a message when the values of the provided keys do not match the pattern
  (missing keys or keys where the value is nil are skipped)."
  ([keys re]
   (v:matches keys re nil))
  ([keys re msg]
   (vs/matches keys re msg)))

(defn v:min-length
  "Takes a seq of keys, a number, and an optional message and validates the values of the
  keys have a count greater than or equal to the length."
  ([keys length]
   (v:min-length keys length nil))
  ([keys length msg]
   (vs/min-length keys length msg)))

(defn v:max-length
  "Takes a seq of keys, a number, and an optional message and validates the values of the
  keys have a count less than or equal to the length."
  ([keys length]
   (v:max-length keys length nil))
  ([keys length msg]
   (vs/max-length keys length msg)))

(defn v:map-of
  "Takes a key-pred, val-pred, and an optional message and returns a validator which expects
  its value to be map where all keys pass the key-pred and all values pass the val-pred."
  ([key-pred val-pred]
   (v:map-of key-pred val-pred nil))
  ([key-pred val-pred msg]
   (vs/map-of key-pred val-pred msg)))

(defn v:coll-of
  "Takes a pred and an optional message and returns a validator which expects its value to
  be a collection where every value passes the predicate."
  ([pred]
   (v:coll-of pred nil))
  ([pred msg]
   (vs/coll-of pred msg)))
