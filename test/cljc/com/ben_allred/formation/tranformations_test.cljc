(ns com.ben-allred.formation.tranformations-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.formation.tranformations :as t]
            [com.ben-allred.formation.test.utils.spies :as spies]
            [clojure.string :as string]))

(deftest m->fn-test
  (testing "(m->fn)"
    (testing "returns an fn"
      (is (fn? (t/m->fn {}))))

    (testing "looks up key in fn"
      (let [f (t/m->fn {:a ::a "b" :bee [:c] {:a :thing}})]
        (is (= ::a (f :a)))
        (is (= :bee (f "b")))
        (is (= {:a :thing} (f [:c])))))))

(deftest when-some?-test
  (testing "(when-some?)"
    (let [spy (spies/create (constantly ::result))
          when-some? (t/when-some? spy)]
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
      (let [map-of (t/map-of inc dec)]
        (is (= {2 1 4 3 6 5 8 7}
               (map-of {1 2 3 4 5 6 7 8})))))))

(deftest coll-of-test
  (testing "(coll-of)"
    (let [coll-of (t/coll-of keyword)]
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

    (let [coll-of (t/coll-of (partial mapv string/lower-case))]
      (testing "when called with a map"
        (testing "returns a map"
          (is (= {"a" "b" "c" "d"}
                 (coll-of {"A" "B" "C" "D"}))))))))

(deftest make-test
  (testing "(make)"
    (testing "given a transformation config"
      (let [str-spy (spies/create str)
            set-spy (spies/create set)
            config {:key-1 str-spy
                    :key-2 keyword
                    :key-3 {:sub-key-1 identity
                            :sub-key-2 set-spy
                            :sub-key-3 {:sub-sub string/upper-case}}}
            make (t/make config)
            data {:key-1 17
                  :key-2 "key-1"
                  :key-3 {:sub-key-1 ::anything
                          :sub-key-2 [4 7 7 4]
                          :sub-key-3 {:sub-sub "string"}}}]
        (testing "when called with all values"
          (let [result (make data)]
            (testing "transforms root values"
              (is (= "17" (:key-1 result)))
              (is (= :key-1 (:key-2 result))))

            (testing "transforms nested values"
              (is (= ::anything (get-in result [:key-3 :sub-key-1])))
              (is (= #{4 7} (get-in result [:key-3 :sub-key-2])))
              (is (= "STRING" (get-in result [:key-3 :sub-key-3 :sub-sub]))))))

        (testing "when called with extra values"
          (let [result (-> data
                           (assoc :key-4 ::key-4)
                           (assoc-in [:key-3 :sub-key3 :sub-sub-2] ::sub-sub-2)
                           (make))]
            (testing "carries additional root values through"
              (is (= ::key-4 (:key-4 result))))

            (testing "carries additional nested values through"
              (is (= ::sub-sub-2 (get-in result [:key-3 :sub-key3 :sub-sub-2]))))))

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
