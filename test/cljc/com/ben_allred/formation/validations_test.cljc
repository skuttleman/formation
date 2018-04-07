(ns com.ben-allred.formation.validations-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.formation.validations :as v]
            [com.ben-allred.formation.test.utils.spies :as spies]))

(deftest make-optional-test
  (testing "(make-optional)"
    (let [map-of-k->s-vectors? (spies/create #(let [m (get-in % [:key-3 :sub-key-3 :sub-sub])]
                                                (when-not (and (map? m)
                                                               (every? (fn [[k v]]
                                                                         (and (keyword? k)
                                                                              (vector? v)
                                                                              (every? string? v))) m))
                                                  ["should be a map of keyword -> vector of strings"])))
          gte (spies/create >=)
          v (v/make-optional {:key-1 [[integer?] [gte 12] #(when-not (:key-1 %)
                                                             ["this only happens when not :key-1"])]
                              :key-2 ^:required ^"key-2 is required" [^"a uuid" [uuid?]]
                              :key-3 {:sub-key-1 ^:required [[string?]
                                                             ^"length between 8 and 16" [#(<= 8 (count %) 16)]]
                                      :sub-key-2 [^{:tag ["should be a keyword" "any keyword"]} [keyword?]]
                                      :sub-key-3 {:sub-sub [map-of-k->s-vectors?]}}})
          data {:key-1 35
                :key-2 (java.util.UUID/randomUUID)
                :key-3 {:sub-key-1 "an apple a day"
                        :sub-key-2 :a-thing
                        :sub-key-3 {:sub-sub {:some-key       ["has" "things"]
                                              :some-other-key ["has" "other" "things"]
                                              :a-third-key    ["also" "has" "things"]}}}}]
      (testing "when passed fully valid data"
        (spies/reset! map-of-k->s-vectors?)

        (let [result (v data)]
          (testing "returns nil"
            (is (nil? result)))
          (testing "calls fn validators with entire data structure"
            (is (spies/called-with? map-of-k->s-vectors? data)))))

      (testing "when passed extra data"
        (testing "returns nil"
          (is (nil? (v (assoc data ::extra ::stuff))))))

      (testing "when passed only required valid data"
        (testing "returns nil"
          (is (nil? (v {:key-1 nil
                        :key-2 (java.util.UUID/randomUUID)
                        :key-3 {:sub-key-1 "required string"
                                :sub-key-3 nil}})))))

      (testing "when passed fully invalid data"
        (let [data {:key-1 1
                    :key-2 "not a uuid"
                    :key-3 {:sub-key-1 [:a :sequence :of :keywords]
                            :sub-key-2 (constantly :a-thing)
                            :sub-key-3 {:sub-sub {false "one string"}}}}
              result (v data)]
          (testing "returns errors for :key-1"
            (is (= ["invalid"]
                   (:key-1 result))))

          (testing "returns errors for :key-2"
            (is (= ["a uuid"]
                   (:key-2 result))))

          (testing "returns errors for :key-3 :sub-key-1"
            (is (= ["invalid" "length between 8 and 16"]
                   (get-in result [:key-3 :sub-key-1]))))

          (testing "returns errors for :key-3 :sub-key-2"
            (is (= ["should be a keyword" "any keyword"]
                   (get-in result [:key-3 :sub-key-2]))))

          (testing "returns errors for :key-3 :sub-key-3 :sub-sub"
            (is (= ["should be a map of keyword -> vector of strings"]
                   (get-in result [:key-3 :sub-key-3 :sub-sub]))))))

      (testing "when passed partially invalid data"
        (let [result (v (assoc data :key-1 false :key-2 nil))]
          (testing "only returns errors for incorrect data"
            (is (= {:key-1 ["invalid" "this only happens when not :key-1"] :key-2 ["key-2 is required"]}
                   result)))))

      (testing "when passed nil"
        (testing "only returns errors for required data"
          (is (= {:key-2 ["key-2 is required"] :key-3 {:sub-key-1 ["required"]}}
                 (v nil))))))))

(deftest make-required-test
  (testing "(make-required)"
    (let [map-of-k->s-vectors? (spies/create #(let [m (get-in % [:key-3 :sub-key-3 :sub-sub])]
                                                (when-not (and (map? m)
                                                               (every? (fn [[k v]]
                                                                         (and (keyword? k)
                                                                              (vector? v)
                                                                              (every? string? v))) m))
                                                  ["should be a map of keyword -> vector of strings"])))
          gte (spies/create >=)
          v (v/make-required {:key-1 ^:optional [[integer?] [gte 12] #(when-not (:key-1 %)
                                                                        ["this only happens when not :key-1"])]
                              :key-2 ^"key-2 is required" [^"a uuid" [uuid?]]
                              :key-3 {:sub-key-1 [[string?] ^"length between 8 and 16" [#(<= 8 (count %) 16)]]
                                      :sub-key-2 ^:optional [^{:tag ["should be a keyword" "any keyword"]} [keyword?]]
                                      :sub-key-3 {:sub-sub ^:optional [map-of-k->s-vectors?]}}})
          data {:key-1 35
                :key-2 (java.util.UUID/randomUUID)
                :key-3 {:sub-key-1 "an apple a day"
                        :sub-key-2 :a-thing
                        :sub-key-3 {:sub-sub {:some-key       ["has" "things"]
                                              :some-other-key ["has" "other" "things"]
                                              :a-third-key    ["also" "has" "things"]}}}}]
      (testing "when passed fully valid data"
        (spies/reset! map-of-k->s-vectors?)

        (let [result (v data)]
          (testing "returns nil"
            (is (nil? result)))
          (testing "calls fn validators with entire data structure"
            (is (spies/called-with? map-of-k->s-vectors? data)))))

      (testing "when passed extra data"
        (testing "returns nil"
          (is (nil? (v (assoc data ::extra ::stuff))))))

      (testing "when passed only required valid data"
        (testing "returns nil"
          (is (nil? (v {:key-1 nil
                        :key-2 (java.util.UUID/randomUUID)
                        :key-3 {:sub-key-1 "required string"
                                :sub-key-3 nil}})))))

      (testing "when passed fully invalid data"
        (let [data {:key-1 1
                    :key-2 "not a uuid"
                    :key-3 {:sub-key-1 [:a :sequence :of :keywords]
                            :sub-key-2 (constantly :a-thing)
                            :sub-key-3 {:sub-sub {false "one string"}}}}
              result (v data)]
          (testing "returns errors for :key-1"
            (is (= ["invalid"]
                   (:key-1 result))))

          (testing "returns errors for :key-2"
            (is (= ["a uuid"]
                   (:key-2 result))))

          (testing "returns errors for :key-3 :sub-key-1"
            (is (= ["invalid" "length between 8 and 16"]
                   (get-in result [:key-3 :sub-key-1]))))

          (testing "returns errors for :key-3 :sub-key-2"
            (is (= ["should be a keyword" "any keyword"]
                   (get-in result [:key-3 :sub-key-2]))))

          (testing "returns errors for :key-3 :sub-key-3 :sub-sub"
            (is (= ["should be a map of keyword -> vector of strings"]
                   (get-in result [:key-3 :sub-key-3 :sub-sub]))))))

      (testing "when passed partially invalid data"
        (let [result (v (assoc data :key-1 false :key-2 nil))]
          (testing "only returns errors for incorrect data"
            (is (= {:key-1 ["invalid" "this only happens when not :key-1"] :key-2 ["key-2 is required"]}
                   result)))))

      (testing "when passed nil"
        (testing "only returns errors for required data"
          (is (= {:key-2 ["key-2 is required"] :key-3 {:sub-key-1 ["required"]}}
                 (v nil))))))))
