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
  (let [original-dir (System/getProperty "user.dir")]
    (u/change-dir-to (str original-dir "/../" project))
    (m/info "\n... Executing tests of" project "on" (u/output-of (sh/sh "pwd")))
    (if (and (u/is-success? (sh/sh "./lein.sh" "clean"))
             (u/is-success? (sh/sh "./lein.sh" "test")))
      (m/info "===> All Tests of" project "are passed")
      (m/info "===> Some Tests of" project "are FAILED!!!"))
    (u/change-dir-to original-dir)))

(defn test-all [projects]
  (doseq [p projects]
    (lein-test p)))

(defn reset-project! [project]
  (let [original-dir (System/getProperty "user.dir")]
    (u/change-dir-to (str original-dir "/../" project))
    (m/info "\n... Reset changes of" project "on" (u/output-of (sh/sh "pwd")))
    (if (u/is-success? (sh/sh "git" "checkout" "."))
      (m/info "===> Reset all changes")
      (m/info "===> Could NOT reset changes on" project))
    (u/change-dir-to original-dir)))

(defn reset-all! [projects]
  (doseq [p projects]
    (reset-project! p)))

(defn show-changes [projects]
  (doseq [p projects]
    (let [original-dir (System/getProperty "user.dir")
          _ (u/change-dir-to (str original-dir "/../" p))
          changes (->> (sh/sh "git" "diff" "--name-only")
                       (u/output-of))]
      (if (empty? changes)
        (m/info "* No update has been applied on the project" p)
        (m/info "* Changes on project" p "\n\n" changes))
      (u/change-dir-to original-dir))))

(defn update-ns-of-projects! [projects namespaces]
  (doseq [[namespace project] (cartesian-product namespaces projects)]
    (update-name-space! namespace project)))

(defn run! [action & args]
  (try
    (apply action args)
    (catch Exception e (m/info "Error : " (.getMessage e)))))