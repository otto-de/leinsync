(ns leiningen.leinsync.program-test
  (:require [clojure.test :refer :all]
            [leiningen.leinsync.utils :as u]
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
        commands (s/find-command {:a "" :b "" :c ""} m)]
    (is (= 3 (count commands)))))

(deftest ^:unit ->commands
  (testing "find correct commands"
    (let [m {:a (fn [x y] (+ x y))
             :b (fn [x y] (* x y))}
          commands (s/->commands {:a "" :b ""} m)]
      (is (= 2 (count commands)))))

  (testing "returns fallback if no command found"
    (let [m {:default {:a ""}
             :a       (fn [_ x y] (+ x y))
             :b       (fn [_ x y] (* x y))}
          [fn-default & _] (s/->commands {} m)]
      (is (= 3 (fn-default 1 2))))))

(deftest ^:unit sub-str
  (is (= "abcd ..." (u/sub-str "abcde" 4)))
  (is (= "abcde" (u/sub-str "abcde" 7)))
  (is (= " ..." (u/sub-str "abcde" 0))))

(deftest ^:unit includes?
  (is (true? (u/includes? "abcde" "a")))
  (is (false? (u/includes? "abcde" "f"))))

(deftest ^:unit is-success?
  (is (true? (u/is-success? {:exit 0})))
  (is (false? (u/is-success? {:exit 1}))))

(deftest ^:unit output-of
  (is (= "text" (u/output-of {:out "text"})))
  (is (= "a,b,c" (u/output-of {:out "a\nb\nc\n"} ","))))

(deftest ^:unit split-output-of
  (is (= ["a" "b" "c"] (u/split-output-of {:out "a\nb\nc\n"}))))

(deftest ^:unit error-of
  (is (= "text" (u/error-of {:err "text"})))
  (is (= "a,b,c" (u/error-of {:err "a\nb\nc\n"} ","))))

(deftest ^:unit yes-or-no
  (is (true? (u/yes-or-no "y")))
  (is (true? (u/yes-or-no "n")))
  (is (false? (u/yes-or-no "yn"))))

(deftest ^:unit is-number
  (is (false? (u/is-number 6 "6")))
  (is (true? (u/is-number 6 "5")))
  (is (false? (u/is-number 6 "7"))))

(deftest ^:unit is-number
  (is (true? (u/lazy-contains? (lazy-seq [1 2]) 1)))
  (is (false? (u/lazy-contains? (lazy-seq [1 2]) 3))))