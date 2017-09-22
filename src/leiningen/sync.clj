(ns leiningen.sync
  (:refer-clojure :exclude [sync])
  (:require [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [leiningen.leinsync.utils :as u]
            [leiningen.core.main :as m]
            [leiningen.leinsync.commands :as c]
            [leiningen.leinsync.namespaces :as ns]))

(defn find-command [options commands]
  (->> options
       (keys)
       (select-keys commands)
       (reduce-kv (fn [m k f] (conj m (partial u/run! f (k options)))) [])))

(defn option->command [options commands-map]
  (if-let [commands (seq (find-command options commands-map))]
    commands
    (find-command (:default commands-map) commands-map)))

(defn may-update-source-project-desc [source-project options]
  (if-let [include-option (:include options)]
    (assoc-in source-project ns/namespace-def include-option)
    source-project))

(defn execute-program [target-projects source-project options sync-commands]
  (doseq [command (option->command options sync-commands)]
    (->> options
         (may-update-source-project-desc source-project)
         (command target-projects))))

(defn cli-options [profiles]
  [["-d" "--deps global|profile-name" "List all profile/global deps on projects"
    :parse-fn keyword
    :validate [#(or (= :global %) (u/lazy-contains? profiles (name %)))
               (str "--deps must be one of: " (conj profiles "global"))]]
   ["-l" "--list diff|all" "List resources to be synchronized"
    :parse-fn keyword
    :validate [#(or (= :all %) (= :diff %))
               (str "--list must be diff or all ")]]
   ["-i" "--include namespaces" "Synchronize only the passed namespaces"
    :parse-fn #(u/split %)
    :validate [#(not (empty? %))
               (str "--include should not be empty and have the pattern ns1,ns2")]]
   ["-n" "--notest" "Synchronize shared code base without executing tests on target projects"]
   ["-t" "--test" "Executing tests on target projects"]
   ["-s" "--status" "Check status on target projects"]
   ["-r" "--reset" "Reset all the uncommitted changes in all target projects"]
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
  (if profiles
    (set (map name (keys profiles)))
    #{}))

(defn sync [project-desc & args]
  (let [{:keys [options arguments summary errors]} (->> project-desc
                                                        (get-profiles)
                                                        (cli-options)
                                                        (cli/parse-opts args))]
    (cond
      (not-empty errors) (m/info (str/join " " errors))
      (= 1 (count arguments)) (-> arguments
                                  (first)
                                  (u/split)
                                  (execute-program project-desc options c/SYNC-COMMANDS))
      :else (m/abort (usage summary)))
    (m/exit)))