(ns com.ben-allred.formation.validations-test
  (:require
    [clojure.set :as set]
    [clojure.string :as string]
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.formation.validations :as v]
    [com.ben-allred.formation.core :as f]))

(deftest required-test
  (testing "required"
    (let [validator (v/required "this is required")]
      (testing "produces the message when called with nil"
        (is (= ["this is required"] (validator nil))))

      (testing "produces empty for any other value"
        (are [value] (nil? (validator value))
          ::value
          true
          false
          [])))))

(deftest pred-test
  (testing "pred"
    (let [validator (v/pred zero? "always zero")]
      (testing "produces nil when called with nil"
        (is (nil? (validator nil))))

      (testing "produces the message when the value fails the predicate"
        (are [value] (= ["always zero"] (validator value))
          1
          -1))

      (testing "produces nil when the value satisfies the predicate"
        (is (nil? (validator 0)))))

    (testing "produces the message the predicate throws"
      (let [validator (v/pred #(throw (ex-info "" {})) "thrown")]
        (is (= ["thrown"] (validator ::value)))))))

(deftest combine-test
  (testing "combine"
    (let [validator (v/validator
                      [(v/required "required")
                       (v/pred string? "string")
                       (v/pred #(= % (string/lower-case %)) "lowercase")])]
      (testing "produces nil when the value satisfies all predicates"
        (are [value] (nil? (validator value))
          "lower"
          "123"
          "asdf238947234@#$"
          ""))

      (testing "produces errors when the value does not satisfy one or more predicate"
        (are [value expected] (= expected (validator value))
          nil ["required"]
          :keyword ["string" "lowercase"]
          "ASDFADSF" ["lowercase"]
          "Aasdfkljhejk234234" ["lowercase"])))))

(deftest validator-test
  (testing "validator"
    (testing "when validating records"
      (let [validator (v/validator {:a (v/required "must have an a")
                                    :b (v/pred string? "must be a string")
                                    :c [(v/required "must have a c")
                                        (v/pred #{:a :b :c} "must be a, b, or c")]})]
        (testing "produces nil when everything is valid"
          (are [value] (nil? (validator value))
            {:a :a :b "b" :c :c}
            {:a :a :c :a}))

        (testing "produces errors for values that are not valid"
          (are [value expected] (= expected (validator value))
            {:c :a} {:a ["must have an a"]}
            {:a [] :b :b :c :b} {:b ["must be a string"]}
            {:a "a"} {:c ["must have a c"]}
            {:a "a" :c :d} {:c ["must be a, b, or c"]}
            {:b 3} {:a ["must have an a"]
                    :b ["must be a string"]
                    :c ["must have a c"]}))))

    (testing "when validating nested records"
      (let [validator (v/validator
                        {:a (v/required "must have an a")
                         :b {:c (v/pred keyword? "must be a keyword")
                             :d (v/pred (partial re-matches #"[abc]+") "must be a, b, or c")}
                         :e [{:f (v/required "must have an f")}
                             {:f (v/pred seq "cannot be empty")}]})]

        (testing "produces nil when everything is valid"
          (are [value] (nil? (validator value))
            {:a :a :e {:f [{}]}}
            {:a 0 :b {} :e {:f [nil]}}
            {:a "a" :b {:c :c} :e {:f [:f]}}
            {:a "a" :b {:d "acbab"} :e {:f [:f]}}
            {:a "a" :b {:c :c :d "acbab"} :e {:f [:f]}}))

        (testing "produces errors for values that are not valid"
          (are [value expected] (= expected (validator value))
            {:e {:f (list :f)}} {:a ["must have an a"]}
            {:a () :b {:c "c" :d "abc"} :e {:f ["f"]}} {:b {:c ["must be a keyword"]}}
            {:a "" :b {:c :c :d "dabc"} :e {:f [[]]}} {:b {:d ["must be a, b, or c"]}}
            {:b {:c "c" :d "abcd"}} {:a ["must have an a"]
                                     :b {:c ["must be a keyword"]
                                         :d ["must be a, b, or c"]}
                                     :e {:f ["must have an f"]}}
            {:a [] :e {:f []}} {:e {:f ["cannot be empty"]}}
            nil {:a ["must have an a"]
                 :e {:f ["must have an f"]}}))))

    (testing "when validating tuples"
      (let [validator (v/validator
                        ^::f/tuple-of
                        [(v/pred string? "string")
                         (v/pred (partial set/superset? #{:a :b :c}) "bad selection")
                         [(v/required "needed")
                          (v/pred #{"a"} "always a")]])]
        (testing "produces nil when everything is valid"
          (are [value] (nil? (validator value))
            ["string" #{:a :c} "a"]
            ["string" nil "a"]
            [nil #{:a :b :c} "a"]
            [nil nil "a"]))

        (testing "produces errors for values that are not valid"
          (are [value expected] (= expected (validator value))
            nil [nil nil ["needed"]]
            [] [nil nil ["needed"]]
            [nil nil nil] [nil nil ["needed"]]
            [nil nil "b"] [nil nil ["always a"]]
            [:keyword #{:a :b :c :d}] [["string"] ["bad selection"] ["needed"]]))))

    (testing "when validating nested tuples"
      (let [validator (v/validator
                        ^::f/tuple-of
                        [(v/required "needed")
                         ^::f/tuple-of
                         [(v/pred keyword? "keyword")
                          [(v/pred (comp (partial = 5) count) "length of 5")
                           (v/pred string? "string")]]])]
        (testing "produces nil when everything is valid"
          (are [value] (nil? (validator value))
            [:value]
            [true nil]
            [false []]
            ["value" [:keyword]]
            ["value" [nil "12345"]]
            ["value" [:keyword "12345"]]))

        (testing "produces errors for values that are not valid"
          (are [value expected] (= expected (validator value))
            [nil] [["needed"] nil]
            [:value ["string"]] [nil [["keyword"] nil]]
            [0 [nil "123456"]] [nil [nil ["length of 5"]]]
            [nil [:keyword [1 2 3 4 5]]] [["needed"] [nil ["string"]]]))))))

(deftest vicinity-of-test
  (testing "vicinity-of"
    (let [validator (v/validator
                      [{:a (v/required "needed")}
                       ^::f/vicinity-of
                       {:a (v/pred :b "must have a b")
                        :b [(v/pred :a "must have an a")
                            (v/pred (comp string? :b) "string")]}])]
      (testing "produces nil when everything is valid"
        (is (nil? (validator {:a :a :b "bee"}))))

      (testing "produces errors for values that are not valid"
        (are [value expected] (= expected (validator value))
          nil {:a ["needed"]}
          {} {:a ["needed" "must have a b"]
              :b ["must have an a" "string"]}
          {:a false :b ""} {:b ["must have an a"]}
          {:a :a} {:a ["must have a b"]
                   :b ["string"]}
          {:b :b} {:a ["needed"]
                   :b ["must have an a" "string"]}
          {:b "string"} {:a ["needed"]
                         :b ["must have an a"]})))))

(deftest coll-of-test
  (testing "coll-of"
    (let [validator (v/validator
                      ^::f/coll-of
                      [{:a (v/pred number? "number")
                        :b [(v/required "must have a b")
                            (v/pred (partial re-matches #".*A.*") "an A in it")]}])]
      (testing "produces nil when everything is valid"
        (are [value] (nil? (validator value))
          nil
          []
          [{:a 0 :b "asdfAasdf"}]
          [{:b "A"} {:b "aAa"} {:a 1.1 :b "asdlkfhkjadsfA"} {:a -33 :b "Aaslkdf"}]))

      (testing "produces errors for values that are not valid"
        (are [value expected] (= expected (validator value))
          [{:a :wrong :b "A"}] [{:a ["number"]}]
          [{:b "a"} {}] [{:b ["an A in it"]} {:b ["must have a b"]}]
          [{:b "A"} {:a 3.0 :b "aAa"} {} {:a 1 :b "AAA"}] [nil nil {:b ["must have a b"]} nil])))))

(deftest map-of-test
  (testing "map-of"
    (let [validator (v/validator
                      ^::f/map-of
                      {(v/pred uuid? "uuid")
                       [(v/pred keyword? "keyword")
                        (v/pred #{:a :b :c} "a, b, or c")]})]
      (testing "produces nil when everything is valid"
        (are [value] (nil? (validator value))
          nil
          {}
          {nil nil}
          {#uuid "7311aaf6-c71c-47dd-bac5-0f91b2ee45e5" :b
           #uuid "4f93321c-440b-443a-a90e-db1ed96fd2a9" :a
           #uuid "0b08115c-4b69-485d-b575-9a7ee1b8f4dd" nil
           nil                                          :c}))

      (testing "produces errors for values that are not valid"
        (are [value expected] (= expected (validator value))
          {:a #uuid "a7b5db87-7956-445e-abd8-d5c1aa516578"}
          {:a ["uuid" "keyword" "a, b, or c"]}

          {#uuid "4f93321c-440b-443a-a90e-db1ed96fd2a9" "a"}
          {#uuid "4f93321c-440b-443a-a90e-db1ed96fd2a9" ["keyword" "a, b, or c"]}

          {#uuid "4f93321c-440b-443a-a90e-db1ed96fd2a9" :d}
          {#uuid "4f93321c-440b-443a-a90e-db1ed96fd2a9" ["a, b, or c"]}

          {:a                                           :a
           #uuid "4f93321c-440b-443a-a90e-db1ed96fd2a9" :d
           #uuid "7311aaf6-c71c-47dd-bac5-0f91b2ee45e5" :b}
          {:a                                           ["uuid"]
           #uuid "4f93321c-440b-443a-a90e-db1ed96fd2a9" ["a, b, or c"]})))))

(deftest meta-test
  (testing "meta"
    (let [validator (v/validator
                      ^{::f/meta [(v/required "not nil")
                                  (v/pred (comp #{:c} :c) "c is c")]}
                      {:a (v/pred string? "string")
                       :b ^::f/tuple-of [(v/pred keyword? "keyword") (v/pred number? "number")]})]
      (testing "produces nil when everything is valid"
        (are [value] (nil? (validator value))
          {:a "string" :b [:keyword 3] :c :c}
          {:a "string" :c :c}
          {:b [] :c :c}
          {:c :c}))

      (testing "metadata validators produce errors"
        (are [value expected] (= expected (::f/meta (meta (validator value))))
          nil ["not nil"]
          {:c :d} ["c is c"])))))

(deftest validations-test
  (testing "validations"
    (let [validator (v/validator
                      ^{::f/meta [(v/required "must have a value")
                                  (v/pred (comp (partial = 2) count) "tuple")]}
                      ^::f/tuple-of
                      [(v/pred keyword? "keyword")
                       {:users ^{::f/meta [(v/required "must have users")
                                           (v/pred (comp (partial <= 1) count) "at least one user")]}
                               ^::f/coll-of
                               [{:name
                                 (v/pred string? "name")

                                 :properties
                                 ^::f/map-of
                                 {[(v/required "non-nil key")
                                   (v/pred keyword? "keyword key")]
                                  (constantly nil)}

                                 :favorite-things
                                 ^{::f/meta (v/required "must have things")}
                                 {:color  (v/pred #{:blue :orange :yellow} "invalid color")
                                  :number (v/pred number? "invalid number")
                                  :book   [{:title  (v/pred string? "title")
                                            :author (v/pred string? "author")}
                                           ^::f/vicinity-of
                                           {:title  (v/pred (comp (partial apply not=)
                                                                  (juxt :author :title))
                                                            "cannot be author's name")
                                            :author (v/pred (comp (partial apply not=)
                                                                  (juxt :author :title))
                                                            "cannot be title")}]}}]}])]
      (testing "produces nil when there are no errors"
        (are [value] (nil? (validator value))
          [:keyword {:users [{:name            "a name"
                              :properties      {:key nil}
                              :favorite-things {:color :orange}}]}]
          [nil {:users [{:favorite-things {}}]}]))


      (testing "produces value errors"
        (are [value expected] (= expected (validator value))
          ["string" {:users [{:favorite-things {}}]}]
          [["keyword"] nil]

          [:k {:users [{:favorite-things {}} {:favorite-things {:color :lavender}}]}]
          [nil {:users [nil {:favorite-things {:color ["invalid color"]}}]}]

          [nil {:users [{:favorite-things {:book {:title "johnson" :author "johnson"}}}]}]
          [nil {:users [{:favorite-things {:book {:title  ["cannot be author's name"]
                                                  :author ["cannot be title"]}}}]}]

          [nil {:users [{:favorite-things {:book {}}}]}]
          [nil {:users [{:favorite-things {:book {:title  ["cannot be author's name"]
                                                  :author ["cannot be title"]}}}]}]

          [nil {:users [{:favorite-things {:number :3}}]}]
          [nil {:users [{:favorite-things {:number ["invalid number"]}}]}]

          [:key {:users [{:properties {nil 3} :favorite-things {}}]}]
          [nil {:users [{:properties {nil ["non-nil key"]}}]}]

          [:key {:users [{:properties {"key" 3} :favorite-things {}}]}]
          [nil {:users [{:properties {"key" ["keyword key"]}}]}]))

      (testing "produces metadata errors"
        (are [value path expected] (-> value
                                       (validator)
                                       (cond->
                                         (seq path) (get-in path))
                                       (meta)
                                       (::f/meta)
                                       (= expected))
          nil [] ["must have a value"]
          [] [] ["tuple"]
          [1 2 3] [] ["tuple"]
          [nil nil] [1 :users] ["must have users"]
          [nil {:users []}] [1 :users] ["at least one user"]
          [:k {:users [{:favorite-things {}} {:name "a name"}]}] [1 :users 1 :favorite-things] ["must have things"])))))
