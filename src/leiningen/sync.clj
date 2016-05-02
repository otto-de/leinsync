(ns leiningen.sync
  (:refer-clojure :exclude [sync])
  (:require [leiningen.core.main :as main]
            [clojure.string :as str]
            [leiningen.namespaces :as ns]
            [leiningen.utils :as u]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as str]))

(defn update-and-test! [projects namespaces]
  (do (u/run! ns/update-ns-of-projects! projects namespaces)
      (u/run! ns/test-all projects)
      (main/info "lein sync" (str/join "," projects) "--diff" "to see changes")
      (main/info "lein sync" (str/join "," projects) "--commit" "to commit changes")
      (main/info "lein sync" (str/join "," projects) "--push" "to push unpushed commit")))

(defn do-update [projects namespaces {no-test? :notest
                                      reset?   :reset
                                      diff?    :diff
                                      pull?    :pull
                                      push?    :push
                                      status?  :status
                                      commit?  :commit}]
  (cond
    (true? status?) (u/run! ns/status_all projects)
    (true? push?) (u/run! ns/push-all! projects)
    (true? pull?) (u/run! ns/pull-rebase-all! projects)
    (true? commit?) (u/run! ns/commit-all! projects)
    (true? diff?) (u/run! ns/show-all-diff projects)
    (true? reset?) (u/run! ns/reset-all! projects)
    (true? no-test?) (u/run! ns/update-ns-of-projects! projects namespaces)
    :else (update-and-test! projects namespaces)))

(defn one-arg-program [project-description projects options]
  (do-update (u/split projects) (ns/spec-selector project-description) options))

(defn two-args-program [projects namespaces options]
  (do-update (u/split projects) (u/split namespaces) options))

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
  (let [{:keys [options arguments summary]} (parse-opts args cli-options)]
    (cond
      (= 1 (count arguments)) (one-arg-program project-desc (first arguments) options)
      (= 2 (count arguments)) (two-args-program (first arguments) (second arguments) options)
      :else (main/abort (usage summary)))
    (main/exit)))