(ns leiningen.git
  (:require [leiningen.utils :as u]
            [clojure.java.shell :as sh]
            [leiningen.core.main :as m]
            [leiningen.table-pretty-print :as pp]
            [clojure.string :as str]
            [leiningen.namespaces :as ns]))

(def output-length 120)

(defn remove-change-status-from [path]
  (-> path
      (str/replace #"M\s" "")
      (str/replace #"A\s" "")
      (str/replace #"D\s" "")
      (str/replace #"R\s" "")
      (str/replace #"C\s" "")
      (str/replace #"U\s" "")
      (str/replace #"\?\?\s" "")
      (str/replace #"\s" "")))

(defn log-git-status [status & args]
  (pp/print-table status)
  (apply m/info args)
  (m/info "\n"))

(defn status-failed []
  (str "==> " :failed))

(defn commit-date [path]
  (u/output-of (sh/sh "git" "log" "-1" "--format=%cr" path) ""))

(defn get-changed-files []
  (let [result (sh/sh "git" "ls-files" "--others" "--exclude-standard")]
    (if (u/is-success? result)
      (->> (u/output-of result)
           (str/split-lines)
           (remove empty?))
      [])))

(defn get-untracked-files []
  (let [result (sh/sh "git" "diff" "--name-only")]
    (if (u/is-success? result)
      (->> (u/output-of result)
           (str/split-lines)
           (remove empty?))
      [])))

(defn add [path]
  (let [result (sh/sh "git" "add" path)]
    (if (u/is-success? result)
      {:status :added}
      {:status :failed})))

(defn unpushed-commit-changes []
  (u/output-of (sh/sh "git" "diff" "origin/master..HEAD" "--name-only") " "))

(defn reset-project! [project]
  (if (u/is-success? (sh/sh "git" "checkout" "."))
    {:project project :status :resetted}
    {:project project :status (status-failed)}))

(defn pull-rebase! [project]
  (let [pull-result (sh/sh "git" "pull" "-r")]
    (if (u/is-success? pull-result)
      {:project project
       :status  :pulled
       :details (u/sub-str (u/output-of pull-result " ") output-length)}
      {:project project
       :status  (status-failed)
       :cause   (u/sub-str (u/error-of pull-result " ") output-length)})))

(defn push! [project]
  (let [push-result (sh/sh "git" "push" "origin")]
    (if (u/is-success? push-result)
      {:project project
       :status  :pushed}
      {:project project
       :status  (status-failed)
       :cause   (u/sub-str (u/error-of push-result " ") output-length)})))

(defn check-and-push! [project]
  (if (empty? (unpushed-commit-changes))
    {:project project :status :skipped :cause "Nothing to push on"}
    (if (= "y" (-> (str "\n* Are you sure to push on " project "? (y/n)")
                   (u/ask-user u/yes-or-no)))
      (push! project))))

(defn get-details-status [status-result projects-desc]
  (let [synchronized-resources (->> status-result
                                    (u/output-of)
                                    (str/split-lines)
                                    (filter #(ns/sync-resources?
                                              projects-desc
                                              (remove-change-status-from %)))
                                    (str/join " "))]
    {:synchronized-resources (if (empty? synchronized-resources)
                               :no-change
                               synchronized-resources)}))

(defn details-status [project projects-desc]
  (let [status-result (sh/sh "git" "status" "--short")
        unpushed-changes (unpushed-commit-changes)]
    (if (u/is-success? status-result)
      (merge {:project                project
              :unpushed-commit-change (if (empty? unpushed-changes)
                                        :no-change unpushed-changes)}
             (get-details-status status-result projects-desc))
      {:project project :status (status-failed)})))

(defn overview-status [project]
  (let [status-result (sh/sh "git" "status" "--short")]
    (if (u/is-success? status-result)
      {:project     project
       :all-changes (if (empty? (u/output-of status-result ""))
                      :no-change
                      (u/output-of status-result " "))}
      {:project project :status (status-failed)})))

(defn commit! [project commit-msg]
  (let [commit-result (sh/sh "git" "commit" "-m" commit-msg)]
    (if (u/is-success? commit-result)
      {:project        project
       :status         :commited
       :commit-message commit-msg}
      {:project        project
       :status         (status-failed)
       :commit-message commit-msg
       :cause          (u/sub-str (u/error-of commit-result " ") output-length)})))

(defn commit-project! [project commit-msg projects-desc]
  (let [add-status (->> (concat (get-changed-files) (get-untracked-files))
                        (filter #(ns/sync-resources? projects-desc %))
                        (map add)
                        (filter #(= % :failed)))]
    (if (zero? (count add-status))
      (commit! project commit-msg)
      {:project        project
       :status         :skipped
       :commit-message commit-msg
       :cause          add-status})))