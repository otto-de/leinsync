(ns leiningen.utils
  (:require [clojure.string :as str])
  (:import (jnr.posix POSIXFactory)))

(defn change-dir-to [path]
  (.chdir (POSIXFactory/getPOSIX) path)
  (System/setProperty "user.dir" path))

(defn is-success? [result]
  (= (:exit result) 0))

(defn output-of [result]
  (:out result))

(defn split [input] (str/split input #","))