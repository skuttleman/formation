# formation

A Clojure library for data validation and transformation.

[![Clojars Project](https://img.shields.io/clojars/v/com.ben-allred/formation.svg)](https://clojars.org/com.ben-allred/formation)

## Usage

In your `project.clj` add to your dependencies:

> [com.ben-allred/formation "0.5.0"]

Takes a length and an optional message and returns a validator which returns a message when
the `count` of the value is greater than the length.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def validator (f/max-length 13 "no more than 13"))

(validator [1 2 3])
;; => nil
(validator "elkajsdkfeliasjdelfkasjdlkfjesiljfslajkef")
;; => ("no more than 13")
```

#### `meta validations`

New with `0.5.0` you can use `::meta` for validating a collection in its entirety. Since these errors
exist outside of the structure of the collection, it is useful to describe and access these errors
separate from validations inside the collection.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def validator (f/validator
                 ^{::f/meta [(f/min-length 1 "at least one") (f/required "required")]}
                 ^::f/coll-of [(f/pred string? "string")]))

(validator ["a" "b" "c"])
;; => nil

(::f/meta (meta (validator nil)))
;; => ("required")

(::f/meta (meta (validator [])))
;; => ("at least one")
```

#### `validator-vicinity`

Given validator configs for a `record`s or `tuple`s (does not work with `map` and `coll` validators), creates
a validator whose entire value is passed to each root-level validator to determine its validation. In the
example `{:a a-validator :b b-validator}`, both `a-validator` and `b-validator` are called with the entire record.
Similarly `[first-validator second-validator]` causes both `first-validator` and `second-validator` to be called
with the entire tuple.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def validator (f/validator ^::f/vicinity-of {:a (f/pred :b "requires b") :b (f/pred :a "requires a")}))

(validator {:a 1 :b 2}
;; => nil

(validator {})
;; => {:a ("requires b") :b ("requires a")}
```

#### `validator-map`

Takes a key-config and value-config and returns a validator which validates a map
against two validators created via `f/validator`. The validator returns a map
of errors with any keys which produced errors.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def validator (f/validator
                 ^::f/map-of
                 {[(f/required "key required") (f/pred keyword? "keyword")]
                  [(f/required "val required") (f/pred string? "string")]}))

(validator {:fine "string" :bad-val 13 nil nil})
;; => {:bad-val ("string") nil ("key required" "val required")}
```

#### `validator-coll`

Takes a config and returns a validator which expects a collection and validates every
value with the config via `f/validator`. Returns distinct messages.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def validator (f/validator ^::f/coll-of [{:word (f/pred string? "invalid") :number (f/pred number? "invalid")}]))

(validator [{:word "string" :number 1} {:word "another string" :number -17.8}])
;; => nil
(validator [{:word :not-a-word :number "72"}])
;; => [{:word ("invalid") :number ("invalid")}]
```

#### `validator-tuple`

Takes multiple configs and returns a validator which expects a tuple of values which get
validated individually and returned in the order of the configs.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def validator (f/validator ^::f/tuple-of [(f/pred number? "number") {:key (f/pred keyword? "keyword") :value (f/pred string? "invalid")}]))

(validator [13 {:key :a-key :value "some value"}])
;; => [nil nil nil]
(validator ["asdf" {:key "bad" :value "still ok"}])
;; => [("number") {:key ("keyword")}]
```

### Transformations

Transformers are just functions that take a value and return a transformed value. A lot
of functions in clojure's core library fall under this category, so the following functions
are used to combine them in various ways.

#### `transformer`

Takes a nested config of transformers and returns a single transformer that expects and
returns data in the same shape.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def transformer (f/transformer {:upper-string (f/when-somep string/upper-case)
                                 :nested       [{:keyword keyword}
                                                {:boolean boolean}]}))

(transformer {:upper-string "a string"
              :nested {:keyword "keyword"
                       :boolean "truthy"}})
;; => {:upper-string "A STRING"
;; =>  :nested {:keyword :keyword
;; =>           :boolean true}}
(transformer {:upper-string nil
              :nested {:keyword nil
                       :boolean nil}})
;; => {:upper-string nil
;; =>  :nested {:keyword nil
;; =>           :boolean false}}
```

#### `transformer-map`

Takes a key-config and val-config and transforms a map by calling every key and value with
the resulting key-transformer val-transformer.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def transformer (f/transformer ^::f/map-of {keyword [sort vec]}))

(transformer {"a" [1 4 5 2 7 3 6]
              "b" #{:d :g :a :e :b :c :f}})
;; = {:a [1 2 3 4 5 6 7] :b [:a :b :c :d :e :f :g]}
```

#### `transformer-coll`

Takes a config and returns a transformer that expects a collection and transforms it by passing
every item through the resulting transformer. Like `map` except it returns the same collection
type as passed in. 

```clojure
(require '[com.ben-allred.formation.core :as f])

(def transformer (f/transformer ^::f/coll-of [[name string/upper-case first]]))

(transformer #{:a :b :c :d :e :f :g})
;; => #{\A \B \C \D \E \F \G}
```

#### `transformer-tuple`

Takes multiple configs and returns a transformer which excepts a collection and produces a vector
with every item transformed through the supplied configs.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def transformer (f/transformer ^::f/tuple-of [keyword seq]))

(transformer (list "something" {:a 1 :b 2}))
;; => [:something ([:a 1] [:b 2])]
(transformer ["19" "abc"])
;; => [:19 (\a \b \c)]
```

#### `when-somep`

Wraps a transformer which only gets called if the value passed in is not nil.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def transformer (f/when-somep #(string/split % #",")))

(transformer "1,2,3,4")
;; => ("1" "2" "3" "4")
(transformer nil)
;; => nil
```

## Development

```bash
$ git clone
$ cd
$ lein install
$ lein repl :start :port [PORT]
```

## Deploy

Don't forget to bump the version in `project.clj`.

```bash
$ lein do clean, deploy clojars
```

## License

Copyright © 2018 Ben Allred

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
