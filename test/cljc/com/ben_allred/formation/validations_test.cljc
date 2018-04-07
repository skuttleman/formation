(ns com.ben-allred.formation.validations-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.formation.validations :as v]
            [com.ben-allred.formation.test.utils.spies :as spies]))

(deftest nest-test
  (testing "(nest)"
    (let [spy (spies/create)
          nest (v/nest ::nest-key spy)]
      (testing "calls validator"
        (spies/reset! spy)
        (nest {::nest-key ::nest-value})

        (is (spies/called-with? spy ::nest-value)))

      (testing "when validator returns no messages"
        (spies/reset! spy)
        (spies/respond-with! spy (constantly []))

        (testing "returns nil"
          (is (nil? (nest {::nest-key ::nest-value})))))

      (testing "when validator returns messages"
        (spies/reset! spy)
        (spies/respond-with! spy (constantly [::message-1 ::message-2]))

        (testing "returns message map"
          (is (= {::nest-key [::message-1 ::message-2]}
                 (nest {}))))))))

(deftest combine-test
  (testing "(combine)"
    (let [validator-1 (spies/create)
          validator-2 (spies/create)
          validator-3 (spies/create)
          combine (v/combine validator-1 validator-2 validator-3)]
      (testing "calls all validators with value"
        (spies/reset! validator-1 validator-2 validator-3)
        (combine ::some-input)

        (is (spies/called-with? validator-1 ::some-input))
        (is (spies/called-with? validator-2 ::some-input))
        (is (spies/called-with? validator-3 ::some-input)))

      (testing "merges values"
        (spies/reset! validator-1 validator-2 validator-3)
        (spies/respond-with! validator-1 (constantly {::1 [::thing-1]}))
        (spies/respond-with! validator-2 (constantly {::2 [::thing-2]}))
        (spies/respond-with! validator-3 (constantly {::3 [::thing-3]}))

        (is (= {::1 [::thing-1] ::2 [::thing-2] ::3 [::thing-3]}
               (combine ::some-input))))

      (testing "deep merges values"
        (spies/reset! validator-1 validator-2 validator-3)
        (spies/respond-with! validator-1 (constantly {::sub-1 {::1 [::sub-1-thing-1]} ::sub-2 nil}))
        (spies/respond-with! validator-2 (constantly {::sub-1 {::2 [::sub-1-thing-2]} ::sub-2 {::2 [::sub-2-thing-2]}}))
        (spies/respond-with! validator-3 (constantly {::sub-1 nil ::sub-2 {::3 [::sub-2-thing-3]}}))

        (is (= {::sub-1 {::1 [::sub-1-thing-1] ::2 [::sub-1-thing-2]}
                ::sub-2 {::2 [::sub-2-thing-2] ::3 [::sub-2-thing-3]}}
               (combine ::some-input))))

      (testing "concatenates messages"
        (spies/reset! validator-1 validator-2 validator-3)
        (spies/respond-with! validator-1 (constantly {::sub {::sub-key [::sub-message-1]} ::key [::message-1]}))
        (spies/respond-with! validator-2 (constantly {::sub {::sub-key [::sub-message-2]} ::key [::message-2]}))
        (spies/respond-with! validator-3 (constantly {::sub {::sub-key [::sub-message-3]} ::key [::message-3]}))

        (is (= {::sub {::sub-key [::sub-message-1 ::sub-message-2 ::sub-message-3]}
                ::key [::message-1 ::message-2 ::message-3]}
               (combine ::some-input))))

      (testing "only includes distinct messages"
        (spies/reset! validator-1 validator-2 validator-3)
        (spies/respond-with! validator-1 (constantly {::sub {::sub-key [::sub-message]} ::key [::message]}))
        (spies/respond-with! validator-2 (constantly {::sub {::sub-key [::sub-message]} ::key [::message]}))
        (spies/respond-with! validator-3 (constantly {::sub {::sub-key [::sub-message]} ::key [::message]}))

        (is (= {::sub {::sub-key [::sub-message]} ::key [::message]}
               (combine ::some-input)))))))

(deftest make-test
  (testing "(make)"
    (let [map-of-k->s-vectors? (spies/create #(let [m (get-in % [:key-3 :sub-key-3 :sub-sub])]
                                                (when-not (and (map? m)
                                                               (every? (fn [[k v]]
                                                                         (and (keyword? k)
                                                                              (vector? v)
                                                                              (every? string? v))) m))
                                                  ["should be a map of keyword -> vector of strings"])))
          gte (spies/create >=)
          v (v/make {:key-1 [[integer?] [gte 12] #(when-not (:key-1 %)
                                                    ["this only happens when not :key-1"])]
                     :key-2 ^:required! [^"a uuid" [uuid?]]
                     :key-3 {:sub-key-1 ^:required! [[string?] ^"length between 8 and 16" [#(<= 8 (count %) 16)]]
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
            (is (= {:key-1 ["invalid" "this only happens when not :key-1"] :key-2 ["required"]}
                   result)))))

      (testing "when passed nil"
        (testing "only returns errors for required data"
          (is (= {:key-2 ["required"] :key-3 {:sub-key-1 ["required"]}}
                 (v nil))))))))
