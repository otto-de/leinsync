(ns leiningen.sync
  (:refer-clojure :exclude [sync])
  (:require [leiningen.core.main :as main]
            [leiningen.constant :as c]
            [clojure.string :as str]
            [leiningen.namespaces :as ns]))

(defn split [input] (str/split input #","))

(defn one-arg-program [project-description projects]
  (ns/update-ns-of-projects! (split projects) (c/sync-spec-seletor project-description)))

(defn two-args-program [projects namespaces]
  (ns/update-ns-of-projects! (split projects) (split namespaces)))

(defn sync [project-desc & args]
  (cond
    (= 1 (count args)) (one-arg-program project-desc (first args))
    (= 2 (count args)) (two-args-program (first args) (second args))
    :else (main/abort c/usage-msg))
  (main/exit))
