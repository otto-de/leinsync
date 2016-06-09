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

(defn test-on [projects-desc p]
  (u/run-command-on (pr/->target-project-path p)
                    t/lein-test p
                    (get projects-desc (keyword p))))

;;;;; Sync Commands ;;;;;

(defn reset-all! [projects _]
  (doseq [p projects]
    (u/run-command-on (pr/->target-project-path p) git/reset-project! p)))

(defn commit-all! [projects _]
  (let [commit-msg (->> projects
                        (str/join ",")
                        (str "\nPlease enter the commit message for the projects: ")
                        (u/ask-user))]
    (doseq [p projects]
      (u/run-command-on (pr/->target-project-path p) git/commit-project! p commit-msg))
    (m/info "\n\nTo push        : lein sync" (str/join "," projects) "--push")))

(defn pull-rebase-all! [projects _]
  (doseq [p projects]
    (u/run-command-on (pr/->target-project-path p) git/pull-rebase! p)))

(defn push-all! [projects _]
  (doseq [p projects]
    (u/run-command-on (pr/->target-project-path p) git/check-and-push! p)))

(defn status-all [projects _]
  (doseq [p projects]
    (u/run-command-on (pr/->target-project-path p) git/status p)))

(defn show-all-diff [projects _]
  (doseq [p projects]
    (u/run-command-on (pr/->target-project-path p) git/diff p)))

(defn update-projects! [target-projects source-project-desc]
  (let [all-target-projects-desc (pr/read-all-target-project-clj target-projects)
        namespaces (u/cartesian-product (get-in source-project-desc ns/namespace-def) target-projects)
        resources (u/cartesian-product (get-in source-project-desc ns/resource-def) target-projects)]
    (if (not (empty? namespaces)) (ns/update-namespaces! namespaces source-project-desc all-target-projects-desc))
    (if (not (empty? resources)) (ns/update-resouces! resources source-project-desc all-target-projects-desc))))

(defn run-test [target-projects _]
  (let [target-projects-desc (pr/read-all-target-project-clj target-projects)]
    (-> (partial test-on target-projects-desc)
        (map target-projects)
        (t/log-test-hints))))

(defn list [target-projects {source-project :name}]
  (let [all-projects-desc (-> target-projects
                              (conj source-project)
                              (pr/read-all-target-project-clj))]
    (reduce str
            (map
             (partial l/list-resources all-projects-desc)
             [ns/namespace-def ns/resource-def]))))
