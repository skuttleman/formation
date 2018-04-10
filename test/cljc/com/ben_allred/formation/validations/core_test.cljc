(ns com.ben-allred.formation.validations.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.formation.validations.core :as v]
            [com.ben-allred.formation.test.utils.spies :as spies]))

(deftest make-test
  (testing "(make)"
    (testing "when making a validator"
      (testing "from a sequential collection"
        (let [spy-1 (spies/create (constantly ["a problem"]))
              spy-2 (spies/create (constantly ["another problem"]))
              spy-3 (spies/create)
              make (v/make [spy-1 spy-2 spy-3])]
          (testing "returns messages in order combined"
            (is (= ["a problem" "another problem"]
                   (make ::something))))

          (testing "calls all validators"
            (spies/reset! spy-1 spy-2 spy-3)
            (make ::some-data)

            (is (spies/called-with? spy-1 ::some-data))
            (is (spies/called-with? spy-2 ::some-data))
            (is (spies/called-with? spy-3 ::some-data)))))

      (testing "from a map"
        (let [spy-1 (spies/create (constantly ["a problem"]))
              spy-2 (spies/create (constantly ["another problem"]))
              spy-3 (spies/create)
              make (v/make {:key-1 spy-1
                            :key-2 spy-2
                            :key-3 spy-3})]
          (testing "returns messages in order combined"
            (is (= {:key-1 ["a problem"]
                    :key-2 ["another problem"]}
                   (make {:key-1 ::key-1
                          :key-2 ::key-2
                          :key-3 ::key-3
                          ::some ::data}))))

          (testing "calls all validators"
            (spies/reset! spy-1 spy-2 spy-3)
            (make {:key-1 ::key-1
                   :key-2 ::key-2
                   :key-3 ::key-3
                   ::some ::data})

            (is (spies/called-with? spy-1 ::key-1))
            (is (spies/called-with? spy-2 ::key-2))
            (is (spies/called-with? spy-3 ::key-3)))))

      (testing "from a function"
        (let [spy (spies/create (constantly ["an error"]))
              make (v/make spy)]
          (testing "calls the function"
            (spies/reset! spy)
            (make ::some-data)

            (is (spies/called-with? spy ::some-data)))

          (testing "returns the function's response"
            (is (= ["an error"] (make ::some-data))))))

      (testing "from nested maps, collections, and functions"
        (let [spy-1 (spies/create (constantly {:key-1 ["an error"]}))
              spy-2 (spies/create (constantly ["another error"]))
              spy-3 (spies/create)
              spy-4 (spies/create (constantly {:sub-sub ["a sub error"]}))
              spy-5 (spies/create (constantly {:sub-key {:sub-sub ["another sub error"]}}))
              make (v/make [spy-1
                            {:key-1 spy-2
                             :key-2 [{:sub-key spy-3}
                                     {:sub-key spy-4}
                                     spy-5]
                             :key-3 [{}]
                             :key-4 []}])
              data {:key-1 ::key-1
                    :key-2 {:sub-key {:sub-sub ::sub-sub}}
                    ::more ::data}
              result (make data)]
          (testing "calls the validators with the correct data"
            (is (spies/called-with? spy-1 data))
            (is (spies/called-with? spy-2 ::key-1))
            (is (spies/called-with? spy-3 {:sub-sub ::sub-sub}))
            (is (spies/called-with? spy-4 {:sub-sub ::sub-sub}))
            (is (spies/called-with? spy-5 (:key-2 data))))

          (testing "returns the errors"
            (is (= {:key-1 ["an error" "another error"]
                    :key-2 {:sub-key {:sub-sub ["a sub error" "another sub error"]}}}
                   result))))))))
