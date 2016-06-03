(ns leiningen.sync
  (:refer-clojure :exclude [sync])
  (:require [leiningen.core.main :as main]
            [leiningen.utils :as u]
            [leiningen.namespaces :as ns]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [leiningen.core.main :as m]))

(def sync-commands {:default [:update :test]
                    :update  ns/update-projects!
                    :notest  ns/update-projects!
                    :test    ns/run-test
                    :reset   ns/reset-all!
                    :diff    ns/show-all-diff
                    :status  ns/status-all
                    :commit  ns/commit-all!
                    :pull    ns/pull-rebase-all!
                    :push    ns/push-all!})

(defn find-command [option-keys]
  (->> option-keys
       (select-keys sync-commands)
       (reduce-kv (fn [m _ f] (conj m (partial u/run! f))) [])))

(defn ->commands [options]
  (let [commands (find-command (keys options))]
    (if (empty? commands)
      (find-command (:default sync-commands))
      commands)))

(defn execute-program [target-projects source-project-desc options]
  (doseq [command (->commands options)]
    (command target-projects source-project-desc)))

(def cli-options
  [[nil "--notest" "Synchronize shared code base without executing tests on target projects"]
   [nil "--test" "Executing tests on target projects"]
   [nil "--diff" "Show changes on target projects"]
   [nil "--status" "Check status on target projects"]
   [nil "--reset" "Reset all the uncommited changes in all target projects"]
   [nil "--commit" "Commit change on target projects"]
   [nil "--pull" "Pull rebase on target projects"]
   [nil "--push" "Push on target projects"]])

(defn usage [options-summary]
  (->> [""
        "sync is a Leiningen plugin to synchronize same codebase between different clojure projects"
        (str "version: " (u/get-version "sync"))
        ""
        "Usage:"
        ""
        "  *  lein sync [options] \"project-1,project-2,project-3\""
        ""
        "Options:"
        options-summary
        ""
        "To specify the namespaces and resources to be shared, you must define them in project.clj. i.e"
        ":ns-sync {:test-cmd    [[\"lein\" \"test\"]]"
        "          :namespaces  [\"namespace.to.be.sync.1\" \"namespace.to.be.sync.2\"]"
        "          :resources   [\"resource.to.be.sync.1\"  \"resource.to.be.sync.2\" ]}"
        ""]
       (str/join \newline)))

(defn sync [project-desc & args]
  (let [{:keys [options arguments summary errors]} (cli/parse-opts args cli-options)]
    (cond
      (not-empty errors) (m/info errors \newline (usage summary))
      (= 1 (count arguments)) (execute-program
                               (u/split (first arguments))
                               project-desc
                               options)
      :else (main/abort (usage summary)))
    (main/exit)))