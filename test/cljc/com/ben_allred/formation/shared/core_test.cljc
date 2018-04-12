(ns com.ben-allred.formation.shared.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.formation.shared.core :as s]
            [com.ben-allred.formation.test.utils.spies :as spies]))

(deftest m->fn-test
  (testing "(m->fn)"
    (testing "returns an fn"
      (is (fn? (s/ifn->fn {}))))

    (testing "looks up key in fn"
      (let [f (s/ifn->fn {:a ::a "b" :bee [:c] {:a :thing}})]
        (is (= ::a (f :a)))
        (is (= :bee (f "b")))
        (is (= {:a :thing} (f [:c])))))))

(deftest when-some?-test
  (testing "(when-some?)"
    (let [spy (spies/create (constantly ::result))
          when-some? (s/when-somep spy)]
      (testing "when value is not nil"
        (spies/reset! spy)

        (let [result (when-some? ::something)]
          (testing "applies value to function"
            (is (spies/called-with? spy ::something)))

          (testing "returns value returned by function"
            (is (= ::result result)))))

      (testing "when value is nil"
        (spies/reset! spy)

        (let [result (when-some? nil)]
          (testing "does not call function"
            (is (spies/never-called? spy)))

          (testing "returns nil"
            (is (nil? result))))))))
