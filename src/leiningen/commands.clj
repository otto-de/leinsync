(ns leiningen.commands
  (:refer-clojure :exclude [list])
  (:require [leiningen.core.main :as m]
            [clojure.string :as str]
            [leiningen.project-reader :as pr]
            [leiningen.utils :as u]
            [leiningen.namespaces :as ns]
            [leiningen.git :as git]
            [leiningen.list-ns :as l]
            [leiningen.tests :as t]))

(defn test-on [projects-desc project]
  (u/run-command-on
   (pr/->target-project-path project)
   t/lein-test
   project
   (get projects-desc (keyword project))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-projects! [target-projects source-project-desc]
  (let [all-target-projects-desc (pr/read-all-target-project-clj target-projects)
        namespaces (u/cartesian-product (get-in source-project-desc ns/namespace-def) target-projects)
        resources (u/cartesian-product (get-in source-project-desc ns/resource-def) target-projects)]
    (if (not (empty? namespaces)) (ns/update-namespaces! namespaces source-project-desc all-target-projects-desc))
    (if (not (empty? resources)) (ns/update-resouces! resources source-project-desc all-target-projects-desc))))

(defn pull-rebase-all! [projects _]
  (-> #(u/run-command-on (pr/->target-project-path %) git/pull-rebase! %)
      (map projects)
      (git/log-git-status)))

(defn push-all! [projects _]
  (-> #(u/run-command-on (pr/->target-project-path %) git/check-and-push! %)
      (map projects)
      (git/log-git-status)))

(defn run-test [target-projects _]
  (let [target-projects-desc (pr/read-all-target-project-clj target-projects)]
    (-> (partial test-on target-projects-desc)
        (map target-projects)
        (t/log-test-hints))))

(defn commit-all! [projects _]
  (let [commit-msg (->> projects
                        (str/join ",")
                        (str "\nPlease enter the commit message for the projects: ")
                        (u/ask-user))]
    (-> #(u/run-command-on (pr/->target-project-path %) git/commit-project! % commit-msg)
        (map projects)
        (git/log-git-status))
    (m/info "\n\n*To push        : lein sync" (str/join "," projects) "--push")))

(defn show-all-diff [projects _]
  (-> #(u/run-command-on (pr/->target-project-path %) git/diff %)
      (map projects)
      (git/log-git-status)))

(defn status-all [projects _]
  (-> #(u/run-command-on (pr/->target-project-path %) git/status %)
      (map projects)
      (git/log-git-status)))

(defn reset-all! [projects _]
  (-> #(u/run-command-on (pr/->target-project-path %) git/reset-project! %)
      (map projects)
      (git/log-git-status)))

(defn list [target-projects {source-project :name}]
  (let [all-projects-desc (-> target-projects
                              (conj source-project)
                              (pr/read-all-target-project-clj))]
    (reduce str
            (map
             (partial l/list-resources all-projects-desc)
             [ns/namespace-def ns/resource-def]))))