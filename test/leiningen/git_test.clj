(ns leiningen.git-test
  (:require [clojure.test :refer :all]
            [leiningen.git :as git]))

(deftest ^:unit remove-git-change-status-from
  (is (= "a.changed.resource"
         (git/remove-git-change-status-from " M a.changed.resource")))
  (is (= "a.changed.resource"
         (git/remove-git-change-status-from "M a.changed.resource")))

  (is (= "a.changed.resource"
         (git/remove-git-change-status-from " A a.changed.resource")))
  (is (= "a.changed.resource"
         (git/remove-git-change-status-from "A a.changed.resource")))

  (is (= "a.changed.resource"
         (git/remove-git-change-status-from " D a.changed.resource")))
  (is (= "a.changed.resource"
         (git/remove-git-change-status-from "D a.changed.resource")))

  (is (= "a.changed.resource"
         (git/remove-git-change-status-from " R a.changed.resource")))
  (is (= "a.changed.resource"
         (git/remove-git-change-status-from "R a.changed.resource")))

  (is (= "a.changed.resource"
         (git/remove-git-change-status-from " C a.changed.resource")))
  (is (= "a.changed.resource"
         (git/remove-git-change-status-from "C a.changed.resource")))

  (is (= "a.changed.resource"
         (git/remove-git-change-status-from " U a.changed.resource")))
  (is (= "a.changed.resource"
         (git/remove-git-change-status-from "U a.changed.resource")))

  (is (= "a.changed.resource"
         (git/remove-git-change-status-from " ?? a.changed.resource")))
  (is (= "a.changed.resource"
         (git/remove-git-change-status-from "?? a.changed.resource"))))

(deftest ^:unit remove-git-change-status-from
  (let [project-desc {:source-paths   ["folder1"]
                      :test-paths     []
                      :resource-paths ["folder2"]
                      :ns-sync        {:namespaces ["de.otto.one.cool.ns"]
                                       :resources  ["log.xml"]}}
        git-status-lines ["M folder1/de/otto/one/cool/ns.clj"
                          "M folder1/de/otto/one/not-relevante-ns.clj"
                          "D folder2/log.xml"]]
    (is (= {:other-changes         "M folder1/de/otto/one/not-relevante-ns.clj"
            :sync-relevant-changes "M folder1/de/otto/one/cool/ns.clj D folder2/log.xml"}
           (git/get-details-status git-status-lines project-desc)))))

(deftest ^:unit sync-resources-of
  (let [project-desc {:source-paths   ["folder1"]
                      :test-paths     []
                      :resource-paths ["folder2"]
                      :ns-sync        {:namespaces ["de.otto.one.cool.ns"]
                                       :resources  ["log.xml"]}}
        changed-file ["folder1/de/otto/one/cool/ns.clj"
                      "folder1/de/otto/one/not-relevante-ns.clj"]

        untracked-file ["folder2/log.xml"
                        "folder2/not-relevant.csv"]]
    (is (= ["folder1/de/otto/one/cool/ns.clj"
            "folder2/log.xml"]
           (git/sync-resources-of changed-file untracked-file project-desc)))))

(deftest ^:unit changes-empty?
  (is (true? (git/changes-empty? {:unpushed-changes :no-change})))
  (is (false? (git/changes-empty? ["something"]))))
