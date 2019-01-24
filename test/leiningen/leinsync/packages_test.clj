(ns leiningen.leinsync.packages-test
  (:require [clojure.test :refer :all]
            [leiningen.leinsync.packages :as packages]
            [clojure.java.io :as io]
            [leiningen.leinsync.namespaces :as ns])
  (:import (java.io File)))

(deftest ^:unit is-sync-namespace-test
  (is (true? (packages/is-on-sync-package?
               "folder1/de/otto/one/cool/ns.clj"
               {:source-paths ["folder1"]
                :sync         {:packages ["de.otto.one"]}})))

  (is (true? (packages/is-on-sync-package?
               "folder1/de/otto/one/ns.clj"
               {:source-paths ["folder1"]
                :sync         {:packages ["de.otto.one"]}})))

  (is (false? (packages/is-on-sync-package?
                "folder1/de/otto/two/one/ns.clj"
                {:source-paths ["folder1"]
                 :sync         {:packages ["de.otto.one"]}}))))

(deftest ^:unit should-update-package-test
  (is (true? (packages/should-update-package?
               "de.otto.one"
               {:source-paths ["folder1"]
                :sync         {:packages ["de.otto.one"]}})))

  (is (false? (packages/should-update-package?
                "de.otto.two"
                {:source-paths ["folder1"]
                 :sync         {:packages ["de.otto.one"]}}))))

(deftest ^:unit is-package-test
  (is (true? (packages/is-package? [:folder-name (io/file "test/leiningen/leinsync")])))
  (is (false? (packages/is-package? [:folder-name (io/file "test/leiningen/leinsync/packages_test.clj")]))))

(deftest ^:unit get-src-test-folders-test
  (is (= #{"folder1" "folder2"}
         (packages/get-src-test-folders {:source-paths ["folder1"]
                                         :test-paths   ["folder2"]
                                         :sync         {:packages ["de.otto.one"]}}))))

(deftest ^:unit folder-name-of-test
  (is (= "x"
         (packages/folder-name-of "src/path/to/package/x")))
  (is (= "src/path/to/package/x"
         (packages/get-package-path "src.path.to.package.x"))))

(deftest ^:unit update-file!-test
  (let [state (atom [])
        file-type :namespace
        write-f (fn [x y] (reset! state {:to (.getPath x) :from (.getPath y)}))
        target-project "target-project"
        folder-name "folder"
        package-path "src/to/package/x"
        src-package-file (new File "src/to/package/x/ns.clj")]
    (is (= {:from "src/to/package/x/ns.clj",
            :to   "../target-project/folder/src/to/package/x/ns.clj"}
           (packages/update-file! file-type write-f target-project folder-name package-path src-package-file)))))

(deftest ^:unit make-sync-work-unit-test
  (let [children [(new File "child-1") (new File "child-2") (new File "child-3")]
        package-path "src/de/otto/package/x/y"
        source-project-desc {:source-paths ["folder-1"]
                             :sync         {:packages ["de.otto.package.x"]}}
        target-projects-desc {:source-paths ["folder-1"]
                              :sync         {:packages ["de.otto.package.x"]}}]
    (with-redefs-fn {#'io/file                   (fn [x] (new File x))
                     #'packages/is-package?      (fn [[folder]] (= folder "folder-1"))
                     #'packages/files-of-package (fn [x] children)}
      #(is (= [["folder-1" children ns/src-path-def]]
              (packages/make-sync-work-unit package-path
                                            source-project-desc
                                            target-projects-desc))))))