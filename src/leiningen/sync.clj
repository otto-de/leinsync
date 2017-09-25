(ns leiningen.sync
  (:refer-clojure :exclude [sync])
  (:require [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [clojure.java.io :as io]
            [leiningen.leinsync.utils :as u]
            [leiningen.core.main :as m]
            [leiningen.leinsync.commands :as c]
            [leiningen.leinsync.namespaces :as ns])
  (:import (java.util.regex PatternSyntaxException)))

(def PARENT-FOLDER "../")

(defn find-command [options commands]
  (->> options
       (keys)
       (select-keys commands)
       (reduce-kv (fn [m k f] (conj m (partial u/run! f (k options)))) [])))

(defn option->command [options commands-map]
  (or (seq (find-command options commands-map))
      (find-command (:default commands-map) commands-map)))

(defn may-update-source-project-desc [options k selector source-project]
  (if-let [include-option (k options)]
    (assoc-in source-project selector include-option)
    source-project))

(defn find-sync-projects [path]
  (->> path
       (io/file)
       (.listFiles)
       (filter (fn [f]
                 (and (.isDirectory f)
                      (u/exists? (str (u/absolute-path-of f) "/project.clj")))))
       (map #(.getName %))
       (set)))

(defn regex-matches? [regex]
  #(re-find (re-pattern (str "(?i)" regex)) %))

(defn exact-matches? [input]
  #(= % input))

(defn find-matching [projects acc part-input]
  (concat acc (or (seq (filter (exact-matches? part-input) projects))
                  (seq (filter (regex-matches? part-input) projects))
                  [])))

(defn parse-search-input [input projects]
  (reduce (partial find-matching projects) [] (u/split input)))

(defn execute-program [search-project-string
                       source-project-desc
                       options
                       sync-commands
                       parent-project-folder]
  (try
    (let [sync-projects (find-sync-projects parent-project-folder)
          target-projects (parse-search-input search-project-string sync-projects)]
      (doseq [command (option->command options sync-commands)]
        (->> source-project-desc
             (may-update-source-project-desc options :include-namespace ns/namespace-def)
             (may-update-source-project-desc options :include-resource ns/resource-def)
             (command target-projects))))
    (catch PatternSyntaxException e
      (m/info "An error occurs with the input string: " search-project-string (.getMessage e)))))

(defn cli-options [profiles]
  [["-d" "--deps global|profile-name" "List all profile/global deps on projects"
    :parse-fn keyword
    :validate [#(or (= :global %) (u/lazy-contains? profiles (name %)))
               (str "--deps must be one of: " (conj profiles "global"))]]
   ["-l" "--list diff|all" "List resources to be synchronized"
    :parse-fn keyword
    :validate [#(or (= :all %) (= :diff %))
               (str "--list must be diff or all ")]]
   ["-i" "--include-namespace ns1,ns2" "Synchronize only the passed namespaces"
    :parse-fn #(u/split %)
    :validate [#(not (empty? %))
               (str "--include-namespace should not be empty and have the pattern ns1,ns2")]]
   ["-j" "--include-resource rs1,rs2" "Synchronize only the passed resources"
    :parse-fn #(u/split %)
    :validate [#(not (empty? %))
               (str "--include-resource should not be empty and have the pattern rs1,rs2")]]
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
  (set (map name (keys profiles))))

(defn parse-args [project-desc args]
  (->> project-desc
       (get-profiles)
       (cli-options)
       (cli/parse-opts args)))

(defn sync [project-desc & args]
  (let [{:keys [options arguments summary errors]} (parse-args project-desc args)]
    (cond
      (seq errors) (m/abort (str/join " " errors))
      (not= 1 (count arguments)) (m/abort (usage summary))
      :else (execute-program (first arguments) project-desc options c/SYNC-COMMANDS PARENT-FOLDER))
    (m/exit)))