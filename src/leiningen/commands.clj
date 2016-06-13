(ns leiningen.commands
  (:refer-clojure :exclude [list])
  (:require   [clojure.string :as str]
              [leiningen.project-reader :as pr]
              [leiningen.utils :as u]
              [leiningen.namespaces :as ns]
              [leiningen.git :as git]
              [leiningen.list-ns :as l]
              [leiningen.tests :as t]))

(defn pull-rebase-all! [projects _]
  (-> #(u/run-command-on (pr/->target-project-path %) git/pull-rebase! %)
      (map projects)
      (git/log-git-status)))

(defn push-all! [projects _]
  (-> #(u/run-command-on (pr/->target-project-path %) git/check-and-push! %)
      (map projects)
      (git/log-git-status)))

(defn run-test [target-projects _]
  (let [target-project-desc (pr/read-all-target-project-clj target-projects)]
    (-> #(u/run-command-on (pr/->target-project-path %) t/lein-test % ((keyword %) target-project-desc))
        (map target-projects)
        (t/log-test-hints))))

(defn commit-all! [projects _]
  (let [commit-msg (->> projects
                        (str/join ",")
                        (str "\nPlease enter the commit message for the projects: ")
                        (u/ask-user))
        projects-str (str/join "," projects)]
    (-> #(u/run-command-on (pr/->target-project-path %) git/commit-project! % commit-msg)
        (map projects)
        (git/log-git-status "\n*To push        : lein sync" projects-str "--push"))))

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
    (doall
     (map
      (partial l/list-resources all-projects-desc)
      [ns/namespace-def ns/resource-def]))))

(defn update-projects! [target-projects source-project-desc]
  (let [all-target-project-desc (pr/read-all-target-project-clj target-projects)
        namespaces (u/cartesian-product (get-in source-project-desc ns/namespace-def) target-projects)
        resources (u/cartesian-product (get-in source-project-desc ns/resource-def) target-projects)]
    (if (not (empty? namespaces)) (ns/update-namespaces! namespaces source-project-desc all-target-project-desc))
    (if (not (empty? resources)) (ns/update-resouces! resources source-project-desc all-target-project-desc))))