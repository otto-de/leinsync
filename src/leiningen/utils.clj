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

(defn error-of [result]
  (:err result))

(defn split [input] (str/split input #","))

(defn run! [action & args]
  (try
    (apply action args)
    (catch Exception e (m/info "Error : " (.getMessage e)))))

(defn format-str [input max-length]
  (let [input-length (count input)
        diff (- max-length input-length)]
    (cond
      (pos? diff) (str (str/upper-case input) (str/join "" (repeat diff " ")))
      (neg? diff) (subs (str/upper-case input) 0 max-length)
      :else (str/upper-case input))))

(defn run-command-on [project command & args]
  (m/info "|===================================|"
          (format-str project 7)
          "|===================================|")
  (let [original-dir (System/getProperty "user.dir")]
    (change-dir-to (str original-dir "/" project))
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