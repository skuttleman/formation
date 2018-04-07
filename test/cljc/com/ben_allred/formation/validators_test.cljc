(ns com.ben-allred.formation.validators-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.formation.validators :as vs]
            [com.ben-allred.formation.test.utils.spies :as spies]))

(deftest nest-test
  (testing "(nest)"
    (let [spy (spies/create)
          nest (vs/nest ::nest-key spy)]
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
          combine (vs/combine validator-1 validator-2 validator-3)]
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
