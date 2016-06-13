(ns leiningen.sync
  (:refer-clojure :exclude [sync])
  (:require [leiningen.utils :as u]
            [leiningen.commands :as command]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [leiningen.core.main :as m]))

(def sync-commands {:default [:update :test]
                    :list    command/list
                    :update  command/update-projects!
                    :notest  command/update-projects!
                    :test    command/run-test
                    :reset   command/reset-all!
                    :diff    command/show-all-diff
                    :status  command/status-all
                    :commit  command/commit-all!
                    :pull    command/pull-rebase-all!
                    :push    command/push-all!})

(defn find-command [option-keys commands]
  (->> option-keys
       (select-keys commands)
       (reduce-kv (fn [m _ f] (conj m (partial u/run! f))) [])))

(defn ->commands [options commands-map]
  (let [commands (find-command (keys options) commands-map)]
    (if (empty? commands)
      (find-command (:default commands-map) commands-map)
      commands)))

(defn execute-program [target-projects source-project-desc options]
  (doseq [command (->commands options sync-commands)]
    (command target-projects source-project-desc)))

(def cli-options
  [["-l" "--list" "List resources to be synchronized"]
   ["-n" "--notest" "Synchronize shared code base without executing tests on target projects"]
   ["-t" "--test" "Executing tests on target projects"]
   ["-d" "--diff" "Show changes on target projects"]
   ["-s" "--status" "Check status on target projects"]
   ["-r" "--reset" "Reset all the uncommited changes in all target projects"]
   ["-c" "--commit" "Commit change on target projects"]
   ["-p" "--pull" "Pull rebase on target projects"]
   ["-ps" "--push" "Push on target projects"]])

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
      :else (m/abort (usage summary)))
    (m/exit)))