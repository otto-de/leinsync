(ns leiningen.leinsync.project-reader
  (:require [leiningen.core.project :as p]))

(defn ->target-project-path [project-name]
  (str "../" project-name))

(defn read-target-project-clj [p]
  (-> (->target-project-path p)
      (str "/project.clj")
      (p/read-raw)))

(defn read-all-target-project-clj [target-projects]
  (zipmap (map keyword target-projects) (map read-target-project-clj target-projects)))
