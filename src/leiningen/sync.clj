(ns leiningen.sync
  (:refer-clojure :exclude [sync])
  (:require [leiningen.leinsync.utils :as u]
            [leiningen.leinsync.commands :as c]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [leiningen.core.main :as m]))

(defn find-command [options commands]
  (let [option-keys (keys options)]
    (->> option-keys
         (select-keys commands)
         (reduce-kv (fn [m k f] (conj m (partial u/run! f (k options)))) []))))

(defn ->commands [options commands-map]
  (let [commands (find-command options commands-map)]
    (if (empty? commands)
      (find-command (:default commands-map) commands-map)
      commands)))

(defn execute-program [target-projects source-project-desc options]
  (doseq [command (->commands options c/SYNC-COMMANDS)]
    (command target-projects source-project-desc)))

(defn cli-options [profiles]
  [["-d" "--deps global|profile-name" "List all profile/global deps on projects"
    :parse-fn keyword
    :validate [#(or (= :global %) (u/lazy-contains? profiles (name %)))
               (str "--deps must be one of: " (conj profiles "global"))]]
   ["-l" "--list" "List resources to be synchronized"]
   ["-n" "--notest" "Synchronize shared code base without executing tests on target projects"]
   ["-t" "--test" "Executing tests on target projects"]
   ["-s" "--status" "Check status on target projects"]
   ["-r" "--reset" "Reset all the uncommited changes in all target projects"]
   ["-c" "--commit" "Commit change on target projects"]
   ["-p" "--pull" "Pull rebase on target projects"]
   ["-u" "--push" "Push on target projects"]])

(defn usage [options-summary]
  (str/join \newline
            ["sync is a Leiningen plugin to synchronize same codebase between different clojure projects"
             (str "version: " (u/get-artifact-version "sync"))
             ""
             "Usage:"
             "  *  lein sync [options] \"project-1,project-2,project-3\""
             ""
             "Options:"
             options-summary
             ""
             "To specify the namespaces and resources to be shared, you must define them in project.clj. i.e"
             ":ns-sync {:test-cmd    [[\"lein\" \"test\"]]"
             "          :namespaces  [\"namespace.to.be.sync.1\" \"namespace.to.be.sync.2\"]"
             "          :resources   [\"resource.to.be.sync.1\"  \"resource.to.be.sync.2\" ]}"
             ""]))

(defn get-profiles [{profiles :profiles}]
  (if (nil? profiles)
    []
    (set (map name (keys profiles)))))

(defn sync [project-desc & args]
  (let [profiles (get-profiles project-desc)
        {:keys [options arguments summary errors]} (cli/parse-opts args (cli-options profiles))]
    (cond
      (not-empty errors) (m/info (str/join " " errors))
      (= 1 (count arguments))
      (execute-program (u/split (first arguments)) project-desc options)
      :else (m/abort (usage summary)))
    (m/exit)))