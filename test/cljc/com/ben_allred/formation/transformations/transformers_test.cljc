(ns com.ben-allred.formation.transformations.transformers-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.formation.transformations.transformers :as ts]
            [com.ben-allred.formation.test.utils.spies :as spies]
            [clojure.string :as string]))



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
