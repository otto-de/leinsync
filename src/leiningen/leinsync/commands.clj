(ns leiningen.leinsync.commands
  (:refer-clojure :exclude [list])
  (:require [clojure.string :as str]
            [leiningen.leinsync.project-reader :as pr]
            [leiningen.leinsync.utils :as u]
            [leiningen.leinsync.namespaces :as ns]
            [leiningen.leinsync.git :as git]
            [leiningen.leinsync.list-ns :as l]
            [leiningen.leinsync.deps :as deps]
            [leiningen.leinsync.tests :as t]))

(defn pull-rebase-all! [_ projects _]
  (-> #(u/run-command-on (pr/->target-project-path %) git/pull-rebase! %)
      (map projects)
      (git/log-git-status)))

(defn push-all! [_ projects _]
  (-> #(u/run-command-on (pr/->target-project-path %) git/check-and-push! %)
      (map projects)
      (git/log-git-status)))

(defn run-test [_ projects _]
  (let [target-project-desc (pr/read-all-target-project-clj projects)]
    (-> #(u/run-command-on (pr/->target-project-path %) t/lein-test % ((keyword %) target-project-desc))
        (map projects)
        (t/log-test-hints))))

(defn commit-all! [_ projects _]
  (let [projects-str (str/join "," projects)
        commit-msg (->> projects-str
                        (str "\nPlease enter the commit message for the projects: ")
                        (u/ask-user))
        projects-desc (pr/read-all-target-project-clj projects)]
    (-> #(u/run-command-on (pr/->target-project-path %) git/commit-project! % commit-msg ((keyword %) projects-desc))
        (map projects)
        (git/log-git-status "\n*To push        : lein sync" projects-str "--push"))))

(defn status-all [_ projects _]
  (let [projects-desc (pr/read-all-target-project-clj projects)]
    (-> #(u/run-command-on (pr/->target-project-path %) git/details-status % ((keyword %) projects-desc))
        (map projects)
        (git/log-git-status))))

(defn reset-all! [_ projects _]
  (-> #(u/run-command-on (pr/->target-project-path %) git/reset-project! %)
      (map projects)
      (git/log-git-status)))

(defn list [_ target-projects {source-project :name}]
  (let [all-projects-desc (-> target-projects
                              (conj source-project)
                              (pr/read-all-target-project-clj))]
    (l/list-resources all-projects-desc ns/namespace-def)
    (l/list-resources all-projects-desc ns/resource-def)))

(defn update-projects! [_ target-projects source-project-desc]
  (let [all-target-project-desc (pr/read-all-target-project-clj target-projects)
        namespaces (u/cartesian-product (get-in source-project-desc ns/namespace-def) target-projects)
        resources (u/cartesian-product (get-in source-project-desc ns/resource-def) target-projects)]
    (if (not (empty? namespaces)) (ns/update-namespaces! namespaces source-project-desc all-target-project-desc))
    (if (not (empty? resources)) (ns/update-resouces! resources source-project-desc all-target-project-desc))))

(defn deps [_ target-projects {source-project :name}]
  (let [all-projects-desc (-> target-projects
                              (conj source-project)
                              (pr/read-all-target-project-clj))]
    (deps/check-deps all-projects-desc)))

(def SYNC-COMMANDS {:default {:update "" :test ""}
                    :deps    deps
                    :list    list
                    :update  update-projects!
                    :notest  update-projects!
                    :test    run-test
                    :reset   reset-all!
                    :status  status-all
                    :commit  commit-all!
                    :pull    pull-rebase-all!
                    :push    push-all!})