(ns leiningen.utils
  (:refer-clojure :exclude [run!])
  (:require [clojure.string :as str]
            [leiningen.core.main :as m])
  (:import (jnr.posix POSIXFactory)
           (java.io File)))

(defn change-dir-to [relative-path]
  (let [absolute-path (.getCanonicalPath (new File relative-path))]
    (.chdir (POSIXFactory/getPOSIX) absolute-path)
    (System/setProperty "user.dir" absolute-path)))

(defn is-success? [result]
  (= (:exit result) 0))

(defn output-of [result]
  (:out result))

(defn split [input] (str/split input #","))

(defn run! [action & args]
  (try
    (apply action args)
    (catch Exception e (m/info "Error : " (.getMessage e)))))

(defn run-command-on [project command & args]
  (let [original-dir (System/getProperty "user.dir")]
    (change-dir-to (str original-dir "/../" project))
    (apply command args)
    (change-dir-to original-dir)))

(defn get-input [prompt]
  (m/info prompt)
  (read-line))

(defn ask-user
  ([question] (ask-user question (fn [_] true)))
  ([question validate-fn]
   (loop [input (get-input question)]
     (if (validate-fn input)
       input
       (do
         (m/info "the input was not correct")
         (recur (get-input question)))))))