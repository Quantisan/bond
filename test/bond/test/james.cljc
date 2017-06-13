(ns bond.test.james
  (:require #?(:clj [clojure.test :refer (deftest is testing)])
            [bond.james :as bond :include-macros true]
            [bond.test.target :as target])
  #?(:cljs (:require-macros [cljs.test :refer (is deftest testing)])))

(deftest spy-logs-args-and-results []
  (bond/with-spy [target/foo]
    (target/foo 1)
    (target/foo 2)
    (is (= [{:args [1] :return 2}
            {:args [2] :return 4}]
           (bond/calls target/foo)))
    ;; cljs doesn't throw artity exceptions
    #?(:clj (let [exception (is (thrown? clojure.lang.ArityException (target/foo 3 4)))]
              (is (= {:args [3 4] :throw exception}
                     (-> target/foo bond/calls last)))))))

(deftest stub-works []
  (is (= ""
         (with-out-str
           (bond/with-stub [target/bar]
             (target/bar 3))))))

(deftest stub-with-replacement-works []
  (bond/with-stub [target/foo
                   [target/bar #(str "arg is " %)]]
    (testing "stubbing works"
      (is (nil? (target/foo 4)))
      (is (= "arg is 3" (target/bar 3))))
    (testing "spying works"
      (is (= [4] (-> target/foo bond/calls first :args)))
      (is (= [3] (-> target/bar bond/calls first :args))))))


(deftest iterator-style-stubbing-works []
  (bond/with-stub [target/foo
                   [target/bar [#(str "first arg is " %)
                                #(str "second arg is " %)
                                #(str "third arg is " %)]]]
    (testing "stubbing works"
      (is (nil? (target/foo 4)))
      (is (= "first arg is 3" (target/bar 3)))
      (is (= "second arg is 4" (target/bar 4)))
      (is (= "third arg is 5" (target/bar 5))))
    (testing "spying works"
      (is (= [4] (-> target/foo bond/calls first :args)))
      (is (= [3] (-> target/bar bond/calls first :args)))
      (is (= [4] (-> target/bar bond/calls second :args)))
      (is (= [5] (-> target/bar bond/calls last :args))))))

(deftest spying-entire-namespaces-works
  (bond/with-spy-ns [bond.test.target]
    (target/foo 1)
    (target/foo 2)
    (is (= [{:args [1] :return 2}
            {:args [2] :return 4}]
           (bond/calls target/foo)))
    (is (= 0 (-> target/bar bond/calls count)))))

(deftest stubbing-entire-namespaces-works
  (testing "without replacements"
    (bond/with-stub-ns [bond.test.target]
      (is (nil? (target/foo 10)))
      (is (= [10] (-> target/foo bond/calls first :args)))))
  (testing "with replacements"
    (bond/with-stub-ns [[bond.test.target (constantly 3)]]
      (is (= 3 (target/foo 10)))
      (is (= [10] (-> target/foo bond/calls first :args))))))
