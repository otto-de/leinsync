(ns leiningen.leinsync.git-test
  (:require [clojure.test :refer :all]
            [leiningen.leinsync.git :as git]))

(deftest ^:unit remove-git-change-status-from
  (is (= "a.changed.resource"
         (git/remove-git-status " M a.changed.resource")))
  (is (= "a.changed.resource"
         (git/remove-git-status "M a.changed.resource")))

  (is (= "a.changed.resource"
         (git/remove-git-status " A a.changed.resource")))
  (is (= "a.changed.resource"
         (git/remove-git-status "A a.changed.resource")))

  (is (= "a.changed.resource"
         (git/remove-git-status " D a.changed.resource")))
  (is (= "a.changed.resource"
         (git/remove-git-status "D a.changed.resource")))

  (is (= "a.changed.resource"
         (git/remove-git-status " R a.changed.resource")))
  (is (= "a.changed.resource"
         (git/remove-git-status "R a.changed.resource")))

  (is (= "a.changed.resource"
         (git/remove-git-status " C a.changed.resource")))
  (is (= "a.changed.resource"
         (git/remove-git-status "C a.changed.resource")))

  (is (= "a.changed.resource"
         (git/remove-git-status " U a.changed.resource")))
  (is (= "a.changed.resource"
         (git/remove-git-status "U a.changed.resource")))

  (is (= "a.changed.resource"
         (git/remove-git-status " ?? a.changed.resource")))
  (is (= "a.changed.resource"
         (git/remove-git-status "?? a.changed.resource"))))

(deftest ^:unit other-change-status
  (is (= {:other-changes "has 10 changes"}
         (git/other-change-status (take 10 (repeat "x")) true)))
  (is (= {:other-changes :no-change}
         (git/other-change-status [] true)
         (git/other-change-status [] false)))
  (is (= {:other-changes "x x"}
         (git/other-change-status (take 2 (repeat "x")) false)))
  (is (= {:other-changes "x x x . . ."}
         (git/other-change-status (take 5 (repeat "x")) false))))

(deftest ^:unit sync-change
  (is (= {:sync-relevant-changes "x x"}
         (git/sync-change (take 2 (repeat "x")))))
  (is (= {:sync-relevant-changes :no-change}
         (git/sync-change []))))

(deftest ^:unit get-details-status
  (let [project-desc {:source-paths   ["folder1"]
                      :test-paths     []
                      :resource-paths ["folder2"]
                      :sync        {:namespaces ["de.otto.one.cool.ns"]
                                    :resources  ["log.xml"]}}
        git-status-lines ["M folder1/de/otto/one/cool/ns.clj"
                          "M folder1/de/otto/one/not-relevante-ns.clj"
                          "D folder2/log.xml"]]
    (is (= {:other-changes         "M folder1/de/otto/one/not-relevante-ns.clj"
            :sync-relevant-changes "M folder1/de/otto/one/cool/ns.clj D folder2/log.xml"}
           (git/get-details-status git-status-lines project-desc))))

  (let [project-desc {:source-paths   ["folder1" "folder2" "folder3" "folder4" "folder5"]
                      :test-paths     []
                      :resource-paths ["folder2"]
                      :sync        {:namespaces ["de.otto.one.cool.ns"]
                                    :resources  ["log.xml"]}}
        git-status-lines ["M folder1/de/otto/one/cool/ns.clj"
                          "M folder1/de/otto/one/not-relevante-ns.clj"
                          "D folder2/log.xml"
                          "D folder3/log.xml"
                          "D folder4/log.xml"
                          "D folder5/log.xml"]]
    (is (= {:other-changes         "M folder1/de/otto/one/not-relevante-ns.clj D folder3/log.xml D folder4/log.xml . . ."
            :sync-relevant-changes "M folder1/de/otto/one/cool/ns.clj D folder2/log.xml"}
           (git/get-details-status git-status-lines project-desc)))))

(deftest ^:unit sync-resources-of
  (let [project-desc {:source-paths   ["folder1"]
                      :test-paths     []
                      :resource-paths ["folder2"]
                      :sync        {:namespaces ["de.otto.one.cool.ns"]
                                    :resources  ["log.xml"]}}
        changed-file ["folder1/de/otto/one/cool/ns.clj"
                      "folder1/de/otto/one/not-relevante-ns.clj"]

        untracked-file ["folder2/log.xml"
                        "folder2/not-relevant.csv"]]
    (is (= ["folder1/de/otto/one/cool/ns.clj"
            "folder2/log.xml"]
           (git/sync-resources-of changed-file untracked-file project-desc)))))

(deftest ^:unit changes-empty?
  (is (true? (git/has-no-change? {:unpushed-changes :no-change})))
  (is (false? (git/has-no-change? ["something"]))))

(deftest ^:unit get-last-commit-date-test
  (is (not-empty (git/last-commit-date "test-resources/dummy.clj"))))
