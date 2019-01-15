(ns leiningen.leinsync.commands
  (:refer-clojure :exclude [list])
  (:require [clojure.string :as str]
            [leiningen.leinsync.project-reader :as pr]
            [leiningen.leinsync.utils :as u]
            [leiningen.leinsync.namespaces :as ns]
            [leiningen.leinsync.git :as git]
            [leiningen.leinsync.list :as l]
            [leiningen.leinsync.deps :as deps]
            [leiningen.leinsync.tests :as t]
            [leiningen.leinsync.packages :as p]))

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
  (let [commit-msg (->> (str/join "," projects)
                        (str "\nPlease enter the commit message for the projects: ")
                        (u/ask-user))
        status (map #(u/run-command-on (pr/->target-project-path %)
                                       git/commit-project!
                                       %
                                       commit-msg
                                       ((keyword %)
                                        (pr/read-all-target-project-clj projects)))
                    projects)
        committed-project (seq (map :project (filter #(= :committed (:status %)) status)))
        not-committed-project (seq (map :project (filter #(not= :committed (:status %)) status)))]
    (git/log-git-status status
                        (if committed-project
                          (str "\n*To push        : lein sync " (str/join "," committed-project) " --push")
                          "")
                        (if not-committed-project
                          (str "\n*Could not commit on the projects: " (str/join "," not-committed-project))
                          ""))))

(defn status-all [_ projects _]
  (let [projects-desc (pr/read-all-target-project-clj projects)]
    (-> #(u/run-command-on (pr/->target-project-path %) git/details-status % ((keyword %) projects-desc))
        (map projects)
        (git/log-git-status))))

(defn reset-all! [_ projects _]
  (-> #(u/run-command-on (pr/->target-project-path %) git/reset-project! %)
      (map projects)
      (git/log-git-status)))

(defn list [arg target-projects {source-project :name}]
  (let [all-projects-desc (pr/read-all-target-project-clj (conj target-projects source-project))]
    (l/list-resources all-projects-desc p/package-def arg)
    (l/list-resources all-projects-desc ns/namespace-def arg)
    (l/list-resources all-projects-desc ns/resource-def arg)))

(defn update-combination-of [path source-project-desc target-projects]
  (seq (u/cartesian-product (get-in source-project-desc path) target-projects)))

(defn update-projects! [_ target-projects source-project-desc]
  (let [target-projects-desc (pr/read-all-target-project-clj target-projects)]
    (when-let [namespaces (update-combination-of ns/namespace-def source-project-desc target-projects)]
      (ns/update-namespaces! namespaces source-project-desc target-projects-desc))
    (when-let [resources (update-combination-of ns/resource-def source-project-desc target-projects)]
      (ns/update-resources! resources source-project-desc target-projects-desc))
    (when-let [packages (update-combination-of p/package-def source-project-desc target-projects)]
      (p/update-packages! packages source-project-desc target-projects-desc))))

(defn deps [arg target-projects {source-project :name}]
  (deps/check-dependencies-of (pr/read-all-target-project-clj (conj target-projects source-project))
                              (if (= :global arg) [:dependencies] [:profiles arg :dependencies])))

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