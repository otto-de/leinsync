(ns leiningen.utils
  (:refer-clojure :exclude [run!])
  (:require [clojure.string :as str]
            [leiningen.core.main :as m]
            [clojure.java.io :as io]
            [clojure.math.combinatorics :as combo]
            [clojure.java.shell :as sh])
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
    (catch Exception e (m/info "Error " (.getMessage e)))))

(defn format-str [input max-length]
  (let [diff (- max-length (count input))]
    (cond
      (pos? diff) (str input (str/join "" (repeat diff " ")))
      (neg? diff) (str (subs input 0 (- max-length 2)) "..")
      :else input)))

(defn run-command-on [project command & args]
  (m/info "|****************************|"
          (format-str project 12)
          "|****************************|")
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
         (m/info "the input was not correct")
         (recur (capture-input question)))))))

(defn exists? [path]
  (-> path
      (io/as-file)
      (.exists)))

(defn cartesian-product [c1 c2]
  (combo/cartesian-product c1 c2))

(defn run-cmd [cmd]
  (m/info "... Executing " (str/join " " cmd))
  (let [result (apply sh/sh cmd)]
    (if (is-success? result)
      {:result :passed}
      {:result :failed
       :cmd    (str/join " " cmd)})))