(ns leiningen.git
  (:require [leiningen.utils :as u]
            [clojure.java.shell :as sh]
            [leiningen.core.main :as m]
            [clojure.pprint :as pp]))

(def output-length 120)

(defn log-git-status [status]
  (pp/print-table status)
  (m/info "\n"))

(defn get-changed-files []
  (u/output-of (sh/sh "git" "diff" "--name-only") " "))

(defn reset-project! [project]
  (if (u/is-success? (sh/sh "git" "checkout" "."))
    {:project project :status :reset}
    {:project project :status (str "==>" :failed)}))

(defn diff [project]
  (let [changes (get-changed-files)]
    (if (empty? changes)
      {:project project :diff :no-change}
      {:project project :diff changes})))

(defn pull-rebase! [project]
  (let [pull-result (sh/sh "git" "pull" "-r")]
    (if (not (u/is-success? pull-result))
      {:project project
       :status (str "==>" :failed)
       :cause (u/sub-str (u/error-of pull-result " ") output-length)}
      {:project project
       :status :pulled
       :details (u/sub-str (u/output-of pull-result " ") output-length)})))

(defn unpushed-commit []
  (u/output-of (sh/sh "git" "diff" "origin/master..HEAD" "--name-only")))

(defn push! [project]
  (let [push-result (sh/sh "git" "push" "origin")]
    (if (not (u/is-success? push-result))
      {:project project
       :status (str "==>" :failed)
       :cause (u/sub-str (u/error-of push-result " ") output-length)}
      {:project project
       :status :pushed
       :cause "Nothing to push on"})))

(defn check-and-push! [project]
  (if (empty? (unpushed-commit))
    {:project project :status :skipped :cause "Nothing to push on"}
    (if (= "y" (-> (str "\n* Are you sure to push on " project "? (y/n)")
                   (u/ask-user u/yes-or-no)))
      (push! project))))

(defn status [project]
  (let [status-result (sh/sh "git" "status" "--short")]
    (if (not (u/is-success? status-result))
      {:project project :status :error}
      {:project project
       :status  (if (empty? (u/output-of status-result ""))
                  :no-change
                  (u/output-of status-result " "))})))

(defn commit-project! [project commit-msg]
  (if (empty? (get-changed-files))
    {:project        project
     :status         :skipped
     :commit-message commit-msg
     :cause          "No change to be committed on"}
    (let [commit-result (sh/sh "git" "commit" "-am" commit-msg)]
      (if (u/is-success? commit-result)
        {:project        project
         :status         :commited
         :commit-message commit-msg}
        {:project        project
         :status         (str "==>" :failed)
         :commit-message commit-msg
         :cause          (u/sub-str (u/error-of commit-result " ") output-length)}))))