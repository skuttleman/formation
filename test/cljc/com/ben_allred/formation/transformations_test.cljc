(ns com.ben-allred.formation.transformations-test
  (:require
    [clojure.string :as string]
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.formation.core :as f]
    [com.ben-allred.formation.transformations :as t]))

(deftest combine-test
  (testing "combine"
    (let [transform (t/transformer
                      [string/trim
                       #(string/replace % #"\s+" "-")
                       string/lower-case
                       keyword])]
      (are [value expected] (= expected (transform value))
        "keyword" :keyword
        "KEYWORD" :keyword
        "A Phrase" :a-phrase
        "   \t what GOes  \nhere? " :what-goes-here?))))

(deftest tuple-of-test
  (testing "tuple-of"
    (let [transform (t/transformer
                      ^::f/tuple-of
                      [keyword string/upper-case inc])]
      (are [value expected] (= expected (transform value))
        ["a" "b" 0] [:a "B" 1]
        [:keyword "UPPER" -1] [:keyword "UPPER" 0]
        ["key" "_lOwEr_" 99 ::extra] [:key "_LOWER_" 100 ::extra]
        [nil "" 3.5] [nil "" 4.5]))))

(deftest coll-of-test
  (testing "coll-of"
    (let [transform (t/transformer ^::f/coll-of [keyword])]
      (are [value expected] (= expected (transform value))
        ["a" "b" "c"] [:a :b :c]
        #{"a" "b" "c"} #{:a :b :c}
        () ()))))

(deftest map-of-test
  (testing "map-of"
    (let [transform (t/transformer ^::f/map-of {keyword string/trim})]
      (are [value expected] (= expected (transform value))
        {} {}
        {:keyword "string"} {:keyword "string"}
        {:a "a" "b" "\tb\t" "c" "   c   \n"} {:a "a" :b "b" :c "c"}))))

(deftest limto-to-test
  (testing "limit-to"
    (testing "with records"
      (let [transform (t/transformer ^::f/limit-to {:a string/upper-case
                                                    :b inc})]
        (are [value expected] (= expected (transform value))
          {} {}
          {:a "just-A"} {:a "JUST-A"}
          {:a "lower" :b 0} {:a "LOWER" :b 1}
          {:a "something" :b 123 :c ::extra} {:a "SOMETHING" :b 124})))

    (testing "with tuples"
      (let [transform (t/transformer
                        ^::f/tuple-of ^::f/limit-to
                        [keyword string/upper-case inc])]
        (are [value expected] (= expected (transform value))
          ["keyword" "UPPER" 0 :extra] [:keyword "UPPER" 1])))))

(deftest transformations-test
  (testing "transformations"
    (let [transform (t/transformer
                      ^::f/tuple-of
                      [[string/lower-case keyword]
                       [{:name string/trim}
                        {:email       string/lower-case
                         :attributes  ^::f/map-of {[string/lower-case keyword] identity}
                         :preferences [set ^::f/coll-of [keyword]]}]])]
      (are [value expected] (= expected (transform value))
        ["KEYWORD" {:name "  my name \t"
                    :email "ME@Example.com"
                    :extra ::extra
                    :attributes {"a" ::anything
                                 "b" {::a 1 ::b 2}
                                 "C" [1 2 3]}
                    :preferences ["a" "b" "c" "b"]}]
        [:keyword {:name "my name"
                   :email "me@example.com"
                   :extra ::extra
                   :attributes {:a ::anything
                                :b {::a 1 ::b 2}
                                :c [1 2 3]}
                   :preferences #{:a :b :c}}]
        ["value" {}]
        [:value {}]))))

(deftest varargs-test
  (testing "var-args"
    (let [transform (t/transformer ^::f/tuple-of [+ -])]
      (are [value args expected] (= expected (apply transform value args))
        [0 0] nil [0 0]
        [1 7] [4] [5 3]
        [-4 4] [2 2] [0 0]))))
