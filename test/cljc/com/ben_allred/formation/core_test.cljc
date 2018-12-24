(ns com.ben-allred.formation.core-test
  (:require
    [clojure.string :as string]
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.formation.core :as f]))

(deftest identity-test
  (testing "identity"
    (let [validator (f/identity string/lower-case "lowercase")]
      (testing "produces nil when called with nil"
        (is (nil? (validator nil))))

      (testing "produces the message when the value does not match the pattern"
        (are [value] (= ["lowercase"] (validator value))
          "aAa"
          "AaA"
          "ASDFDADASF"
          "asdf.123847%#$#Dadfasdfe"))

      (testing "produces nil when the value matches the pattern"
        (are [value] (nil? (validator value))
          ""
          "asdfe"
          "2342342"
          "asdf2_34as-df@#$"))

      (testing "produces the message the value is not a string"
        (is (= ["lowercase"] (validator ::value)))))))

(deftest matches-test
  (testing "matches"
    (let [validator (f/matches #"pattern-[0-9]+" "does not match")]
      (testing "produces nil when called with nil"
        (is (nil? (validator nil))))

      (testing "produces the message when the value does not match the pattern"
        (are [value] (= ["does not match"] (validator value))
          "pattern-"
          ""
          "something-else-pattern-123"
          "pattern-123-something-else"))

      (testing "produces nil when the value matches the pattern"
        (are [value] (nil? (validator value))
          "pattern-1"
          "pattern-111000111"
          "pattern-09887654321"))

      (testing "produces the message the value is not a string"
        (is (= ["does not match"] (validator ::value)))))))

(deftest min-length-test
  (testing "min-length"
    (let [validator (f/min-length 10 "at least 10")]
      (testing "produces nil when called with nil"
        (is (nil? (validator nil))))

      (testing "produces the message when the value is too short"
        (are [value] (= ["at least 10"] (validator value))
          "pattern"
          ""
          [1 2 3 4 5 6 7 8 9]
          {:a 1}))

      (testing "produces nil when the value is long enough"
        (are [value] (nil? (validator value))
          "the quick brown fox"
          [1 2 3 4 5 6 7 8 9 10]
          {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 7 :h 8 :i 9 :j 10}
          "                      "))

      (testing "produces the message the value is not countable"
        (is (= ["at least 10"] (validator ::value)))))))

(deftest max-length-test
  (testing "max-length"
    (let [validator (f/max-length 10 "at most 10")]
      (testing "produces nil when called with nil"
        (is (nil? (validator nil))))

      (testing "produces the message when the value is too long"
        (are [value] (= ["at most 10"] (validator value))
          "the quick brown fox"
          [1 2 3 4 5 6 7 8 9 10 11]
          {:a 1 :b 2 :c 3 :d 4 :e 5 :f 6 :g 7 :h 8 :i 9 :j 10 :k 11}
          "                      "))

      (testing "produces nil when the value is short enough"
        (are [value] (nil? (validator value))
          "pattern"
          ""
          [1 2 3 4 5 6 7 8 9 10]
          {:a 1}))

      (testing "produces the message the value is not countable"
        (is (= ["at most 10"] (validator ::value)))))))
