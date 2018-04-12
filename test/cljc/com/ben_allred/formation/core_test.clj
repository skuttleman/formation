(ns com.ben-allred.formation.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.ben-allred.formation.core :as f]
            [clojure.string :as string]))

(deftest transformations-test
  (testing "Transformations"
    (let [t (f/make-transformer [{:color-code (f/ifn->fn {:blue 1 :red 2 :yellow 3})
                                  :optional   (f/when-somep string/upper-case)
                                  :nest       {:map  [(partial into {} (filter (comp some? val)))
                                                      (f/transformer-map string/trim sort)]
                                               :coll (f/transformer-coll keyword)}}
                                 #(update % :color-code (f/when-somep (partial * -1)))])]

      (testing "transforms data"
        (is (= {:color-code -2
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

        (is (= {:color-code -1
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

        (is (= {:color-code nil
                :nest       {}}
               (t {:color-code nil
                   :nest       {}})))))))

(deftest validator-test
  (testing "Validations"
    (let [v (f/make-validator [{:name (f/pred string? "name should be a string")}
                               {:person-1 {:name            [(f/required "no nameless people")
                                                             (f/max-length 20 "abbreviate, please")
                                                             (f/min-length 3 "no one's name is that short")]
                                           :favorite-letter (f/matches #"[A-Za-z]" "not a letter")
                                           :hair-style      (f/pred #{:toupe :pompadour :mohawk})}}
                               #(when (and (get-in % [:person-1 :name])
                                           (= (get-in % [:person-1 :name])
                                              (get-in % [:person-2 :name])))
                                  {:person-1 {:name ["must be two different people"]}
                                   :person-2 {:name ["must be two different people"]}})])]

      (testing "validates missing data"
        (is (= {:person-1 {:name ["no nameless people"]}}
               (v {}))))

      (testing "validates incorrect data"
        (is (= {:name     ["name should be a string"]
                :person-1 {:name            ["no one's name is that short" "must be two different people"]
                           :favorite-letter ["not a letter"]
                           :hair-style      ["invalid"]}
                :person-2 {:name ["must be two different people"]}}
               (v {:name     :name
                   :person-1 {:name            "Pe"
                              :hair-style      :comb-over
                              :favorite-letter 17}
                   :person-2 {:name "Pe"}})))

        (is (= {:person-1 {:name ["abbreviate, please"]}}
               (v {:name     "a string"
                   :person-1 {:name "Apu Nahasapeemapetilon"}}))))

      (testing "returns nil for valid data"
        (is (nil? (v {:name     "a name"
                      :person-1 {:name "Johnny"}})))))))
