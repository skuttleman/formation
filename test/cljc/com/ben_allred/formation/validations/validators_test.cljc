(ns com.ben-allred.formation.validations.validators-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.formation.validations.validators :as vs]
            [com.ben-allred.formation.test.utils.spies :as spies]))

(deftest required-test
  (testing "(required)"
    (testing "when a message is supplied"
      (let [required (vs/required [:key-1 :key-3] "this is required")]
        (testing "and when a required key's value is nil"
          (testing "returns the message for that key"
            (is (= ["this is required"] (:key-1 (required {:key-3 "something" :key-2 "also a thing"}))))))

        (testing "and when multiple required keys are nil"
          (testing "returns the message for each required key"
            (let [result (required {:key-2 "doesn't matter"})]
              (is (= ["this is required"] (:key-1 result)))
              (is (= ["this is required"] (:key-3 result))))))

        (testing "and when all required keys' values are not nil"
          (testing "returns nil"
            (is (nil? (required {:key-1 "a thing" :key-3 "a thing"})))))))

    (testing "when no message is supplired"
      (let [required (vs/required [:key-1 :key-3])]
        (testing "uses default message"
          (let [result (required {:key-2 "doesn't matter"})]
            (is (= ["required"] (:key-1 result)))
            (is (= ["required"] (:key-3 result)))))))

    (testing "when called with nil"
      (testing "treats it like an empty map"
        (is (= {:a-key ["required"]}
               ((vs/required [:a-key]) nil)))))))

(deftest pred-test
  (testing "(pred)"
    (testing "when a message is supplied"
      (let [pred (vs/pred [:key-1 :key-3] number? "not a number")]
        (testing "and when a key's value is nil"
          (let [result (pred {:key-1 nil :key-2 ::whatever})]
            (testing "does not return a message for those keys"
              (is (nil? result)))))

        (testing "and when a key's value does not satisfy the pred"
          (let [result (pred {:key-1 "string"})]
            (testing "returns the message for that key"
              (is (= ["not a number"] (:key-1 result))))))

        (testing "and when multiple keys' values do not satisfy the pred"
          (let [result (pred {:key-1 :keyword :key-3 true})]
            (testing "returns the message for multiple keys"
              (is (= ["not a number"] (:key-1 result)))
              (is (= ["not a number"] (:key-3 result))))))

        (testing "and when all keys' values satisfy the pred"
          (testing "returns nil"
            (is (nil? (pred {:key-1 1.7 :key-2 ::whatever :key-3 -4/7})))))))

    (testing "when a message is not supplied"
      (let [pred (vs/pred [:key-1 :key-3] number?)]
        (testing "and when a key's value does not satisfy the pred"
          (let [result (pred {:key-1 "43"})]
            (testing "returns a default message"
              (is (= ["invalid"] (:key-1 result))))))))

    (testing "when called with nil"
      (testing "treats it like an empty map"
        (is (nil? ((vs/pred [:a-key] string?) nil)))))))

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
      (let [matches (vs/matches [:key-1 :key-3] #"[A-Z]{8}[0-9]{4}" "does not match")]
        (testing "and when a key's value is nil"
          (let [result (matches {:key-1 nil :key-2 ::whatever})]
            (testing "does not return a message for those keys"
              (is (nil? result)))))

        (testing "and when a key's value does not match the pattern"
          (let [result (matches {:key-1 "string"})]
            (testing "returns the message for that key"
              (is (= ["does not match"] (:key-1 result))))))

        (testing "and when multiple keys' values do not match the pattern"
          (let [result (matches {:key-1 :keyword :key-3 "bad value"})]
            (testing "returns the message for multiple keys"
              (is (= ["does not match"] (:key-1 result)))
              (is (= ["does not match"] (:key-3 result))))))

        (testing "and when all keys' values matches the pattern"
          (testing "returns nil"
            (is (nil? (matches {:key-1 "DJEKDUBK8021" :key-2 "DKEJGIEQ1837"})))))))

    (testing "when a message is not supplied"
      (let [matches (vs/matches [:key-1 :key-3] #"fist")]
        (testing "and when a key's value does not match the pattern"
          (let [result (matches {:key-1 "flirt" :key-3 "fist"})]
            (testing "returns a default message"
              (is (= ["invalid"] (:key-1 result))))))))

    (testing "when called with nil"
      (testing "treats it like an empty map"
        (is (nil? ((vs/matches [:a-key] #"join|die") nil)))))))

(deftest min-length-test
  (testing "(min-length)"
    (testing "when a message is supplied"
      (let [min-length (vs/min-length [:key-1 :key-3] 5 "not long enough")]
        (testing "and when a key's value is nil"
          (let [result (min-length {:key-1 nil :key-2 ::whatever})]
            (testing "does not return a message for those keys"
              (is (nil? result)))))

        (testing "and when a key's value is not long enough"
          (let [result (min-length {:key-1 "xxx"})]
            (testing "returns the message for that key"
              (is (= ["not long enough"] (:key-1 result))))))

        (testing "and when multiple keys' values are not long enough"
          (let [result (min-length {:key-1 "abc" :key-3 [1 2 3 4]})]
            (testing "returns the message for multiple keys"
              (is (= ["not long enough"] (:key-1 result)))
              (is (= ["not long enough"] (:key-3 result))))))

        (testing "and when all keys' value are long enough"
          (testing "returns nil"
            (is (nil? (min-length {:key-1 (range 12) :key-2 ::whatever :key-3 "a string that is long enough"})))))))

    (testing "when a message is not supplied"
      (let [min-length (vs/min-length [:key-1 :key-3] 12)]
        (testing "and when a key's value is not long enough"
          (let [result (min-length {:key-1 "43"})]
            (testing "returns a default message"
              (is (= ["minimum length 12"] (:key-1 result))))))))

    (testing "when called with nil"
      (testing "treats it like an empty map"
        (is (nil? ((vs/min-length [:a-key] string?) nil)))))))

(deftest max-length-test
  (testing "(max-length)"
    (testing "when a message is supplied"
      (let [max-length (vs/max-length [:key-1 :key-3] 100 "too long")]
        (testing "and when a key's value is nil"
          (let [result (max-length {:key-1 nil :key-2 ::whatever})]
            (testing "does not return a message for those keys"
              (is (nil? result)))))

        (testing "and when a key's value is too long"
          (let [result (max-length {:key-1 (apply str (range 60))})]
            (testing "returns the message for that key"
              (is (= ["too long"] (:key-1 result))))))

        (testing "and when multiple keys' values are too long"
          (let [result (max-length {:key-1 (range 101) :key-3 (range 111)})]
            (testing "returns the message for multiple keys"
              (is (= ["too long"] (:key-1 result)))
              (is (= ["too long"] (:key-3 result))))))

        (testing "and when all keys' value are not too long"
          (testing "returns nil"
            (is (nil? (max-length {:key-1 (range 12) :key-2 ::whatever :key-3 "a string that is not too long"})))))))

    (testing "when a message is not supplied"
      (let [max-length (vs/max-length [:key-1 :key-3] 12)]
        (testing "and when a key's value is not too long"
          (let [result (max-length {:key-1 "asdklflakswjdlkfjdf"})]
            (testing "returns a default message"
              (is (= ["maximum length 12"] (:key-1 result))))))))

    (testing "when called with nil"
      (testing "treats it like an empty map"
        (is (nil? ((vs/max-length [:a-key] string?) nil)))))))

(deftest map-of-test
  (testing "(map-of)"
    (testing "when a message is supplied"
      (let [map-of (vs/map-of number? keyword? "numbers and keywords only")]
        (testing "and when a key's value is nil"
          (testing "returns a message for that key"
            (is (= {1 ["numbers and keywords only"]} (map-of {1 nil 2 :thing})))))

        (testing "and when a key fails the predicate"
          (let [result (map-of {nil :thing "string" :value :number 17})]
            (testing "returns the message for that key"
              (is (= ["numbers and keywords only"] (get result nil)))
              (is (= ["numbers and keywords only"] (get result "string")))
              (is (= ["numbers and keywords only"] (:number result))))))

        (testing "and when a key's value fails the predicate"
          (let [result (map-of {1 "something" 4 :fine})]
            (testing "returns the message for that key"
              (is (= {1 ["numbers and keywords only"]} result)))))

        (testing "and when multiple keys and/or values fail the predicate"
          (let [result (map-of {1 "something" :number 3 4 :find})]
            (testing "returns the message for multiple keys"
              (is (= {1 ["numbers and keywords only"] :number ["numbers and keywords only"]}
                     result)))))

        (testing "and when all keys and values pass the predicate"
          (testing "returns nil"
            (is (nil? (map-of {1 :one 2 :two 3 :three})))))))

    (testing "when a message is not supplied"
      (let [map-of (vs/map-of string? vector?)]
        (testing "and when a key or value fails the predicate"
          (let [result (map-of {:whatever :whatever})]
            (testing "returns a default message"
              (is (= ["invalid"] (:whatever result))))))

        (testing "when called with nil"
          (testing "returns nil"
            (is (nil? (map-of nil)))))))))

(deftest coll-of-test
  (testing "(coll-of)"
    (testing "when a message is supplied"
      (let [coll-of (vs/coll-of number? "numbers only")]
        (testing "and when every value passes the predicate"
          (testing "returns nil"
            (is (nil? (coll-of [1 2.3 -17 5/3])))))

        (testing "and when every value fails the predicate"
          (testing "returns the message"
            (is (= ["numbers only"] (coll-of [:a :b :c])))))

        (testing "and when some values fail the predicate"
          (testing "returns the message"
            (is (= ["numbers only"] (coll-of [1 2 nil 4 "17"])))))))

    (testing "when a message is not supplied"
      (let [coll-of (vs/coll-of keyword?)]
        (testing "and when a value fails the predicate"
          (testing "returns a default message"
            (is (= ["invalid"] (coll-of {:what's :this?})))))

        (testing "and when the value is nil"
          (testing "returns nil"
            (is (nil? (coll-of nil)))))))))
