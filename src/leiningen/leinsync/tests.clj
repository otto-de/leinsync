(ns leiningen.leinsync.tests
  (:require [clojure.string :as str]
            [leiningen.core.main :as m]
            [leiningen.leinsync.utils :as u]
            [leiningen.leinsync.table-pretty-print :as pp]))

(def test-cmd-def [:sync :test-cmd])
(def standard-test-cmd [["./lein.sh" "clean"] ["./lein.sh" "test"]])

(defn test-cmd [project-desc]
  (let [cmds (get-in project-desc test-cmd-def)]
    (if (empty? cmds) standard-test-cmd cmds)))

(defn underline-failed-cmd [{cmd :cmd result :result}]
  (if (= result :failed)
    {(keyword cmd) (str "==> " result)}
    {(keyword cmd) result}))

(defn merge-test-status [aggregated-status cmd-status]
  (->> cmd-status
       (map underline-failed-cmd)
       (reduce merge)
       (merge aggregated-status)))

(defn test-status [project cmd-results]
  (if (empty? (filter #(= (:result %) :failed) cmd-results))
    (merge-test-status {:project project :result :passed} cmd-results)
    (merge-test-status {:project project :result :failed} cmd-results)))

(defn lein-test [project project-desc]
  (m/info "\n****** Executing tests of" (u/format-str project 12) "******")
  (->> (test-cmd project-desc)
       (map u/run-cmd)
       (test-status project)))

(defn log-test-hints [results]
  (pp/print-full-table results)
  (let [passed-projects (->> results
                             (filter #(= (:result %) :passed))
                             (map :project)
                             (str/join ","))
        failed-project (->> results
                            (filter #(= (:result %) :failed))
                            (map :project)
                            (str/join ","))]
    (when (seq failed-project)
      (m/info "* Please have a look  at the failed project(s):" failed-project))
    (when (seq passed-projects)
      (m/info "\n* Tests are passed on project(s):" passed-projects "\n\n")
      (m/info "To see changes : lein sync" passed-projects "--status")
      (m/info "To commit      : lein sync" passed-projects "--commit")
      (m/info "To push        : lein sync" passed-projects "--push"))))