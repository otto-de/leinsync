(ns leiningen.namespaces-test
  (:require [clojure.test :refer :all]
            [leiningen.namespaces :as ns]
            [leiningen.core.project :as p]
            [leiningen.utils :as u]))

(defn- read-fn [_]
  (p/read-raw "test-resources/project_test.clj"))

(deftest ^:unit convert-namespace->des-path-of-src
  (is (= ["../project/folder1/de/otto/one/cool/ns.clj"
          "../project/folder2/de/otto/one/cool/ns.clj"]
         (ns/namespace->target-path "de.otto.one.cool.ns" "project" read-fn))))

(deftest ^:unit convert-namespace->des-path-of-test
  (is (= ["../project/testfolder1/de/otto/one/cool/ns_test.clj"
          "../project/testfolder2/de/otto/one/cool/ns_test.clj"]
         (ns/namespace->target-path "de.otto.one.cool.ns-test" "project" read-fn))))

(deftest ^:unit resource->target-path-test
  (is (= ["../target-project/folder1/resource.edn"
          "../target-project/folder2/resource.edn"]
         (ns/resource->target-path "resource.edn"
                                   "target-project"
                                   (fn [_] {:resource-paths ["folder1" "folder2"]})))))

(deftest ^:unit resource->source-path-test
  (is (= ["folder1/resource.edn"
          "folder2/resource.edn"]
         (ns/resource->source-path "resource.edn" {:resource-paths ["folder1" "folder2"]}))))

(def project-clj {:source-paths ["folder1" "folder2"]
                  :test-paths   ["testfolder1" "testfolder2"]})

(deftest ^:unit convert-namespace->src-path-of-src
  (is (= ["folder1/de/otto/one/cool/ns.clj"
          "folder2/de/otto/one/cool/ns.clj"]
         (ns/namespace->source-path "de.otto.one.cool.ns" project-clj))))

(deftest ^:unit convert-namespace->src-path-of-test
  (is (= ["testfolder1/de/otto/one/cool/ns_test.clj"
          "testfolder2/de/otto/one/cool/ns_test.clj"]
         (ns/namespace->source-path "de.otto.one.cool.ns-test" project-clj))))

(deftest ^:unit split-test-path
  (is (= {:src-or-test    ["testfolder1" "testfolder2"]
          :namespace-path "de/otto/one/cool/ns_test.clj"}
         (ns/split-path "de.otto.one.cool.ns-test" project-clj))))

(deftest ^:unit split-src-path
  (is (= {:src-or-test    ["folder1" "folder2"]
          :namespace-path "de/otto/one/cool/ns.clj"}
         (ns/split-path "de.otto.one.cool.ns" project-clj))))

(deftest ^:unit is-a-test-ns
  (is (= ["testfolder1" "testfolder2"]
         (ns/test-or-source-namespace "de.otto.one.cool.ns-test" project-clj))))

(deftest ^:unit is-not-a-test-ns
  (is (= ["folder1" "folder2"]
         (ns/test-or-source-namespace "de.otto.one.cool.ns" project-clj))))

(deftest ^:unit is-not-a-test-ns
  (is (= ["testfolder1" "testfolder2"]
         (ns/test-or-source-namespace "test-utils-ns" project-clj))))

(deftest ^:unit flap-map-1
  (is (= '([:a :1] [:a :2] [:a :3] [:b :1] [:b :2] [:b :3] [:c :1] [:c :2] [:c :3])
         (u/cartesian-product '(:a :b :c) '(:1 :2 :3)))))

(deftest ^:unit flap-map-2
  (is (= nil
         (u/cartesian-product '(:a :b :c) '()))))

(deftest ^:unit flap-map-3
  (is (= nil
         (u/cartesian-product '() '(:1 :2 :3)))))

(deftest ^:unit flap-map-4
  (is (= '([:a :1])
         (u/cartesian-product '(:a) '(:1)))))

(deftest ^:unit flap-map-4
  (is (true? (u/exists? "test-resources/project_test.clj")))
  (is (false? (u/exists? "test-resources/project_test_not-exists.clj"))))