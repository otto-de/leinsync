(ns leiningen.commands
  (:refer-clojure :exclude [list])
  (:require [leiningen.utils :as u]
            [leiningen.namespaces :as ns]
            [leiningen.git :as git]
            [leiningen.list-ns :as l]
            [leiningen.tests :as t]
            [clojure.string :as str]
            [leiningen.core.main :as m]))

(defn- log-test-hints [passed-projects failed-project]
  (when (not (empty? failed-project))
    (m/info "* Please have a look  at the failed project(s):" failed-project))
  (when (not (empty? passed-projects))
    (m/info "* Tests are passed on project(s):" passed-projects "\n\n")
    (m/info "To see changes : lein sync" passed-projects "--diff")
    (m/info "To commit      : lein sync" passed-projects "--commit")
    (m/info "To push        : lein sync" passed-projects "--push")))

(defn read-all-target-project-clj [target-projects]
  (zipmap
   (map keyword target-projects)
   (map ns/read-target-project-clj target-projects)))

;;;;; Sync Commands ;;;;;

(defn reset-all! [projects _]
  (doseq [p projects]
    (u/run-command-on (ns/->target-project-path p) git/reset-project! p)))

(defn commit-all! [projects _]
  (let [commit-msg (->> projects
                        (str/join ",")
                        (str "\nPlease enter the commit message for the projects: ")
                        (u/ask-user))]
    (doseq [p projects]
      (u/run-command-on (ns/->target-project-path p) git/commit-project! p commit-msg))
    (m/info "\n\nTo push        : lein sync" (str/join "," projects) "--push")))

(defn pull-rebase-all! [projects _]
  (doseq [p projects]
    (u/run-command-on (ns/->target-project-path p) git/pull-rebase! p)))

(defn push-all! [projects _]
  (doseq [p projects]
    (u/run-command-on (ns/->target-project-path p) git/check-and-push! p)))

(defn status-all [projects _]
  (doseq [p projects]
    (u/run-command-on (ns/->target-project-path p) git/status p)))

(defn show-all-diff [projects _]
  (doseq [p projects]
    (u/run-command-on (ns/->target-project-path p) git/diff p)))

(defn update-projects! [target-projects source-project-desc]
  (let [all-target-projects-desc (read-all-target-project-clj target-projects)
        namespaces (u/cartesian-product (get-in source-project-desc ns/namespace-def) target-projects)
        resources (u/cartesian-product (get-in source-project-desc ns/resource-def) target-projects)]
    (if (not (empty? namespaces)) (ns/update-namespaces! namespaces source-project-desc all-target-projects-desc))
    (if (not (empty? resources)) (ns/update-resouces! resources source-project-desc all-target-projects-desc))))

(defn run-test [target-projects _]
  (let [target-projects-desc (read-all-target-project-clj target-projects)
        results (t/test-all target-projects target-projects-desc)]
    (log-test-hints (->> results
                         (filter #(= (:result %) :passed))
                         (map :project)
                         (str/join ","))
                    (->> results
                         (filter #(= (:result %) :failed))
                         (map :project)
                         (str/join ",")))))

(defn list [target-projects {source-project :name}]
  (let [all-projects-desc (-> target-projects
                              (conj source-project)
                              (read-all-target-project-clj))]
    (l/list-resources all-projects-desc ns/namespace-def)
    (l/list-resources all-projects-desc ns/resource-def)))
