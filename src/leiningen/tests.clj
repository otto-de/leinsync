(ns leiningen.tests
  (:require [leiningen.utils :as u]
            [leiningen.namespaces :as ns]
            [clojure.string :as str]
            [leiningen.core.main :as m]
            [clojure.java.shell :as sh]))

(def test-cmd-def [:ns-sync :test-cmd])
(def standard-test-cmd [["./lein.sh" "clean"] ["./lein.sh" "test"]])

(defn test-cmd [project-desc]
  (let [cmds (get-in project-desc test-cmd-def)]
    (if (empty? cmds) standard-test-cmd cmds)))

(defn lein-test [project project-desc]
  (m/info "\n... Executing tests of" project "on" (u/output-of (sh/sh "pwd")))
  (let [failed-cmd (->> (test-cmd project-desc)
                        (map u/run-cmd)
                        (filter #(= (:result %) :failed)))]
    (if (empty? failed-cmd)
      (do
        (m/info "===> All tests of" project "are passed\n")
        {:project project :result :passed})
      (do
        (m/info "===> On" project "some tests are FAILED when executing"
                (str/join " and " (map :cmd failed-cmd)) "\n")
        {:project project :result :failed}))))

(defn test-all [projects target-projects-desc]
  (doall
   (map
    #(u/run-command-on (ns/->target-project-path %) lein-test % (get-in target-projects-desc [(keyword %)]))
    projects)))
