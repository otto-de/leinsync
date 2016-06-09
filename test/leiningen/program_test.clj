(ns leiningen.program-test
  (:require [clojure.test :refer :all]
            [leiningen.utils :as u]
            [leiningen.sync :as s]))

(deftest ^:unit test-split-string
  (is (= ["project1" "project2" "project3"]
         (u/split "project1,project2,project3")))

  (is (= ["de.otto.one.cool.ns1" "de.otto.one.cool.ns2"]
         (u/split "de.otto.one.cool.ns1,de.otto.one.cool.ns2"))))

(deftest ^:unit test-format-str
  (is (= "123  "
         (u/format-str "123" 5)))

  (is (= "123.."
         (u/format-str "1234567" 5)))

  (is (= "123456"
         (u/format-str "123456" 6))))

(deftest ^:unit find-command
  (let [m {:a (fn [x y] (+ x y))
           :b (fn [x y] (* x y))
           :c (fn [_ _] (throw RuntimeException))}
        [fn-1 fn-2 fn-3] (s/find-command [:a :b :c] m)]
    (is (= 3 (fn-1 1 2)))
    (is (= 2 (fn-2 1 2)))
    (is (= nil (fn-3 1 2)))))

(deftest ^:unit ->commands
  (testing "find correct commands"
    (let [m {:a (fn [x y] (+ x y))
             :b (fn [x y] (* x y))}
          commands (s/->commands {:a "" :b ""} m)]
      (is (= 2 (count commands)))))

  (testing "returns fallback if no command found"
    (let [m {:default [:a]
             :a       (fn [x y] (+ x y))
             :b       (fn [x y] (* x y))}
          [fn-default & _] (s/->commands {:a "" :b ""} m)]
      (is (= 3 (fn-default 1 2))))))