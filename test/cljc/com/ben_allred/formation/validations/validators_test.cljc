(ns com.ben-allred.formation.validations.validators-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.formation.validations.validators :as vs]))

(deftest required-test
  (testing "(required)"
    (testing "when a message is supplied"
      (let [required (vs/required "this is required")]
        (testing "and when a required value is nil"
          (testing "returns the message"
            (is (= ["this is required"] (required nil)))))

        (testing "and when a required value is not nil"
          (testing "returns nil"
            (is (nil? (required "a thing")))))))

    (testing "when no message is supplied"
      (let [required (vs/required)]
        (testing "uses a default message"
          (is (= ["required"] (required nil))))))))

(deftest pred-test
  (testing "(pred)"
    (testing "when a message is supplied"
      (let [pred (vs/pred number? "not a number")]
        (testing "and when the value is nil"
          (let [result (pred nil)]
            (testing "does not return a message"
              (is (nil? result)))))

        (testing "and when the value does not satisfy the predicate"
          (let [result (pred "string")]
            (testing "returns the message"
              (is (= ["not a number"] result)))))

        (testing "and when the value satisfies the predicate"
          (testing "returns nil"
            (is (nil? (pred 1.7)))))))

    (testing "when a message is not supplied"
      (let [pred (vs/pred number?)]
        (testing "returns a default message"
          (is (= ["invalid"] (pred "result"))))))))

(deftest =-test
  (testing "(=)"
    (testing "when a message is supplied"
      (let [equivalent (vs/= [:key-1 :key-2 :key-3] "things should be equal")]
        (testing "and when a key's value is nil"
          (let [result (equivalent {:key-1 "thing" :key-3 nil})]
            (testing "does not return a message for those keys"
              (is (nil? (:key-2 result)))
              (is (nil? (:key-3 result))))))

        (testing "and when the keys' values are not equal"
          (let [result (equivalent {:key-1 "string" :key-2 "a different string"})]
            (testing "returns the message for that key"
              (is (= ["things should be equal"] (:key-1 result)))
              (is (= ["things should be equal"] (:key-2 result))))))

        (testing "and when all keys' values are equal"
          (testing "returns nil"
            (is (nil? (equivalent {:key-1 3 :key-2 3 :key-3 3})))))))

    (testing "when a message is not supplied"
      (let [equivalent (vs/= [:key-1 :key-3])]
        (testing "and when a key-group's values are not equal"
          (let [result (equivalent {:key-1 ::some-value})]
            (testing "returns a default message"
              (is (= ["not equal"] (:key-1 result))))))))

    (testing "when called with nil"
      (testing "treats it like an empty map"
        (is (nil? ((vs/= [:a-key :another-key]) nil)))))))

(deftest matches-test
  (testing "(matches)"
    (testing "when a message is supplied"
      (let [matches (vs/matches #"[A-Z]{8}[0-9]{4}" "does not match")]
        (testing "and when the value is nil"
          (let [result (matches nil)]
            (testing "does not return a message"
              (is (nil? result)))))

        (testing "and when the value does not match the pattern"
          (let [result (matches "string")]
            (testing "returns the message"
              (is (= ["does not match"] result)))))

        (testing "and when the value matches the pattern"
          (testing "returns nil"
            (is (nil? (matches "DJEKDUBK8021")))))))

    (testing "when a message is not supplied"
      (let [matches (vs/matches #"fist")]
        (testing "returns a default message"
          (is (= ["invalid"] (matches "flirt"))))))))

(deftest min-length-test
  (testing "(min-length)"
    (testing "when a message is supplied"
      (let [min-length (vs/min-length 5 "not long enough")]
        (testing "and when the value is nil"
          (testing "does not return a message"
            (is (nil? (min-length nil)))))

        (testing "and when the value is not long enough"
          (testing "returns the message"
            (is (= ["not long enough"] (min-length "xxx")))))

        (testing "and when the value is long enough"
          (testing "returns nil"
            (is (nil? (min-length "a string that is long enough")))))))

    (testing "when a message is not supplied"
      (let [min-length (vs/min-length 12)]
        (testing "and when a key's value is not long enough"
          (let [result (min-length "12")]
            (testing "returns a default message"
              (is (= ["minimum length 12"] result)))))))))

(deftest max-length-test
  (testing "(max-length)"
    (testing "when a message is supplied"
      (let [max-length (vs/max-length 100 "too long")]
        (testing "and when the value is nil"
          (testing "does not return a message"
            (is (nil? (max-length nil)))))

        (testing "and when the value is too long"
          (let [result (max-length (apply str (range 60)))]
            (testing "returns the message"
              (is (= ["too long"] result)))))

        (testing "and when the value is not too long"
          (testing "returns nil"
            (is (nil? (max-length (range 12))))))))

    (testing "when a message is not supplied"
      (let [max-length (vs/max-length 12)]
        (testing "and when the value is not too long"
          (let [result (max-length "asdklflakswjdlkfjdf")]
            (testing "returns a default message"
              (is (= ["maximum length 12"] result)))))))))

(deftest map-of-test
  (testing "(map-of)"
    (let [map-of (vs/map-of [(vs/pred number? "key should be a number")
                             (vs/required "key cannot be nil")]
                            [(vs/pred keyword? "val should be a keyword")
                             (vs/required "val cannot be nil")])]
      (testing "when a key or value does not satisfy the validator"
        (let [result (map-of {nil :thing "string" :value :number 17})]
          (testing "returns the message for that key"
            (is (= ["key cannot be nil"] (get result nil)))
            (is (= ["key should be a number"] (get result "string")))
            (is (= ["key should be a number" "val should be a keyword"] (:number result))))))


      (testing "when all keys and values pass the predicate"
        (testing "returns nil"
          (is (nil? (map-of {1 :one 2 :two 3 :three})))))

      (testing "when called with nil"
        (testing "returns nil"
          (is (nil? (map-of nil))))))))

(deftest coll-of-test
  (testing "(coll-of)"
    (let [coll-of (vs/coll-of (vs/pred number? "numbers only"))]
      (testing "when every value passes the predicate"
        (testing "returns nil"
          (is (nil? (coll-of [1 2.3 -17 5/3])))))

      (testing "when every value fails the predicate"
        (testing "returns the message"
          (is (= ["numbers only"] (coll-of [:a :b :c])))))

      (testing "when some values fail the predicate"
        (testing "returns the message"
          (is (= ["numbers only"] (coll-of [1 2 nil 4 "17"])))))

      (testing "when called with nil"
        (testing "returns nil"
          (is (nil? (coll-of nil))))))))
