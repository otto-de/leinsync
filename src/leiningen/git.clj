(ns leiningen.git
  (:require [leiningen.utils :as u]
            [clojure.java.shell :as sh]
            [leiningen.core.main :as m]))

(defn get-changed-files []
  (u/output-of (sh/sh "git" "diff" "--name-only")))

(defn reset-project! [project]
  (m/info "\n... Reset changes of" project "on" (u/output-of (sh/sh "pwd")))
  (if (u/is-success? (sh/sh "git" "checkout" "."))
    (m/info "===> Reset all changes")
    (m/info "===> Could NOT reset changes on" project)))

(defn diff [project]
  (let [changes (get-changed-files)]
    (if (empty? changes)
      (m/info "* No update has been applied on the project" project "\n")
      (m/info "* Changes on project" project "\n\n" changes))))

(defn pull-rebase! [project]
  (m/info "\n* Pull on" project)
  (let [pull-result (sh/sh "git" "pull" "-r")]
    (if (not (u/is-success? pull-result))
      (m/info "===> Could not commit because" (u/error-of pull-result))
      (m/info (u/output-of pull-result)))))

(defn unpushed-commit []
  (u/output-of (sh/sh "git" "diff" "origin/master..HEAD" "--name-only")))

(defn push! [p]
  (let [push-result (sh/sh "git" "push" "origin")]
    (if (not (u/is-success? push-result))
      (m/info "===> Could not push on" p "because" (u/error-of push-result))
      (m/info (u/output-of push-result)))))

(defn check-and-push! [project]
  (if (empty? (unpushed-commit))
    (m/info "\n ===> Nothing to push on" project)
    (if (= "y" (-> (str "\n* Are you sure to push on " project "? (y/n)")
                   (u/ask-user u/yes-or-no)))
      (push! project))))

(defn status [project]
  (m/info "\n * Status of" project)
  (let [status-result (sh/sh "git" "status")]
    (if (not (u/is-success? status-result))
      (m/info "===> Could not get status because" (u/error-of status-result))
      (m/info (u/output-of status-result)))))

(defn commit-project! [project commit-msg]
  (if (not (empty? (get-changed-files)))
    (let [commit-result (sh/sh "git" "commit" "-am" commit-msg)]
      (if (not (u/is-success? commit-result))
        (m/info "===> Could not commit because" (u/error-of commit-result))
        (m/info "Commited")))
    (m/info "\n* No change to be committed on" project)))

