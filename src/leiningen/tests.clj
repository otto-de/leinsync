(ns leiningen.tests
  (:require [leiningen.utils :as u]
            [clojure.string :as str]
            [leiningen.core.main :as m]
            [clojure.pprint :as pp]))

(def test-cmd-def [:ns-sync :test-cmd])
(def standard-test-cmd [["./lein.sh" "clean"] ["./lein.sh" "test"]])

(defn test-cmd [project-desc]
  (let [cmds (get-in project-desc test-cmd-def)]
    (if (empty? cmds) standard-test-cmd cmds)))

(defn unterline-failed-cmd [{cmd :cmd result :result}]
  (if (= result :failed)
    {(keyword cmd) (str "==> " result)}
    {(keyword cmd) result}))

(defn merge-test-status [aggregrated-status cmd-status]
  (->> cmd-status
       (map unterline-failed-cmd)
       (reduce merge)
       (merge aggregrated-status)))

(defn test-status [project cmd-results]
  (if (empty? (filter #(= (:result %) :failed) cmd-results))
    (merge-test-status {:project project :result :passed} cmd-results)
    (merge-test-status {:project project :result :failed} cmd-results)))

(defn lein-test [project project-desc]
  (->> (test-cmd project-desc)
       (map u/run-cmd)
       (test-status project)))

(defn log-test-hints [results]
  (pp/print-table results)
  (let [passed-projects (->> results
                             (filter #(= (:result %) :passed))
                             (map :project)
                             (str/join ","))
        failed-project (->> results
                            (filter #(= (:result %) :failed))
                            (map :project)
                            (str/join ","))]
    (when (not (empty? failed-project))
      (m/info "\n* Please have a look  at the failed project(s):" failed-project))
    (when (not (empty? passed-projects))
      (m/info "\n* Tests are passed on project(s):" passed-projects "\n\n")
      (m/info "To see changes : lein sync" passed-projects "--diff")
      (m/info "To commit      : lein sync" passed-projects "--commit")
      (m/info "To push        : lein sync" passed-projects "--push"))))