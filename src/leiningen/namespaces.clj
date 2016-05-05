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

(def sync-def-selector :ns-sync)
(def source-path-selector :source-paths)
(def test-path-selector :test-paths)

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

(defn test-or-source-ns [namespace project-clj]
  (if (or (.endsWith namespace "-test")
          (.contains namespace "test"))
    (get-in project-clj [test-path-selector] ["test"])
    (get-in project-clj [source-path-selector] ["src"])))

(defn split-path [path project-desc]
  {:src-or-test    (test-or-source-ns path project-desc)
   :namespace-path (-> path
                       (str/replace #"-" "_")
                       (str/replace #"\." "/")
                       (str ".clj"))})

(defn read-target-project-clj [p]
  (read-project-clj (->target-project-path p)))

(defn ns->target-path [namespace project read-fn]
  (let [project-desc (read-fn project)
        {folders :src-or-test ns-path :namespace-path} (split-path namespace project-desc)]
    (map #(str (->target-project-path project) "/" % "/" ns-path) folders)))

(defn ns->source-path [namespace project-desc]
  (let [{folders :src-or-test ns-path :namespace-path} (split-path namespace project-desc)]
    (map #(str % "/" ns-path) folders)))

(defn update-files! [from-file to-file]
  (try
    (io/make-parents (io/file to-file))
    (spit (io/file to-file) (slurp (io/file from-file)))
    (m/info "UPDATE" to-file)
    (catch FileNotFoundException e
      (m/info "* Could not synchronize" to-file "because: " (.getMessage e)))))

(defn should-update-ns? [namespace target-project]
  (let [sync-def (-> target-project
                     (->target-project-path)
                     (read-project-clj)
                     (sync-def-selector)
                     (set))]
    (contains? sync-def namespace)))

(defn update-name-space! [name-space target-project project-desc]
  (if (should-update-ns? name-space target-project)
    (let [existing-source-paths (filter ns-exists? (ns->source-path name-space project-desc))
          target-paths (ns->target-path name-space target-project read-target-project-clj)
          existing-target-paths (filter ns-exists? target-paths)]
      (cond
        (and (= 1 (count existing-source-paths))
             (= 1 (count existing-target-paths)))
        (update-files! (first existing-source-paths) (first existing-target-paths))
        
        (and (= 1 (count existing-source-paths))
             (= 0 (count existing-target-paths)))
        (m/info "To be done soon")

        :else (m/info "Could not find strategy to update" name-space "on project" target-project)))))

(defn cartesian-product [c1 c2]
  (combo/cartesian-product c1 c2))

(defn lein-test [project]
  (m/info "\n... Executing tests of" project "on" (u/output-of (sh/sh "pwd")))
  (if (and (u/is-success? (sh/sh "./lein.sh" "clean"))
           (u/is-success? (sh/sh "./lein.sh" "test")))
    (do
      (m/info "===> All Tests of" project "are passed\n")
      {:project project :result :passed})
    (do
      (m/info "===> Some Tests of" project "are FAILED!!!\n")
      {:project project :result :failed})))

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

(defn commit-project! [project]
  (if (not (empty? (get-changed-files)))
    (let [commit-result (->> (str "\nPlease enter the commit message for " project)
                             (u/ask-user)
                             (sh/sh "git" "commit" "-am"))]
      (if (not (u/is-success? commit-result))
        (m/info "===> Could not commit because" (u/error-of commit-result))))
    (m/info "\n* No change to be committed on" project)))

(defn pull-rebase! [project]
  (m/info "\n* Pull on" project)
  (let [pull-result (sh/sh "git" "pull" "-r")]
    (if (not (u/is-success? pull-result))
      (m/info "===> Could not commit because" (u/error-of pull-result))
      (m/info (u/output-of pull-result)))))

(defn yes-or-no [input]
  (or (= input "yes") (= input "no")))

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
                     (u/ask-user yes-or-no)))
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

(defn reset-all! [projects _ _]
  (doseq [p projects]
    (u/run-command-on (->target-project-path p) reset-project! p)))

(defn commit-all! [projects _ _]
  (doseq [p projects]
    (u/run-command-on (->target-project-path p) commit-project! p)))

(defn pull-rebase-all! [projects _ _]
  (doseq [p projects]
    (u/run-command-on (->target-project-path p) pull-rebase! p)))

(defn push-all! [projects _ _]
  (doseq [p projects]
    (u/run-command-on (->target-project-path p) check-and-push! p)))

(defn status_all [projects _ _]
  (doseq [p projects]
    (u/run-command-on (->target-project-path p) status p)))

(defn show-all-diff [projects _ _]
  (doseq [p projects]
    (u/run-command-on (->target-project-path p) diff p)))

(defn update-ns-of-projects! [projects namespaces project-desc]
  (doseq [[namespace project] (cartesian-product namespaces projects)]
    (update-name-space! namespace project project-desc)))

(defn update-and-test! [projects namespaces project-desc]
  (update-ns-of-projects! projects namespaces project-desc)
  (let [passed-projects (->> (test-all projects)
                             (filter #(= (:result %) :passed))
                             (map :project)
                             (str/join ","))]
    (when (not (empty? passed-projects))
      (m/info "* Tests are passed on projects:" passed-projects "\n")
      (m/info "To see changes : lein sync" passed-projects "--diff")
      (m/info "To commit      : lein sync" passed-projects "--commit")
      (m/info "To push        : lein sync" passed-projects "--push"))))