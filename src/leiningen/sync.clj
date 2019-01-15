(ns leiningen.sync
  (:refer-clojure :exclude [sync])
  (:require [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [clojure.java.io :as io]
            [leiningen.core.main :as m]
            [leiningen.leinsync.utils :as u]
            [leiningen.leinsync.commands :as c]
            [leiningen.leinsync.namespaces :as ns]
            [leiningen.leinsync.utils :as u]))

(def PARENT-FOLDER "../")
(def ALL-PROJECTS ".*")

(defn find-command [options commands]
  (->> options
       (keys)
       (select-keys commands)
       (reduce-kv (fn [m k f] (conj m (partial u/run! f (k options)))) [])))

(defn option->command [options commands-map]
  (or (seq (find-command options commands-map))
      (find-command (:default commands-map) commands-map)))

(defn is-leiningen-project? [file]
  (and (.isDirectory file)
       (u/exists? (str (u/absolute-path-of file) "/project.clj"))))

(defn find-sync-projects [path]
  (->> path
       (io/file)
       (.listFiles)
       (filter is-leiningen-project?)
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

(defn correct-project-string [limit input]
  (zero? (count (filter #(not (u/is-number limit %)) (u/split input)))))

(defn parse-project-input-string [project-map input]
  (->> input
       (u/split)
       (map read-string)
       (select-keys project-map)
       (vals)))

(defn parse-search-input-interactive [matching-project]
  (let [project-map (into (sorted-map) (map-indexed vector matching-project))]
    (m/info "* These Leiningen projects have been found:")
    (doseq [[project-index project-name] project-map]
      (m/info "  +" project-index (if (> project-index 9) "" " ") " => " project-name))
    (->> (partial correct-project-string (count project-map))
         (u/ask-user (str "\n* Please specify the project you want to sync i.e 1,2,3"))
         (parse-project-input-string project-map))))

(defn may-update-source-project-desc [{:keys [include-namespace include-resource]} source-project-desc]
  (if (or include-namespace include-resource)
    (-> source-project-desc
        (assoc-in ns/namespace-def (or include-namespace []))
        (assoc-in ns/resource-def (or include-resource [])))
    source-project-desc))

(defn enable-debug-mode! [source-project-desc]
  (when (true? (get-in source-project-desc [:ns-sync :debug]))
    (reset! u/DEBUG-MODE true)))

(defn execute-program [search-project-string
                       source-project-desc
                       {interactive-mode :interactive :as options}
                       sync-commands
                       parent-project-folder]
  (try
    (enable-debug-mode! source-project-desc)
    (let [sync-projects (find-sync-projects parent-project-folder)
          matching-project (parse-search-input search-project-string sync-projects)
          target-projects (if interactive-mode
                            (parse-search-input-interactive matching-project)
                            matching-project)]
      (doseq [command (option->command (dissoc options :interactive) sync-commands)]
        (->> source-project-desc
             (may-update-source-project-desc options)
             (command target-projects))))
    (catch Exception e
      (if @u/DEBUG-MODE (m/info e))
      (m/info "An error occurs with the input string: " search-project-string (.getMessage e)))))

(defn cli-options [profiles]
  [["-a" "--interactive" "activate interactive mode"]
   ["-d" "--deps global|profile-name" "List all profile/global deps on projects"
    :parse-fn keyword
    :validate [#(or (= :global %) (u/lazy-contains? profiles (name %)))
               (str "--deps must be one of: " (conj profiles "global"))]]
   ["-i" "--include-namespace ns1,ns2" "Synchronize only the passed namespaces"
    :parse-fn #(u/split %)
    :validate [#(not (empty? %))
               (str "--include-namespace should not be empty and have the pattern ns1,ns2")]]
   ["-j" "--include-resource rs1,rs2" "Synchronize only the passed resources"
    :parse-fn #(u/split %)
    :validate [#(not (empty? %))
               (str "--include-resource should not be empty and have the pattern rs1,rs2")]]
   ["-l" "--list diff|all" "List resources to be synchronized"
    :parse-fn keyword
    :validate [#(or (= :all %) (= :diff %))
               (str "--list must be diff or all ")]]
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
             "  *  lein sync [options] \"project-.*\""
             ""
             "Options:"
             options-summary
             ""
             "To specify the namespaces and resources to be shared, you must define them in project.clj. i.e"
             ":ns-sync {:test-cmd    [[\"lein\" \"test\"]]"
             "          :namespaces  [\"namespace.to.be.sync.1\" \"namespace.to.be.sync.2\"]"
             "          :packages    [\"package.to.be.sync.1\" \"package.to.be.sync.2\"]"
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
  (let [{:keys [options arguments summary errors]} (parse-args project-desc args)
        {interactive-mode :interactive} options]
    (cond
      (seq errors) (m/abort (str/join " " errors))
      (and (zero? (count arguments)) interactive-mode) (execute-program ALL-PROJECTS project-desc options c/SYNC-COMMANDS PARENT-FOLDER)
      (not= 1 (count arguments)) (m/abort (usage summary))
      :else (execute-program (first arguments) project-desc options c/SYNC-COMMANDS PARENT-FOLDER))
    (m/exit)))