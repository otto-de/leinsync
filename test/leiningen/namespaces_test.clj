(ns leiningen.namespaces-test
  (:require [clojure.test :refer :all]
            [leiningen.namespaces :as ns]
            [leiningen.core.project :as p]
            [leiningen.utils :as u]))

(deftest ^:unit convert-namespace->des-path-of-src
  (is (= ["../project/folder1/de/otto/one/cool/ns.clj"
          "../project/folder2/de/otto/one/cool/ns.clj"]
         (ns/namespace->target-path
          "de.otto.one.cool.ns" "project"
          (p/read-raw "test-resources/project_test.clj")))))

(deftest ^:unit convert-namespace->des-path-of-test
  (is (= ["../project/testfolder1/de/otto/one/cool/ns_test.clj"
          "../project/testfolder2/de/otto/one/cool/ns_test.clj"]
         (ns/namespace->target-path
          "de.otto.one.cool.ns-test" "project"
          (p/read-raw "test-resources/project_test.clj")))))

(deftest ^:unit resource->target-path-test
  (is (= ["../target-project/folder1/resource.edn"
          "../target-project/folder2/resource.edn"]
         (ns/resource->target-path "resource.edn"
                                   "target-project"
                                   {:resource-paths ["folder1" "folder2"]}))))

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

(deftest ^:unit edge-case-duplicate-folders
  (is (= ["thesamefolder/de/otto/one/cool/ns.clj"
          "anotherfolder/de/otto/one/cool/ns.clj"]
         (ns/namespace->source-path "de.otto.one.cool.ns" {:source-paths ["thesamefolder" "thesamefolder" "thesamefolder"
                                                                          "anotherfolder"]
                                                           :test-paths   ["testfolder1" "testfolder2"]}))))
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

(deftest ^:unit determine-source-target
  (testing "source and target exist and unique"
    (let [name "path.1"
          existing-source-paths ["source/path/1"]
          existing-target-paths ["target/path/1"]
          target-paths ["target/path/1"]
          target-project "project"]
      (is (= {:source "source/path/1" :target "target/path/1"}
             (ns/determine-source-target name
                                         existing-source-paths
                                         existing-target-paths
                                         target-paths
                                         target-project
                                         ns/ask-for-source-and-target)))))

  (testing "source  exists, target doen't exist but its location is unique"
    (let [name "path.1"
          existing-source-paths ["source/path/1"]
          existing-target-paths []
          target-paths ["target/path/1"]
          target-project "project"]
      (is (= {:source "source/path/1" :target "target/path/1"}
             (ns/determine-source-target name
                                         existing-source-paths
                                         existing-target-paths
                                         target-paths
                                         target-project
                                         ns/ask-for-source-and-target)))))

  (testing "multiple sources and targets exist so ask user for correct locations"
    (let [ask-for-source-and-target (fn [_ _ _ _] {:source :user-input-1 :target :user-input-2})
          name "path.1"
          existing-source-paths ["source/path/1" "source/path/2"]
          existing-target-paths []
          target-paths ["target/path/1"]
          target-project "project"]
      (is (= {:source :user-input-1 :target :user-input-2}
             (ns/determine-source-target name
                                         existing-source-paths
                                         existing-target-paths
                                         target-paths
                                         target-project
                                         ask-for-source-and-target)))))

  (testing "multiple sources and targets exist so ask user for correct locations"
    (let [ask-for-source-and-target (fn [_ _ _ _] {:source :user-input-1 :target :user-input-2})
          name "path.1"
          existing-source-paths ["source/path/1"]
          existing-target-paths []
          target-paths ["target/path/1" "target/path/2"]
          target-project "project"]
      (is (= {:source :user-input-1 :target :user-input-2}
             (ns/determine-source-target name
                                         existing-source-paths
                                         existing-target-paths
                                         target-paths
                                         target-project
                                         ask-for-source-and-target)))))

  (testing "multiple sources and targets exist so ask user for correct locations"
    (let [ask-for-source-and-target (fn [_ _ _ _] {:source :user-input-1 :target :user-input-2})
          name "path.1"
          existing-source-paths ["source/path/1" "source/path/2"]
          existing-target-paths []
          target-paths ["target/path/1" "target/path/2"]
          target-project "project"]
      (is (= {:source :user-input-1 :target :user-input-2}
             (ns/determine-source-target name
                                         existing-source-paths
                                         existing-target-paths
                                         target-paths
                                         target-project
                                         ask-for-source-and-target))))))

(defn- project-occurence-render [existing-path project]
  (if (empty? existing-path)
    {project "O X"}
    {project "  X"}))

(deftest ^:unit build-resource-table
  (testing "print table structure for the namespaces"
    (let [projects {:project-1 {:ns-sync        {:namespaces ["path.to.ns1" "path.to.ns2" "path.to.ns4"]}
                                :source-paths   ["a"]
                                :test-paths     ["b"]
                                :resource-paths ["c"]}
                    :project-2 {:ns-sync        {:namespaces ["path.to.ns1" "path.to.ns3" "path.to.ns4"]}
                                :source-paths   ["d"]
                                :test-paths     ["e"]
                                :resource-paths ["f"]}}]

      (is (= [{:package "path.to" :name "ns1", :project-1 "O X", :project-2 "O X"}
              {:package "path.to" :name "ns2", :project-1 "O X", :project-2 ""}
              {:package "path.to" :name "ns3", :project-1 "", :project-2 "O X"}
              {:package "path.to" :name "ns4", :project-1 "O X", :project-2 "O X"}]
             (->> (ns/build-resource-table projects ns/namespace-def project-occurence-render)
                  (sort-by :name))))))

  (testing "print table structure for the resources"
    (let [projects {:project-1 {:ns-sync        {:resources ["r1.xml" "r2.edn" "r3.json"]}
                                :source-paths   ["a"]
                                :test-paths     ["b"]
                                :resource-paths ["c"]}
                    :project-2 {:ns-sync        {:namespaces ["r1.xml" "r3.json" "r4.csv"]}
                                :source-paths   ["d"]
                                :test-paths     ["e"]
                                :resource-paths ["f"]}}]

      (is (= [{:name "csv", :package "r4", :project-1 "", :project-2 "O X"}
              {:name "json", :package "r3", :project-1 "", :project-2 "O X"}
              {:name "xml", :package "r1", :project-1 "", :project-2 "O X"}]
             (->> (ns/build-resource-table projects ns/namespace-def project-occurence-render)
                  (sort-by :name)))))))