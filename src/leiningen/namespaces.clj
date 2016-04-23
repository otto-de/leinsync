(ns leiningen.namespaces
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [leiningen.constant :as c]
            [leiningen.core.main :as m]
            [clojure.math.combinatorics :as combo]
            [leiningen.core.project :as p])
  (:import (java.io FileNotFoundException)))

(defn test-or-source-ns [ns]
  (if (or (.endsWith ns "-test") (.contains ns "test")) "test" "src"))

(defn split-path [path]
  {:src-or-test (test-or-source-ns path)
   :name-space  (-> path
                    (str/replace #"-" "_")
                    (str/replace #"\." "/")
                    (str c/clojure-file-ending))})

(defn ns->target-path [path project]
  (let [{path :src-or-test ns :name-space} (split-path path)]
    (str "../" project "/" path "/" ns)))

(defn ns->source-path [path]
  (let [{path :src-or-test ns :name-space} (split-path path)]
    (str path "/" ns)))

(defn update-files! [from-file to-file]
  (m/info "UPDATE " to-file " with the content of " from-file)
  (try
    (io/make-parents (io/file to-file))
    (spit (io/file to-file) (slurp (io/file from-file)))
    (catch FileNotFoundException e
      (m/info c/warning (.getMessage e)))))

(defn get-project-sync-ns [project-file-path selector]
  (let [project-clj (p/read-raw project-file-path)
        sync-ns (selector project-clj)]
    (if (nil? sync-ns)
      (throw (RuntimeException. "sync namespace is not defined"))
      sync-ns)))

(defn should-update-ns? [ns target-project]
  (let [project-sync-ns (-> (str "../" target-project "/project.clj")
                            (get-project-sync-ns c/sync-spec-seletor)
                            (set))]
    (contains? project-sync-ns ns)))

(defn update-name-space! [name-space target-project]
  (if (should-update-ns? name-space target-project)
    (update-files! (ns->source-path name-space) (ns->target-path name-space target-project))
    (m/info "Not update:" name-space " of " target-project)))

(defn cartesian-product [c1 c2]
  (combo/cartesian-product c1 c2))

(defn update-ns-of-projects! [projects namespaces]
  (try
    (doseq [[namespace project] (cartesian-product namespaces projects)]
      (update-name-space! namespace project))
    (catch Exception e (m/info "Error : " (.getMessage e)))))