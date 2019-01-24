(ns leiningen.leinsync.packages
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [leiningen.core.main :as m]
            [leiningen.leinsync.namespaces :as ns]
            [leiningen.leinsync.project-reader :as pr]
            [leiningen.leinsync.utils :as u])
  (:import (java.io File)
           (org.apache.commons.io FileUtils)))

(declare get-namespace-of-package)

(def package-def [:sync :packages])

(defn get-src-test-folders-with-type [project-desc]
  (->> (map (fn [x] [ns/src-path-def x]) (get-in project-desc ns/src-path-def ["src"]))
       (into (map (fn [x] [ns/test-path-def x]) (get-in project-desc ns/test-path-def ["test"])))
       (set)))

(defn get-src-test-folders [project-desc]
  (->> (get-in project-desc ns/src-path-def ["src"])
       (into (get-in project-desc ns/test-path-def ["test"]))
       (set)))

(defn is-package? [[_ ^File f]]
  (and (.exists f) (.isDirectory f)))

(defn path-segment [path]
  (str/split path #"/"))

(defn folder-name-of [path]
  (last (path-segment path)))

(defn files-of-package [^File folder]
  (.listFiles folder))

(defn file-names-of-package [^File folder]
  (map (fn [^File f] (.getName f)) (files-of-package folder)))

(defn get-package-path [package]
  (str/replace package #"\." "/"))

(defn get-package-name [package]
  (str/replace package #"/" "."))

(defn make-sync-work-unit [package-path source-project-desc target-projects-desc]
  (->> (get-src-test-folders-with-type source-project-desc)
       (map (fn [[folder-type folder-path]] [(folder-name-of folder-path)
                                             (io/file (str folder-path "/" package-path))
                                             folder-type]))
       (filter is-package?)
       (filter (fn [[folder]] #(contains? (get-src-test-folders target-projects-desc) folder)))
       (map (fn [[folder package folder-type]] [folder (files-of-package package) folder-type]))))

(defn get-files-of-package [project-desc package-name ^File package-file]
  (->> (file-names-of-package package-file)
       (map (fn [n] (let [namespace-file-name (first (str/split n #"\." 2))
                          namespace-name (str package-name "." (str/replace namespace-file-name #"_" "-"))
                          ^File namespace-path (io/file (str (.getAbsolutePath package-file) "/" namespace-file-name))]
                      (if (is-package? [:folder namespace-path])
                        (get-namespace-of-package project-desc namespace-name)
                        [namespace-name]))))
       (remove nil?)
       (apply concat)))

(defn get-namespace-of-package [project-desc package]
  (->> (get-src-test-folders project-desc)
       (map (fn [p] [package (io/file (str p "/" (get-package-path package)))]))
       (filter is-package?)
       (map (fn [[p package]] (get-files-of-package project-desc p package)))
       (apply concat)))

(defn get-package-resource-list [project-desc]
  (->> (get-in project-desc package-def)
       (map (partial get-namespace-of-package project-desc))
       (apply concat)))

(defn write-to-file! [to-file from-file]
  (io/make-parents to-file)
  (spit to-file (slurp from-file)))

(defn write-to-folder! [to-folder from-folder]
  (io/make-parents to-folder)
  (FileUtils/copyDirectory from-folder to-folder))

(defn update-file! [file-type write-f target-folder folder-name package-path ^File src-package-file]
  (let [src-package-file-name (.getName src-package-file)]
    (m/info "*** Update" (str file-type) src-package-file-name "on" folder-name)
    (-> target-folder
        (str "/" package-path "/" src-package-file-name)
        (io/file)
        (write-f src-package-file))))

(defn ask-for-location
  ([namespace paths] (ask-for-location namespace "source project" paths))
  ([namespace project paths]
   (-> namespace
       (ns/location-question-with project paths)
       (u/ask-user (partial u/is-number (count paths)))
       (read-string))))

(defn package-path-on-target-project [target-project package-path folder]
  (str (pr/->target-project-path target-project) "/" folder "/" package-path))

(defn update-package-entry! [target-project target-projects-desc folder-type folder-name package-path ^File src-package-file]
  (let [target-folders (->> (get-in target-projects-desc folder-type)
                            (map #(package-path-on-target-project target-project package-path %))
                            (vec))
        folder-index (cond
                       (empty? target-folders) -1
                       (= (count target-folders) 1) 0
                       :else (ask-for-location (get-package-name package-path) target-project target-folders))]
    (when (>= folder-index 0)
      (if (.isDirectory src-package-file)
        (update-file! :sub-package write-to-folder! (nth target-folders folder-index) folder-name package-path src-package-file)
        (update-file! :namespace write-to-file! (nth target-folders folder-index) folder-name package-path src-package-file)))))

(defn delete-package-files-of-target-project [target-project target-projects-desc package-path [folder-name folder-type]]
  (try
    (doseq [^File file-to-delete (-> (pr/->target-project-path target-project)
                                     (str "/" folder-name "/" package-path)
                                     ^File (io/file)
                                     .listFiles)]
      (if (.isDirectory file-to-delete)
        (FileUtils/deleteDirectory file-to-delete)
        (io/delete-file file-to-delete true)))
    (catch Exception e
      (u/print-debug (str "**** [Error] when deleting a file of the folder: " folder-name) e))))

(defn write-package-files-from-source-to-target-project [target-project target-projects-desc package-path [folder-name src-package-files folder-type]]
  (try
    (doseq [^File src-package-file src-package-files]
      (update-package-entry! target-project target-projects-desc folder-type folder-name package-path src-package-file))
    (catch Exception e
      (u/print-debug (str "**** [Error] when updating a file of the folder: " folder-name) e))))

(defn should-update-package? [package target-projects-desc]
  (contains? (set (get-in target-projects-desc package-def)) package))

(defn is-on-sync-package? [file-path projects-desc]
  (let [file-package-segment (drop-last (rest (path-segment file-path)))]
    (->> (get-in projects-desc package-def)
         (map #(str/split % #"\."))
         (filter (fn [package-segment]
                   (= package-segment
                      (take (count package-segment) file-package-segment))))
         (empty?)
         (not))))

(defn update-package! [package target-project source-project-desc target-projects-desc]
  (when (should-update-package? package target-projects-desc)
    (m/info "\n* Update package" package "to the project" (str/upper-case target-project))
    (let [package-path (get-package-path package)]
      (doseq [work-unit (make-sync-work-unit package-path source-project-desc target-projects-desc)]
        (delete-package-files-of-target-project target-project target-projects-desc package-path work-unit)
        (write-package-files-from-source-to-target-project target-project target-projects-desc package-path work-unit)))))

(defn update-packages! [packages source-project-desc target-projects-desc]
  (m/info "\n*********************** UPDATE PACKAGES ***********************\n*")
  (doseq [[package target-project] packages]
    (update-package! package target-project source-project-desc ((keyword target-project) target-projects-desc))))