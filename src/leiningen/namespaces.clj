(ns leiningen.namespaces
  (:refer-clojure :exclude [run!])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.math.combinatorics :as combo]
            [clojure.java.shell :as sh]
            [leiningen.utils :as u]
            [leiningen.core.project :as p]
            [leiningen.core.main :as m])
  (:import (java.io FileNotFoundException)))

(def sync-ns-def [:ns-sync :namespaces])
(def resource-def [:ns-sync :resources])
(def test-cmd-def [:ns-sync :test-cmd])
(def src-path-def [:source-paths])
(def test-path-def [:test-paths])
(def resource-path-def [:resource-paths])

(defn cartesian-product [c1 c2]
  (combo/cartesian-product c1 c2))

(defn ns-exists? [path]
  (-> path
      (io/as-file)
      (.exists)))

(defn ->target-project-path [project-name]
  (str "../" project-name))

(defn read-project-clj [project-path]
  (-> project-path
      (str "/project.clj")
      (p/read-raw)))

(defn test-cmd [target-project]
  (let [cmd (-> target-project
                (->target-project-path)
                (read-project-clj)
                (get-in test-cmd-def))]
    (if (empty? cmd)
      [["./lein.sh" "clean"] ["./lein.sh" "test"]]
      cmd)))

(defn test-or-source-namespace [namespace project-clj]
  (if (or (.endsWith namespace "-test")
          (.contains namespace "test"))
    (get-in project-clj test-path-def ["test"])
    (get-in project-clj src-path-def ["src"])))

(defn split-path [path project-desc]
  {:src-or-test    (test-or-source-namespace path project-desc)
   :namespace-path (-> path
                       (str/replace #"-" "_")
                       (str/replace #"\." "/")
                       (str ".clj"))})

(defn read-target-project-clj [p]
  (read-project-clj (->target-project-path p)))

(defn resource->target-path [resource target-project read-project-clj]
  (let [resource-folders (-> target-project
                             (read-project-clj)
                             (get-in resource-path-def))]
    (map #(str (->target-project-path target-project) "/" % "/" resource) resource-folders)))

(defn resource->source-path [resource source-project-desc]
  (map #(str % "/" resource)
       (get-in source-project-desc resource-path-def)))

(defn namespace->target-path [namespace target-project read-project-clj]
  (let [project-desc (read-project-clj target-project)
        {folders :src-or-test ns-path :namespace-path} (split-path namespace project-desc)]
    (map #(str (->target-project-path target-project) "/" % "/" ns-path) folders)))

(defn namespace->source-path [namespace source-project-desc]
  (let [{folders :src-or-test ns-path :namespace-path} (split-path namespace source-project-desc)]
    (map #(str % "/" ns-path) folders)))

(defn update-files! [from-file to-file]
  (try
    (io/make-parents (io/file to-file))
    (spit (io/file to-file) (slurp (io/file from-file)))
    (m/info "* " to-file)
    (catch FileNotFoundException e
      (m/info "* Could not synchronize" to-file "because: " (.getMessage e)))))

(defn should-update? [entry-definition entry target-project]
  (-> target-project
      (->target-project-path)
      (read-project-clj)
      (get-in entry-definition)
      (set)
      (contains? entry)))

(defn initial-question [namespace project]
  (str "The location of " namespace " on " project
       " could not be determined.  Please choose one of options:"))

(defn localtion-question-with
  ([ns project [first & rest]]
   (localtion-question-with (initial-question ns project) 0 first rest))
  ([question index first [ffirst rrest]]
   (if (nil? first)
     question
     (recur (str question "\n* " index " ==> " first)
            (inc index)
            ffirst
            rrest))))

(defn ask-for-localtion [namespace project paths]
  (nth paths
       (-> namespace
           (localtion-question-with project paths)
           (u/ask-user (partial u/is-number (count paths)))
           (read-string))))

(defn ask-for-localtion-and-update! [namespace project source-paths target-paths]
  (update-files!
   (if (= 1 (count source-paths))
     (first source-paths)
     (ask-for-localtion namespace project source-paths))
   (if (= 1 (count target-paths))
     (first target-paths)
     (ask-for-localtion namespace project target-paths))))

(defn update-file-if-exists! [name target-project source-paths target-paths]
  (let [existing-source-paths (filter ns-exists? source-paths)
        existing-target-paths (filter ns-exists? target-paths)]
    (cond
      ;source and target exist and unique
      (and (= 1 (count existing-source-paths))
           (= 1 (count existing-target-paths)))
      (update-files! (first existing-source-paths) (first existing-target-paths))
      ;source  exists, target doen't exist but its location is unique
      (and (= 1 (count existing-source-paths))
           (= 0 (count existing-target-paths))
           (= 1 (count target-paths)))
      (update-files! (first existing-source-paths) (first target-paths))
      ;source  exists, target  doen't exist and its location is unique, so ask user
      (<= 1 (count existing-source-paths))
      (ask-for-localtion-and-update! name target-project existing-source-paths target-paths)
      ;default: do nothing
      :else (m/info "WARNING: Could not find strategy to update" name "on project" target-project
                    "\n    ==>" name "may not exist on the source project"))))

(defn update-name-space! [name-space target-project source-project-desc]
  (if (should-update? sync-ns-def name-space target-project)
    (update-file-if-exists!
     name-space
     target-project
     (namespace->source-path name-space source-project-desc)
     (namespace->target-path name-space target-project read-target-project-clj))))

(defn update-resource! [resource target-project source-project-desc]
  (if (should-update? resource-def resource target-project)
    (update-file-if-exists!
     resource
     target-project
     (resource->source-path resource source-project-desc)
     (resource->target-path resource target-project read-target-project-clj))))

(defn update-namespaces! [namspaces source-project-desc]
  (m/info "\n****************** UPDATE NAMESPACE ******************\n*")
  (doseq [[namespace project] namspaces]
    (update-name-space! namespace project source-project-desc))
  (m/info "*\n******************************************************\n"))

(defn update-resouces! [resources source-project-desc]
  (m/info "\n****************** UPDATE RESOURCES ******************\n*")
  (doseq [[resource project] resources]
    (update-resource! resource project source-project-desc))
  (m/info "*\n******************************************************\n"))

(defn run-cmd [cmd]
  (m/info "... Executing " (str/join " " cmd))
  (let [result (apply sh/sh cmd)]
    (if (u/is-success? result)
      {:result :passed}
      {:result :failed :cmd (str/join " " cmd)})))

(defn lein-test [project]
  (m/info "\n... Executing tests of" project "on" (u/output-of (sh/sh "pwd")))
  (let [failed-cmd (->> project
                        (test-cmd)
                        (map run-cmd)
                        (filter #(= (:result %) :failed)))]
    (if (empty? failed-cmd)
      (do
        (m/info "===> All tests of" project "are passed\n")
        {:project project :result :passed})
      (do
        (m/info "===> On" project "some tests are FAILED when executing"
                (str/join " and " (map :cmd failed-cmd)) "\n")
        {:project project :result :failed}))))

(defn reset-project! [project]
  (m/info "\n... Reset changes of" project "on" (u/output-of (sh/sh "pwd")))
  (if (u/is-success? (sh/sh "git" "checkout" "."))
    (m/info "===> Reset all changes")
    (m/info "===> Could NOT reset changes on" project)))

(defn get-changed-files []
  (->> (sh/sh "git" "diff" "--name-only")
       (u/output-of)))

(defn diff [project]
  (let [changes (get-changed-files)]
    (if (empty? changes)
      (m/info "* No update has been applied on the project" project "\n")
      (m/info "* Changes on project" project "\n\n" changes))))

(defn commit-project! [project commit-msg]
  (if (not (empty? (get-changed-files)))
    (let [commit-result (sh/sh "git" "commit" "-am" commit-msg)]
      (if (not (u/is-success? commit-result))
        (m/info "===> Could not commit because" (u/error-of commit-result))
        (m/info "Commited")))
    (m/info "\n* No change to be committed on" project)))

(defn pull-rebase! [project]
  (m/info "\n* Pull on" project)
  (let [pull-result (sh/sh "git" "pull" "-r")]
    (if (not (u/is-success? pull-result))
      (m/info "===> Could not commit because" (u/error-of pull-result))
      (m/info (u/output-of pull-result)))))

(defn unpushed-commit []
  (-> (sh/sh "git" "diff" "origin/master..HEAD" "--name-only")
      (u/output-of)))

(defn push! [p]
  (let [push-result (sh/sh "git" "push" "origin")]
    (if (not (u/is-success? push-result))
      (m/info "===> Could not push on" p "because" (u/error-of push-result))
      (m/info (u/output-of push-result)))))

(defn check-and-push! [project]
  (if (empty? (unpushed-commit))
    (m/info "\n ===> Nothing to push on" project)
    (if (= "yes" (-> (str "\n*Are you sure to push on " project "? (yes/no)")
                     (u/ask-user u/yes-or-no)))
      (push! project))))

(defn status [project]
  (m/info "\n * Status of" project)
  (let [status-result (sh/sh "git" "status")]
    (if (not (u/is-success? status-result))
      (m/info "===> Could not get status because" (u/error-of status-result))
      (m/info (u/output-of status-result)))))

(defn test-all [projects]
  (doall
   (map
    #(u/run-command-on (->target-project-path %) lein-test %)
    projects)))

(defn reset-all! [projects _]
  (doseq [p projects]
    (u/run-command-on (->target-project-path p) reset-project! p)))

(defn commit-all! [projects _]
  (let [commit-msg (->> projects
                        (str/join ",")
                        (str "\nPlease enter the commit message for projects: ")
                        (u/ask-user))]
    (doseq [p projects]
      (u/run-command-on (->target-project-path p) commit-project! p commit-msg))))

(defn pull-rebase-all! [projects _]
  (doseq [p projects]
    (u/run-command-on (->target-project-path p) pull-rebase! p)))

(defn push-all! [projects _]
  (doseq [p projects]
    (u/run-command-on (->target-project-path p) check-and-push! p)))

(defn status_all [projects _]
  (doseq [p projects]
    (u/run-command-on (->target-project-path p) status p)))

(defn show-all-diff [projects _]
  (doseq [p projects]
    (u/run-command-on (->target-project-path p) diff p)))

(defn update-projects! [target-projects source-project-desc]
  (let [namespaces (cartesian-product (get-in source-project-desc sync-ns-def) target-projects)
        resources (cartesian-product (get-in source-project-desc resource-def) target-projects)]
    (if (not (empty? namespaces)) (update-namespaces! namespaces source-project-desc))
    (if (not (empty? resources)) (update-resouces! resources source-project-desc))))

(defn update-and-test! [target-projects src-project-desc]
  (update-projects! target-projects src-project-desc)
  (let [passed-projects (->> (test-all target-projects)
                             (filter #(= (:result %) :passed))
                             (map :project)
                             (str/join ","))]
    (when (not (empty? passed-projects))
      (m/info "* Tests are passed on projects:" passed-projects "\n")
      (m/info "To see changes : lein sync" passed-projects "--diff")
      (m/info "To commit      : lein sync" passed-projects "--commit")
      (m/info "To push        : lein sync" passed-projects "--push"))))