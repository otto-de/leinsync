(ns leiningen.leinsync.namespaces-test
  (:require [clojure.test :refer :all]
            [leiningen.leinsync.namespaces :as ns]
            [leiningen.core.project :as p]
            [leiningen.leinsync.utils :as u]))

(deftest ^:unit path->namespace
  (testing "happy path"
    (is (= {:resource-path "folder1"
            :resource-name "de.otto.one.cool.ns"}
           (ns/path->namespace "folder1/de/otto/one/cool/ns.clj" {:source-paths   ["folder1"]
                                                                  :test-paths     []
                                                                  :resource-paths []
                                                                  :sync           {:namespaces ["de.otto.one.cool.ns"]
                                                                                   :resources  [""]}})))

    (is (= {:resource-path "folder1"
            :resource-name "de.otto.one.cool.name-space-1"}
           (ns/path->namespace "folder1/de/otto/one/cool/name_space_1.clj" {:source-paths   ["folder1"]
                                                                            :test-paths     []
                                                                            :resource-paths []
                                                                            :sync           {:namespaces ["de.otto.one.cool.name-space-1"]
                                                                                             :resources  [""]}})))

    (is (= {:resource-path "folder1"
            :resource-name "de.otto.one.cool.ns"}
           (ns/path->namespace "folder1/de/otto/one/cool/ns.clj" {:source-paths   []
                                                                  :test-paths     ["folder1"]
                                                                  :resource-paths []
                                                                  :sync           {:namespaces ["de.otto.one.cool.ns"]
                                                                                   :resources  [""]}})))
    (is (= {:resource-path "folder1"
            :resource-name "log/log.xml"}
           (ns/path->namespace "folder1/log/log.xml" {:source-paths   []
                                                      :test-paths     []
                                                      :resource-paths ["folder1"]
                                                      :sync           {:namespaces [""]
                                                                       :resources  ["log/log.xml"]}}))))

  (testing "negative case because folder3 is not specified with leinsync"
    (is (= {:resource-path :not-found
            :resource-name :not-found}
           (ns/path->namespace "folder3/de/otto/one/cool/ns.clj" {:source-paths   []
                                                                  :test-paths     []
                                                                  :resource-paths []
                                                                  :sync           {:namespaces ["de.otto.one.cool.ns"]
                                                                                   :resources  [""]}}))))

  (testing "negative case because it is resource ist not a clojure file"
    (is (= {:resource-path :not-found
            :resource-name :not-found}
           (ns/path->namespace "folder1/de/otto/one/cool/ns.xml" {:source-paths   []
                                                                  :test-paths     []
                                                                  :resource-paths ["folder1"]
                                                                  :sync           {:namespaces ["de.otto.one.cool.ns"]
                                                                                   :resources  [""]}})))
    (is (= {:resource-path :not-found
            :resource-name :not-found}
           (ns/path->namespace "folder1/de/otto/one/cool/ns.xml" {:source-paths   []
                                                                  :test-paths     []
                                                                  :resource-paths ["folder1"]
                                                                  :sync           {:namespaces ["de.otto.one.cool.ns"]
                                                                                   :resources  ["ns.xml"]}}))))

  (testing "negative case because ns is unknown"
    (is (= {:resource-path :not-found
            :resource-name :not-found}
           (ns/path->namespace "folder1/de/otto/one/cool/ns.clj" {:source-paths   ["folder1"]
                                                                  :test-paths     []
                                                                  :resource-paths []
                                                                  :sync           {:namespaces ["de.otto.another.cool.ns"]
                                                                                   :resources  [""]}}))))

  (testing "negative case because resource is unknown"
    (is (= {:resource-path :not-found
            :resource-name :not-found}
           (ns/path->namespace "folder1/log1.xml" {:source-paths   ["folder1"]
                                                   :test-paths     []
                                                   :resource-paths []
                                                   :sync           {:namespaces ["de.otto.another.cool.ns"]
                                                                    :resources  ["log2.xml"]}})))
    (is (= {:resource-path :not-found
            :resource-name :not-found}
           (ns/path->namespace "folder1/log1.xml" {:source-paths   []
                                                   :test-paths     []
                                                   :resource-paths ["folder1"]
                                                   :sync           {:namespaces ["de.otto.another.cool.ns"]
                                                                    :resources  ["log/log1.xml"]}})))))

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
         (ns/namespace->path "de.otto.one.cool.ns-test" project-clj))))

(deftest ^:unit split-src-path
  (is (= {:src-or-test    ["folder1" "folder2"]
          :namespace-path "de/otto/one/cool/ns.clj"}
         (ns/namespace->path "de.otto.one.cool.ns" project-clj))))

(deftest ^:unit is-a-test-ns
  (is (= ["testfolder1" "testfolder2"]
         (ns/test-or-source-namespace "de.otto.one.cool.ns-test" project-clj))))

(deftest ^:unit is-not-a-ns
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
(deftest ^:unit cartesian-product-test
  (is (= [[:a :1] [:a :2] [:a :3] [:b :1] [:b :2] [:b :3] [:c :1] [:c :2] [:c :3]]
         (u/cartesian-product [:a :b :c] [:1 :2 :3])))
  (is (not (u/cartesian-product [:a :b :c] [])))
  (is (not (u/cartesian-product [] [:1 :2 :3])))
  (is (= [[:a :1]] (u/cartesian-product [:a] [:1]))))

(deftest ^:unit exists-file-test
  (is (true? (u/exists? (u/absolute-path-of "test-resources/project_test.clj"))))
  (is (false? (u/exists? (u/absolute-path-of "test-resources/project_test_not-exists.clj"))))
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

(deftest ^:unit should-update?
  (is (true? (ns/should-update?
              [:sync :resources] "path.to.ns1"
              {:sync {:resources ["path.to.ns1" "path.to.ns2"]}})))
  (is (false? (ns/should-update?
               [:sync :resources] "path.to.ns3"
               {:sync {:resources ["path.to.ns1" "path.to.ns2"]}})))
  (is (false? (ns/should-update? [] "path.to.ns3" {})))
  (is (false? (ns/should-update? [] "" {}))))

(deftest ^:unit location-with-question-test-test
  (is (= (str
          "* ==> The location of a.b.c.name on PROJECT could not be determined.\n      "
          "Please choose one of options (a number):\n         "
          "+ -1 -> to skip updating a.b.c.name\n         "
          "+  0 -> ../folder/1/a.clj\n         "
          "+  1 -> ../folder/2/b.clj\n         "
          "+  2 -> ../folder/3/c.clj")

         (ns/location-question-with "a.b.c.name"
                                    "project"
                                    ["../folder/1/a.clj"
                                     "../folder/2/b.clj"
                                     "../folder/3/c.clj"]))))

(deftest ^:unit is-sync-namespace-test
  (is (true? (ns/is-sync-namespace?
              "folder1/de/otto/one/cool/ns.clj"
              {:source-paths   ["folder1"]
               :resource-paths ["folder2"]
               :sync           {:namespaces ["de.otto.one.cool.ns"]
                                :resources  ["log.xml"]}})))

  (is (false? (ns/is-sync-namespace?
               "folder3/de/otto/one/cool/ns.clj"
               {:source-paths   ["folder1"]
                :resource-paths ["folder2"]
                :sync           {:namespaces ["de.otto.one.cool.ns"]
                                 :resources  ["log.xml"]}}))))