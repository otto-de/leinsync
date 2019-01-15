(ns leiningen.leinsync.packages
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [leiningen.core.main :as m]
            [leiningen.leinsync.namespaces :as ns]
            [leiningen.leinsync.project-reader :as pr])
  (:import (java.io File)))

(def package-def [:ns-sync :packages])

(defn get-src-test-folders [project-desc]
  (->> (get-in project-desc ns/src-path-def ["src"])
       (into (get-in project-desc ns/test-path-def ["test"]))
       (set)))

(defn is-package? [[_ ^File f]]
  (and (.exists f) (.isDirectory f)))

(defn folder-name-of [path]
  (last (str/split path #"/")))

(defn files-of-package [^File folder]
  (.listFiles folder))

(defn get-package-path [package]
  (str/replace package #"\." "/"))

(defn make-sync-work-unit [package-path source-project-desc target-projects-desc]
  (let [target-package-folders (get-src-test-folders target-projects-desc)]
    (->> (get-src-test-folders source-project-desc)
         (map (fn [p] [(folder-name-of p) (io/file (str p "/" package-path))]))
         (filter is-package?)
         (filter (fn [[folder]] #(contains? target-package-folders folder)))
         (map (fn [[folder ^File package]] [folder (files-of-package package)])))))

(defn write-to-file! [to-file from-file]
  (io/make-parents to-file)
  (spit to-file (slurp from-file)))

(defn update-file [target-project folder-name package-path ^File src-package-file]
  (let [src-package-file-name (.getName src-package-file)]
    (m/info "*** Update namespace" src-package-file-name)
    (-> (pr/->target-project-path target-project)
        (str "/" folder-name "/" package-path "/" src-package-file-name)
        (io/file)
        (write-to-file! src-package-file))))

(defn delete-package-files-of-target-project [target-project folder-name package-path]
  (try
    (doseq [file-to-delete (-> (pr/->target-project-path target-project)
                               (str "/" folder-name "/" package-path)
                               ^File (io/file)
                               .listFiles)]
      (io/delete-file file-to-delete true))
    (catch Exception e
      (m/info "**** [Error] when deleting a file of the folder:" folder-name (.getMessage e)))))

(defn write-package-files-from-source-to-target-project [folder-name package-path src-package-files target-project]
  (try
    (doseq [^File src-package-file src-package-files]
      (update-file target-project folder-name package-path src-package-file))
    (catch Exception e
      (m/info "**** [Error] when updating a file of the folder:" folder-name (.getMessage e)))))

(defn update-package! [package target-project source-project-desc target-projects-desc]
  (let [package-path (get-package-path package)]
    (doseq [[folder-name src-package-files] (make-sync-work-unit package-path source-project-desc target-projects-desc)]
      (delete-package-files-of-target-project target-project folder-name package-path)
      (write-package-files-from-source-to-target-project folder-name package-path src-package-files target-project))))

(defn update-packages! [packages source-project-desc target-projects-desc]
  (m/info "\n*********************** UPDATE PACKAGES ***********************\n*")
  (doseq [[package target-project] packages]
    (m/info "\n* Update package" package "to the project" (str/upper-case target-project))
    (update-package! package target-project source-project-desc ((keyword target-project) target-projects-desc))))