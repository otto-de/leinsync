(ns leiningen.namespaces
  (:refer-clojure :exclude [run!])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [leiningen.core.main :as m]
            [clojure.math.combinatorics :as combo]
            [clojure.java.shell :as sh]
            [leiningen.utils :as u]
            [leiningen.core.project :as p])
  (:import (java.io FileNotFoundException)))

(def spec-selector :ns-sync)

(defn test-or-source-ns [ns]
  (if (or (.endsWith ns "-test") (.contains ns "test")) "test" "src"))

(defn split-path [path]
  {:src-or-test (test-or-source-ns path)
   :name-space  (-> path
                    (str/replace #"-" "_")
                    (str/replace #"\." "/")
                    (str ".clj"))})

(defn ns->target-path [path project]
  (let [{path :src-or-test ns :name-space} (split-path path)]
    (str "../" project "/" path "/" ns)))

(defn ns->source-path [path]
  (let [{path :src-or-test ns :name-space} (split-path path)]
    (str path "/" ns)))

(defn update-files! [from-file to-file]
  (try
    (io/make-parents (io/file to-file))
    (spit (io/file to-file) (slurp (io/file from-file)))
    (m/info "UPDATE" to-file)
    (catch FileNotFoundException e
      (m/info "COULD NOT UPDATE" to-file "because: " (.getMessage e)))))

(defn get-project-sync-ns [project-file-path selector]
  (let [project-clj (p/read-raw project-file-path)
        sync-ns (selector project-clj)]
    (if (nil? sync-ns)
      (throw (RuntimeException. "Sync Namespace is not defined"))
      sync-ns)))

(defn should-update-ns? [ns target-project]
  (let [project-sync-ns (-> (str "../" target-project "/project.clj")
                            (get-project-sync-ns spec-selector)
                            (set))]
    (contains? project-sync-ns ns)))

(defn update-name-space! [name-space target-project]
  (if (should-update-ns? name-space target-project)
    (update-files!
      (ns->source-path name-space)
      (ns->target-path name-space target-project))))

(defn cartesian-product [c1 c2]
  (combo/cartesian-product c1 c2))

(defn lein-test [project]
  (m/info "\n... Executing tests of" project "on" (u/output-of (sh/sh "pwd")))
  (if (and (u/is-success? (sh/sh "./lein.sh" "clean"))
           (u/is-success? (sh/sh "./lein.sh" "test")))
    (m/info "===> All Tests of" project "are passed")
    (m/info "===> Some Tests of" project "are FAILED!!!")))

(defn reset-project! [project]
  (m/info "\n... Reset changes of" project "on" (u/output-of (sh/sh "pwd")))
  (if (u/is-success? (sh/sh "git" "checkout" "."))
    (m/info "===> Reset all changes")
    (m/info "===> Could NOT reset changes on" project)))

(defn get-changed-files []
  (->> (sh/sh "git" "diff" "--name-only")
       (u/output-of)))

(defn show-changes [project]
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

(defn push! [project]
  (let [answer (-> (str "\n*Are you sure to push on " project "?")
                   (u/ask-user yes-or-no))]
    (if (= answer "yes")
      (let [push-result (sh/sh "git" "push" "origin")]
        (if (not (u/is-success? push-result))
          (m/info "===> Could not push because" (u/error-of push-result))
          (m/info (u/output-of push-result)))))))


(defn status [project]
  (m/info "\n * Status of" project)
  (let [status_result (sh/sh "git" "status")]
    (if (not (u/is-success? status_result))
      (m/info "===> Could not get status because" (u/error-of status_result))
      (m/info (u/output-of status_result)))))

(defn test-all [projects]
  (doseq [p projects]
    (u/run-command-on p lein-test p)))

(defn reset-all! [projects]
  (doseq [p projects]
    (u/run-command-on p reset-project! p)))

(defn commit-all! [projects]
  (doseq [p projects]
    (u/run-command-on p commit-project! p)))

(defn pull-rebase-all! [projects]
  (doseq [p projects]
    (u/run-command-on p pull-rebase! p)))

(defn push-all! [projects]
  (doseq [p projects]
    (u/run-command-on p push! p)))

(defn status_all [projects]
  (doseq [p projects]
    (u/run-command-on p status p)))

(defn show-all-changes [projects]
  (doseq [p projects]
    (u/run-command-on p show-changes p)))

(defn update-ns-of-projects! [projects namespaces]
  (doseq [[namespace project] (cartesian-product namespaces projects)]
    (update-name-space! namespace project)))