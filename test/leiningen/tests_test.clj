(ns leiningen.tests-test
  (:require [clojure.test :refer :all]
            [leiningen.tests :as t]))

(deftest ^:unit test-cmd
  (is (= t/standard-test-cmd (t/test-cmd {})))
  (is (= [["test" "command"]]
         (t/test-cmd {:ns-sync {:test-cmd [["test" "command"]]}}))))

(deftest ^:unit test-status
  (is (= {:project "project" :result :passed}
         (t/test-status "project" [])))
  (is (= {:project "project" :result :failed}
         (t/test-status "project" [{:result :failed}]))))
