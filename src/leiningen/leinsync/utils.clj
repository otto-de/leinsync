(ns leiningen.leinsync.utils
  (:refer-clojure :exclude [run!])
  (:require [clojure.string :as str]
            [leiningen.core.main :as m]
            [clojure.java.io :as io]
            [clojure.math.combinatorics :as combo]
            [clojure.java.shell :as sh])
  (:import (jnr.posix POSIXFactory)
           (java.io File)
           (java.util Properties)))

(def verbose false)

(defn change-dir-to [relative-path]
  (let [absolute-path (.getCanonicalPath (new File relative-path))]
    (if (not (.exists (io/as-file absolute-path)))
      (throw (RuntimeException. (str "The folder " absolute-path " does not exist"))))
    (.chdir (POSIXFactory/getPOSIX) absolute-path)
    (System/setProperty "user.dir" absolute-path)))

(defn is-success? [result]
  (= (:exit result) 0))

(defn sub-str [input length]
  (let [input-length (count input)]
    (if (<= input-length length)
      input
      (str (subs input 0 (min length input-length)) " ..."))))

(declare output-of)

(defn split-output-of [result]
  (str/split-lines (str/trim (output-of result))))

(defn output-of
  ([result] (:out result))
  ([result separator]
   (str/join separator (split-output-of result))))

(defn error-of
  ([result] (:err result))
  ([result separator]
   (str/join separator (str/split-lines (str/trim (error-of result))))))

(defn split [input] (str/split input #","))

(defn run! [action & args]
  (try
    (apply action args)
    (catch Exception e
      (if verbose
        (m/info "Error: " (.getMessage e) e)
        (m/info "Error: " (.getMessage e))))))

(defn format-str [input max-length]
  (let [diff (- max-length (count input))]
    (cond
      (pos? diff) (str input (str/join "" (repeat diff " ")))
      (neg? diff) (str (subs input 0 (- max-length 2)) "..")
      :else input)))

(defn run-command-on [project command & args]
  (if verbose
    (m/info "\n*************************" (format-str project 12) "*************************"))
  (let [original-dir (System/getProperty "user.dir")
        _ (change-dir-to (str original-dir "/" project))
        return (apply command args)
        _ (change-dir-to original-dir)]
    return))

(defn capture-input [prompt]
  (m/info prompt)
  (read-line))

(defn yes-or-no [input]
  (or (= input "y")
      (= input "n")))

(defn is-number [limit input]
  (let [n (read-string input)]
    (and (number? n) (< n limit))))

(defn ask-user
  ([question] (ask-user question (fn [_] true)))
  ([question validate-fn]
   (loop [input (capture-input question)]
     (if (validate-fn input)
       input
       (do
         (m/info "The input was not correct")
         (recur (capture-input question)))))))

(defn exists? [path]
  (-> path
      (io/as-file)
      (.exists)))

(defn cartesian-product [c1 c2]
  (combo/cartesian-product c1 c2))

(defn run-cmd [cmd]
  (if verbose
    (m/info "... Executing " (str/join " " cmd) "on" (output-of (sh/sh "pwd") " "))
    (m/info "... Executing " (str/join " " cmd)))
  (let [result (apply sh/sh cmd)
        cmd-str (str/join " " cmd)]
    (if (is-success? result)
      {:result :passed :cmd cmd-str}
      {:result :failed :cmd cmd-str})))

(defn get-version [name]
  (let [path (str "META-INF/maven/" name "/" name "/pom.properties")
        props (io/resource path)]
    (if props
      (with-open [stream (io/input-stream props)]
        (let [props (doto (Properties.) (.load stream))]
          (.getProperty props "version"))))))

(defn includes? [s substr]
  (.contains s substr))

(defn lazy-contains? [coll key]
  (boolean (some #(= % key) coll)))