(ns leiningen.sync
  (:refer-clojure :exclude [sync])
  (:require [leiningen.core.main :as main]
            [leiningen.constant :as c]
            [clojure.string :as str]
            [leiningen.namespaces :as ns]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as str]))

(defn split [input] (str/split input #","))

(defn do-update [projects ns {no-test? :notest reset? :reset}]
  (cond
    (true? reset?) (ns/run! ns/reset-all! projects)
    (true? no-test?) (ns/run! ns/update-ns-of-projects! projects ns)
    :else (ns/run! ns/update-ns-of-projects-and-test! projects ns)))

(defn one-arg-program [project-description projects options]
  (do-update (split projects) (c/sync-spec-seletor project-description) options))

(defn two-args-program [projects namespaces options]
  (do-update (split projects) (split namespaces) options))

(def cli-options
  [[nil "--notest" "Synchronize shared code base without executing tests on target projects"]
   [nil "--reset" "Reset all the uncommited changes in all target projects"]])

(defn usage [options-summary]
  (->> ["sync is a Leiningen plugin to synchronize same codebase between different clojure projects"
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
        ":ns-sync [\"namespace.to.be-sync.1\" test:\"namespace.to.be-sync.2\""]
       (str/join \newline)))

(defn sync [project-desc & args]
  (let [{:keys [options arguments summary]} (parse-opts args cli-options)]
    (cond
      (= 1 (count arguments)) (one-arg-program project-desc (first arguments) options)
      (= 2 (count arguments)) (two-args-program (first arguments) (second arguments) options)
      :else (main/abort (usage summary)))
    (main/exit)))
