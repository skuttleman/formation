# formation

A Clojure library for data validation and transformation.

[![Clojars Project](https://img.shields.io/clojars/v/com.ben-allred/formation.svg)](https://clojars.org/com.ben-allred/formation)

## Usage

In your `project.clj` add to your dependencies:

> [com.ben-allred/formation "0.4.2"]

Creating and using validators and transformers is nesting functions that transform small parts of your data into a data
model that resembles your actual data model.

Suppose your data looks like this:
```clojure
{:property-1 "value"
 :property-2 "sub value"
 :uuid->num  {"f6585ce6-3fe1-4bb2-9312-9c04190e259d" 92838.0
              "97aa2e62-50c2-433e-a9b8-c52ead4f1928" 383743.0}
 :nested     {:people [{:name "Sir Lancelot" :favorite-color "blue"}
                       {:name "Sir Galahad" :favorite-color "yellow"}]}}
```

You could setup a transformer that might look like this:
```clojure
(require '[com.ben-allred.formation.core :as f])

(def my-transformer (f/make-transformer
                      {:property-1 string/upper-case
                       :property-2 string/upper-case
                       :uuid->num  (f/transformer-map #(java.util.UUID/fromString %) int)
                       :nested     {:people (f/transformer-coll (f/make-transformer
                                                                  {:favorite-color keyword}))}}))
```

And your validator might look like this:
```clojure
(require '[com.ben-allred.formation.core :as f])

(def my-validator (f/make-validator
                    {:property-1 [(f/required) (f/pred string?)]
                     :property-2 (f/pred string?)
                     :uuid->num  (f/validator-map (f/pred uuid? "ids should be UUIDs")
                                                  (f/pred pos? "be more positive"))
                     :nested     {:people (f/validator-coll
                                            (f/make-validator
                                              {:name           (f/pred string?)
                                               :favorite-color (f/pred #{:blue :yellow :orange :aquamarine})}))}}))
```

When you transform your data, you get this:
```clojure
(my-transformer {:property-1 "value"
                 :property-2 "sub value"
                 :uuid->num  {"f6585ce6-3fe1-4bb2-9312-9c04190e259d" 92838.0
                              "97aa2e62-50c2-433e-a9b8-c52ead4f1928" 383743.0}
                 :nested     {:people [{:name "Sir Lancelot" :favorite-color "blue"}
                                       {:name "Sir Galahad" :favorite-color "yellow"}]}})
;; => {:property-1 "VALUE"
;; =>  :property-2 "SUB VALUE"
;; =>  :uuid->num  {#uuid "f6585ce6-3fe1-4bb2-9312-9c04190e259d" 92838
;; =>               #uuid "97aa2e62-50c2-433e-a9b8-c52ead4f1928" 383743}
;; =>  :nested     {:people [{:name "Sir Lancelot" :favorite-color :blue}
;; =>                        {:name "Sir Galahad" :favorite-color :yellow}]}}
```

And validating the data returns no errors:
```clojure
(my-validator {:property-1 "value"
               :property-2 "sub value"
               :uuid->num  {"f6585ce6-3fe1-4bb2-9312-9c04190e259d" 92838.0
                            "97aa2e62-50c2-433e-a9b8-c52ead4f1928" 383743.0}
               :nested     {:people [{:name "Sir Lancelot" :favorite-color "blue"}
                                     {:name "Sir Galahad" :favorite-color "yellow"}]}})
;; => nil
```

While validating wholly incorrect data would give you multiple errors:
```clojure
(my-validator {:property-2 #"regex"
               :uuid->num  {"not-a-uuid" :not-a-number
                            "still-not-a-uuid" 42}
               :nested     {:people [{:name 37 :favorite-color :chartreuse}]}})
;; => {:property-1 ["required"]
;; =>  :property-2 ["invalid"]
;; =>  :uuid->num {"not-a-uuid"       ("ids should be UUIDs" "be more positive")
;;                 "still-not-a-uuid" ("ids should be UUIDs")}
;; =>  :nested {:people {:name ["invalid"]
;; =>                    :favorite-color ["invalid"]}}}
```

### Validations

There are several functions for creating and combining data structures and
functions into validators. A validator is any function which takes a value and
returns a sequence of zero or more error messages (typical strings).

#### `make-validator`

Takes a nested map, vector, and/or function and returns a validator function
which nests validation messages in a structure that mirrors the structure passed in.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def validator (f/make-validator #(when-not (string? %) ["should be a string"])))

(validator "string")
;; => nil
(validator :not-a-string)
;; => ["should be a string"]

(def another-validator (f/make-validator {:number #(when-not (number? %) ["should be a number"])
                                          :upper-string [#(when-not (string? %) ["should be a string"])
                                                         #(when-not (re-matches #"[A-Z]+" (str %)) ["should be uppercase"])]}))

(another-validator {:number 17 :upper-string "ASDFADSF"})
;; => nil
(another-validator {})
;; => {:number ("should be a number") :upper-string ("should be a string" "should be uppercase")}

```

#### `required`

Takes an optional message and returns a validator which returns a message when the value is nil.

```clojure
(require '[com.ben-allred.formation.core :as f])

((required) "anything")
;; => nil

((required) nil)
;; => ("required")

((required "not nil") nil)
;; ("not nil")
```

#### `pred`

Takes a predicate and an optional message and returns a validator which validates any
non-nil value against the predicate.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def validator (f/pred pos-int? "positive integer required"))

(validator 23)
;; => nil
(validator :not-a-number)
;; => ("positive integer required")
```

#### `=`

Takes a seq of keys and an optional message and returns a validator which expects a
map and returns errors for any key with a non-nil value that is not equal to the others.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def validator (f/= [:a :b :c] "not equal"))

(validator {:a 1 :b 2 :c nil})
;; => {:a ("not equal") :b ("not equal")}
```

#### `matches`

Takes a regex expression and an optional message and returns a validator which matches any
non-nil value against the regex via `re-matches`.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def validator (f/matches #"[a-z]{8}[0-1]{4}" "8 letters then 4 numbers"))

(validator "iehclsyg9384")
;; => nil
(validator "asdf")
;; => ("8 letters then 4 numbers")
```

#### `min-length`

Takes a length and an optional message and returns a validator which returns a message when
the `count` of the value is less than the length.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def validator (f/min-length 13 "at least 13"))

(validator "asdfadseaslkdjfklaejasdf")
;; => nil
(validator [1 2 3])
;; => ("at least 13")
```

#### `max-length`

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

#### `validator-map`

Takes a key-config and value-config and returns a validator which validates a map
against two validators created via `f/make-validator`. The validator returns a map
of errors with any keys which produced errors.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def validator (f/validator-map [(f/required "key required") (f/pred keyword?)]
                                [(f/required "val required") (f/pred string?)]))

(validator {:fine "string" :bad-val 13 nil nil})
;; => {:bad-val ("invalid") nil ("key required" "val required")}
```

#### `validator-coll`

Takes a config and returns a validator which expects a collection and validates every
value with the config via `f/make-validator`. Returns distinct messages.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def validator (f/validator-coll {:word (f/pred string) :number (f/pred number?)}))

(validator [{:word "string" :number 1} {:word "another string" :number -17.8}])
;; => nil
(validator [{:word :not-a-word :number "72"}])
;; => ("invalid")
```

#### `validator-tuple`

Takes multiple configs and returns a validator which expects a tuple of values which get
validated individually and returned in the order of the configs.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def validator (f/validator-tuple (f/pred number?) {:key (f/pred keyword?) :value (f/pred string?)}))

(validator [13 {:key :a-key :value "some value"}])
;; => [nil nil nil]
(validator ["asdf" {:key "bad" :value "still ok"}])
;; => [("invalid") {:key ("invalid")}]
```

### Transformations

Transformers are just functions that take a value and return a transformed value. A lot
of functions in clojure's core library fall under this category, so the following functions
are used to combine them in various ways.

#### `make-transformer`

Takes a nested config of transformers and returns a single transformer that expects and
returns data in the same shape.

```clojure
(require '[com.ben-allred.formation.core :as f])

(def transformer (f/make-transformer {:upper-string (f/when-somep string/upper-case)
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

(def transformer (f/transformer-map keyword [sort vec]))

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

(def transformer (f/transformer-coll [name string/upper-case first]))

(transformer #{:a :b :c :d :e :f :g})
;; => #{\A \B \C \D \E \F \G}
```

#### `transformer-tuple`

Takes multiple configs and returns a transformer which excepts a collection and produces a vector
with every item transformed through the supplied configs. 

```clojure
(require '[com.ben-allred.formation.core :as f])

(def transformer (f/transformer-tuple keyword seq)

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

#### `ifn->fn`

A helpful utility for wrapping maps and vectors intended to use as a transformer functions because
`make-transformer`, `transformer-coll`, and `transformer-tuple` treat vectors and maps as nested
transformers to be combined.

```clojure
(require '[com.ben-allred.formation.core :as f])

((f/ifn->fn {:blue 0 :yellow 1}) :blue)
;; => 0
((f/ifn->fn [:blue :yellow]) 1)
;; => :yellow
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
