(ns com.ben-allred.formation.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.formation.core :as formation]
            [clojure.string :as string]))

(deftest transformations-test
  (testing "Transformations"
    (let [t (formation/t:make {:color-code (formation/t:m->fn {:blue 1 :red 2 :yellow 3})
                               :optional   (formation/t:when-some? string/upper-case)
                               :nest       {:map  (comp (formation/t:map-of string/trim sort)
                                                        (partial into {} (filter (comp some? val))))
                                            :coll (formation/t:coll-of keyword)}})]
      (testing "transforms data"
        (is (= {:color-code 2
                :optional   "GGG"
                :nest       {:map  {"a" [1 2 3]
                                    "b" [:a :b :c]}
                             :coll #{:things :and :stuff}}}
               (t {:color-code :red
                   :optional   "gGg"
                   :nest       {:map  {"a" [2 3 1]
                                       "b" [:b :a :c]
                                       "c" nil}
                                :coll #{"things" "and" "stuff"}}})))

        (is (= {:color-code 1
                :optional   nil
                :nest       {:map  {}
                             :coll [:things :and :stuff]}}
               (t {:color-code :blue
                   :optional   nil
                   :nest       {:map  {"a" nil "b" nil "c" nil}
                                :coll ["things" "and" "stuff"]}})))

        (is (= {:color-code nil
                :nest       {:coll (list :alone)}}
               (t {:color-code :orange
                   :nest       {:coll (list "alone")}})))

        (is (= {:nest {}}
               (t {:nest {}})))))))

(deftest validations-test
  (testing "Validations"
    (testing "validates data")))
