(ns com.ben-allred.formation.transformations.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.formation.transformations.core :as t]
            [clojure.string :as string]
            [com.ben-allred.formation.test.utils.spies :as spies]))

(deftest make-test
  (testing "(make)"
    (testing "given a transformation config"
      (let [str-spy (spies/create str)
            set-spy (spies/create set)
            config [#(update-in % [:key-3 :sub-key-3 :sub-sub] name)
                    {:key-1 str-spy
                     :key-2 [keyword]
                     :key-3 [{:sub-key-1 identity
                              :sub-key-2 [(partial map keyword) set-spy]
                              :sub-key-3 {:sub-sub string/upper-case}}
                             #(assoc % :always-here? true)]}]
            make (t/make config)
            data {:key-1 17
                  :key-2 "key-1"
                  :key-3 {:sub-key-1 ::anything
                          :sub-key-2 ["4" "7" "7" "4"]
                          :sub-key-3 {:sub-sub :string}}}]
        (testing "when called with all values"
          (let [result (make data)]
            (testing "transforms root values"
              (is (= "17" (:key-1 result)))
              (is (= :key-1 (:key-2 result))))

            (testing "transforms nested values"
              (is (= ::anything (get-in result [:key-3 :sub-key-1])))
              (is (= #{:4 :7} (get-in result [:key-3 :sub-key-2])))
              (is (true? (get-in result [:key-3 :always-here?])))
              (is (= "STRING" (get-in result [:key-3 :sub-key-3 :sub-sub]))))))

        (testing "when called with extra values"
          (let [result (-> data
                           (assoc :key-4 ::key-4)
                           (assoc-in [:key-3 :sub-key-3 :sub-sub-2] ::sub-sub-2)
                           (make))]
            (testing "carries additional root values through"
              (is (= ::key-4 (:key-4 result))))

            (testing "carries additional nested values through"
              (is (= ::sub-sub-2 (get-in result [:key-3 :sub-key-3 :sub-sub-2]))))))

        (testing "when called with missing values"
          (spies/reset! str-spy set-spy)

          (let [result (-> data
                           (dissoc :key-1)
                           (update :key-3 dissoc :sub-key-2)
                           (make))]
            (testing "does not add missing root values"
              (is (not (contains? result :key-1))))

            (testing "does not add missing nested values"
              (is (not (contains? (:key-3 result) :sub-key-2))))

            (testing "does not call missing transformers"
              (is (spies/never-called? str-spy))
              (is (spies/never-called? set-spy)))))))))
