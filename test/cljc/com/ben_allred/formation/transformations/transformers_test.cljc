(ns com.ben-allred.formation.transformations.transformers-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.formation.transformations.transformers :as ts]
            [com.ben-allred.formation.test.utils.spies :as spies]
            [clojure.string :as string]))

(deftest m->fn-test
  (testing "(m->fn)"
    (testing "returns an fn"
      (is (fn? (ts/m->fn {}))))

    (testing "looks up key in fn"
      (let [f (ts/m->fn {:a ::a "b" :bee [:c] {:a :thing}})]
        (is (= ::a (f :a)))
        (is (= :bee (f "b")))
        (is (= {:a :thing} (f [:c])))))))

(deftest when-some?-test
  (testing "(when-some?)"
    (let [spy (spies/create (constantly ::result))
          when-some? (ts/when-some? spy)]
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

(deftest map-of-test
  (testing "(map-of)"
    (testing "maps keys and values"
      (let [map-of (ts/map-of inc dec)]
        (is (= {2 1 4 3 6 5 8 7}
               (map-of {1 2 3 4 5 6 7 8})))))))

(deftest coll-of-test
  (testing "(coll-of)"
    (let [coll-of (ts/coll-of keyword)]
      (testing "when called with a vector"
        (let [result (coll-of ["1" "2" "3" "4"])]
          (testing "returns a vector"
            (is (vector? result)))

          (testing "retains order"
            (is (= [:1 :2 :3 :4]
                   result)))))

      (testing "when called with a sequential collection"
        (let [result (coll-of (list "a" "b" "c" "d"))]
          (testing "returns a seq"
            (is (seq? result)))

          (testing "retains order"
            (is (= [:a :b :c :d]
                   result)))))

      (testing "when called with a set"
        (testing "returns a set"
          (is (= #{:my :favorite :things}
                 (coll-of #{"my" "favorite" "things"}))))))

    (let [coll-of (ts/coll-of (partial mapv string/lower-case))]
      (testing "when called with a map"
        (testing "returns a map"
          (is (= {"a" "b" "c" "d"}
                 (coll-of {"A" "B" "C" "D"}))))))))
