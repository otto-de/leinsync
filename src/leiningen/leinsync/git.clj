(ns leiningen.leinsync.git
  (:require [leiningen.leinsync.utils :as u]
            [clojure.java.shell :as sh]
            [leiningen.core.main :as m]
            [leiningen.leinsync.table-pretty-print :as pp]
            [clojure.string :as str]
            [leiningen.leinsync.namespaces :as ns]
            [leiningen.leinsync.packages :as package])
  (:import (java.io File)))

(def output-length 120)
(def other-change-list-limit 3)
(def sync-list-limit 6)

(defn remove-git-status [path]
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
  (pp/print-full-table status)
  (apply m/info args))

(defn status-failed []
  (str "==> " :failed))

(defn get-changed-files []
  (let [result (sh/sh "git" "ls-files" "--others" "--exclude-standard")]
    (if (u/is-success? result)
      (->> result
           (u/split-output-of)
           (remove empty?))
      [])))

(defn get-untracked-files []
  (let [result (sh/sh "git" "diff" "--name-only")]
    (if (u/is-success? result)
      (->> result
           (u/split-output-of)
           (remove empty?))
      [])))

(defn add [path]
  (let [result (sh/sh "git" "add" path)]
    (if (u/is-success? result)
      {:status :added}
      {:status :failed})))

(defn unpushed-commit-changes []
  (let [unpushed-changes (u/output-of (sh/sh "git" "diff" "origin/master..HEAD" "--name-only"))]
    {:unpushed-changes (if (empty? unpushed-changes)
                         :no-change
                         unpushed-changes)}))

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
       :status  :pushed
       :details (u/sub-str (u/output-of push-result " ") output-length)}
      {:project project
       :status  (status-failed)
       :cause   (u/sub-str (u/error-of push-result " ") output-length)})))

(defn has-no-change? [unpushed-commit-changes]
  (= :no-change (:unpushed-changes unpushed-commit-changes)))

(defn check-and-push! [project]
  (if (has-no-change? (unpushed-commit-changes))
    {:project project
     :status  :skipped
     :cause   "Nothing to push on"}
    (if (= "y" (u/ask-user (str "\n* Are you sure to push on " project "? (y/n)")
                           u/yes-or-no))
      (push! project))))

(defn other-change-status [other-resources compact?]
  (let [list-length (count other-resources)]
    (cond
      (empty? other-resources)
      {:other-changes :no-change}
      (true? compact?)
      {:other-changes (str "has " list-length " changes")}
      (> list-length other-change-list-limit)
      {:other-changes (str/join " " (concat (take other-change-list-limit other-resources) "..."))}
      :else {:other-changes (str/join " " other-resources)})))

(defn sync-change [sync-resources]
  (cond
    (empty? sync-resources) {:sync-relevant-changes :no-change}
    (> (count sync-resources) sync-list-limit)
    {:sync-relevant-changes (str/join " " (concat (take sync-list-limit sync-resources) "..."))}
    :else {:sync-relevant-changes (str/join " " sync-resources)}))

(defn is-sync-resource? [projects-desc path]
  (or (ns/is-sync-namespace? path projects-desc)
      (package/is-on-sync-package? path projects-desc)))

(defn get-details-status [status-lines projects-desc]
  (let [sync-resources (filter
                        #(is-sync-resource? projects-desc (remove-git-status %))
                        status-lines)
        other-resources (filter
                         #(and (not (u/lazy-contains? sync-resources %)) (seq %))
                         status-lines)]
    (if (> (count sync-resources) other-change-list-limit)
      (merge (sync-change sync-resources) (other-change-status other-resources true))
      (merge (sync-change sync-resources) (other-change-status other-resources false)))))

(defn details-status [project projects-desc]
  (let [status-result (sh/sh "git" "status" "--short")]
    (merge {:project project}
           (if (u/is-success? status-result)
             (-> status-result
                 (u/split-output-of)
                 (get-details-status projects-desc))
             {:status (status-failed)}))))

(defn commit! [project commit-msg]
  (let [commit-result (sh/sh "git" "commit" "-m" commit-msg)]
    (merge {:project        project
            :commit-message commit-msg}
           (if (u/is-success? commit-result)
             {:status :committed}
             {:status (status-failed)
              :cause  (-> commit-result
                          (u/error-of " ")
                          (u/sub-str output-length))}))))

(defn sync-resources-of [changed-files untracked-files projects-desc]
  (->> untracked-files
       (concat changed-files)
       (filter #(is-sync-resource? projects-desc %))))

(defn commit-project! [project commit-msg projects-desc]
  (let [add-status (->> projects-desc
                        (sync-resources-of (get-changed-files) (get-untracked-files))
                        (map add))
        failed-add-actions (filter #(= % :failed) add-status)]
    (cond
      (zero? (count add-status)) {:project        project
                                  :status         :skipped
                                  :commit-message commit-msg
                                  :cause          "No change to commit"}
      (not (zero? (count failed-add-actions))) {:project        project
                                                :status         :skipped
                                                :commit-message commit-msg
                                                :cause          add-status}
      :else (commit! project commit-msg))))

(defn last-commit-date [file]
  (let [absolute-path (.getCanonicalPath (new File file))
        git-folder (->> (str/split absolute-path #"/")
                        (drop-last)
                        (str/join "/"))]
    (->> absolute-path
         (str " git -C " git-folder " --no-pager log -1 --date=short --pretty=format:%cd ")
         (sh/sh "/bin/bash" "-c")
         (u/output-of))))