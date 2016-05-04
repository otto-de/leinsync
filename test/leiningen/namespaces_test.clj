(ns leiningen.namespaces-test
  (:require [clojure.test :refer :all]
            [leiningen.namespaces :as ns]))

(deftest ^:unit convert-namespace->des-path-of-src
  (is (= "../project/src/de/otto/one/cool/ns.clj"
         (ns/ns->target-path "de.otto.one.cool.ns" "project"))))

(deftest ^:unit convert-namespace->des-path-of-test
  (is (= "../project/test/de/otto/one/cool/ns_test.clj"
         (ns/ns->target-path "de.otto.one.cool.ns-test" "project"))))

(deftest ^:unit convert-namespace->src-path-of-src
  (is (= "src/de/otto/one/cool/ns.clj"
         (ns/ns->source-path "de.otto.one.cool.ns"))))

(deftest ^:unit convert-namespace->src-path-of-test
  (is (= "test/de/otto/one/cool/ns_test.clj"
         (ns/ns->source-path "de.otto.one.cool.ns-test"))))

(deftest ^:unit split-test-path
  (is (= {:src-or-test "test" :name-space "de/otto/one/cool/ns_test.clj"}
         (ns/split-path "de.otto.one.cool.ns-test"))))

(deftest ^:unit split-src-path
  (is (= {:src-or-test "src" :name-space "de/otto/one/cool/ns.clj"}
         (ns/split-path "de.otto.one.cool.ns"))))

(deftest ^:unit is-a-test-ns
  (is (= "test"
         (ns/test-or-source-ns "de.otto.one.cool.ns-test"))))

(deftest ^:unit is-not-a-test-ns
  (is (= "src"
         (ns/test-or-source-ns "de.otto.one.cool.ns"))))

(deftest ^:unit is-not-a-test-ns
  (is (= "test"
         (ns/test-or-source-ns "test-utils-ns"))))

(deftest ^:unit flap-map-1
  (is (= '([:a :1] [:a :2] [:a :3] [:b :1] [:b :2] [:b :3] [:c :1] [:c :2] [:c :3])
         (ns/cartesian-product '(:a :b :c) '(:1 :2 :3)))))

(deftest ^:unit flap-map-2
  (is (= nil
         (ns/cartesian-product '(:a :b :c) '()))))

(deftest ^:unit flap-map-3
  (is (= nil
         (ns/cartesian-product '() '(:1 :2 :3)))))

(deftest ^:unit flap-map-4
  (is (= '([:a :1])
         (ns/cartesian-product '(:a) '(:1)))))