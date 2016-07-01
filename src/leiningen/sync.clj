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

(def cli-options
  [["-d" "--deps global|profile-name" "List all profile/global deps on projects"
    :parse-fn #(keyword %)
    :validate [#(or (= :global %) (not (empty? (name %)))) "--deps must be global or profile"]]
   ["-l" "--list" "List resources to be synchronized"]
   ["-n" "--notest" "Synchronize shared code base without executing tests on target projects"]
   ["-t" "--test" "Executing tests on target projects"]
   ["-s" "--status" "Check status on target projects"]
   ["-r" "--reset" "Reset all the uncommited changes in all target projects"]
   ["-c" "--commit" "Commit change on target projects"]
   ["-p" "--pull" "Pull rebase on target projects"]
   ["-u" "--push" "Push on target projects"]])

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

(defn error-hint [errors summary project-desc]
  (m/info (str/join " " errors))
  (if (not (nil? (:profiles project-desc)))
    (m/info "Possible profile names:" (map name (keys (:profiles project-desc)))))
  (m/info (usage summary)))

(defn sync [project-desc & args]
  (let [{:keys [options arguments summary errors]} (cli/parse-opts args cli-options)]
    (cond
      (not-empty errors)
      (error-hint errors summary project-desc)
      (= 1 (count arguments))
      (execute-program (u/split (first arguments)) project-desc options)
      :else (m/abort (usage summary)))
    (m/exit)))