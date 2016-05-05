(ns leiningen.sync
  (:refer-clojure :exclude [sync])
  (:require [leiningen.core.main :as main]
            [leiningen.utils :as u]
            [leiningen.namespaces :as ns]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]))

(def sync-commands {:default ns/update-and-test!
                    :notest  ns/update-ns-of-projects!
                    :reset   ns/reset-all!
                    :diff    ns/show-all-diff
                    :pull    ns/pull-rebase-all!
                    :push    ns/push-all!
                    :status  ns/status_all
                    :commit  ns/commit-all!})

(defn ->commands [options]
  (let [commands (->> options
                      (keys)
                      (select-keys sync-commands)
                      (reduce-kv (fn [m _ f] (conj m (partial u/run! f))) []))]
    (if (empty? commands)
      [(partial u/run! (:default sync-commands))]
      commands)))

(defn execute-program [target-projects namespaces source-project-desc options]
  (doseq [command (->commands options)]
    (command
     target-projects
     namespaces
     source-project-desc)))

(defn one-arg-program [source-project-desc target-projects options]
  (execute-program
   (u/split target-projects)
   (ns/sync-def-selector source-project-desc)
   source-project-desc
   options))

(defn two-args-program [projects namespaces project-desc options]
  (execute-program
   (u/split projects)
   (u/split namespaces)
   project-desc
   options))

(def cli-options
  [[nil "--notest" "Synchronize shared code base without executing tests on target projects"]
   [nil "--diff" "Show changes on target projects"]
   [nil "--commit" "Commit change on target projects"]
   [nil "--pull" "pull rebase on target projects"]
   [nil "--push" "push on target projects"]
   [nil "--status" "check status on target projects"]
   [nil "--reset" "Reset all the uncommited changes in all target projects"]])

(defn usage [options-summary]
  (->> [""
        "sync is a Leiningen plugin to synchronize same codebase between different clojure projects"
        ""
        "Usage:"
        ""
        "  *  lein [options] sync \"project-1,project-2,project-3\""
        ""
        "  *  lein [options] sync \"project-1,project-2\" \"namespace.to.sync.1,namespace.to.sync.2\""
        ""
        "Options:"
        options-summary
        ""
        "To specify the namespaces to be shared, you must define them in project.clj. i.e"
        ":ns-sync [\"namespace.to.be.sync.1\" \"namespace.to.be.sync.2\"]"
        ""]
       (str/join \newline)))

(defn sync [project-desc & args]
  (let [{:keys [options arguments summary]} (cli/parse-opts args cli-options)]
    (cond
      (= 1 (count arguments)) (one-arg-program project-desc (first arguments) options)
      (= 2 (count arguments)) (two-args-program (first arguments) (second arguments) project-desc options)
      :else (main/abort (usage summary)))
    (main/exit)))