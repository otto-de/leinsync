(ns leiningen.leinsync.tests-test
  (:require [clojure.test :refer :all]
            [leiningen.leinsync.tests :as t]
            [leiningen.leinsync.utils :as u]))

(deftest ^:unit test-cmd
  (is (= t/standard-test-cmd (t/test-cmd {})))
  (is (= [["test" "command"]]
         (t/test-cmd {:ns-sync {:test-cmd [["test" "command"]]}}))))

(deftest ^:unit test-status
  (is (= {:project "project", :result :passed, :test-1 :passed, :test-2 :passed}
         (t/test-status "project" [{:cmd "test-1" :result :passed}
                                   {:cmd "test-2" :result :passed}])))
  (is (= {:project "project", :result :failed, :test-1 "==> :failed", :test-2 :passed}
         (t/test-status "project" [{:cmd "test-1" :result :failed}
                                   {:cmd "test-2" :result :passed}]))))

(deftest ^:unit unterline-failed-cmd
  (is (= {:run :passed}
         (t/underline-failed-cmd {:cmd :run :result :passed})))
  (is (= {:run (str "==> " :failed)}
         (t/underline-failed-cmd {:cmd :run :result :failed}))))

(deftest ^:unit merge-test-status
  (is (= :failed
         (:result (t/merge-test-status
                   {:cmd ["test" "5"] :result :failed}
                   [:cmd ["test" "2"] {:result :failed}
                    {:cmd ["test" "3"] :result :passed}
                    {:cmd ["test" "4"] :result :failed}]))))

  (is (= :passed
         (:result (t/merge-test-status
                   {:cmd ["test" "5"] :result :passed}
                   [:cmd ["test" "2"] {:result :passed}
                    {:cmd ["test" "3"] :result :passed}
                    {:cmd ["test" "4"] :result :passed}])))))

(deftest ^:unit lein-test
  (let [state (atom [])]
    (with-redefs-fn {#'u/run-cmd (fn [cmd] (swap! state conj cmd) {:result :passed :cmd cmd})}
      #(is (= {nil :passed, :project "project", :result :passed}
              (t/lein-test "project" {:ns-sync {:test-cmd [["command"]]}}))))
    (with-redefs-fn {#'u/run-cmd (fn [cmd] (swap! state conj cmd) {:result :passed :cmd cmd})}
      #(is (= [["command"] ["command"]]
              (do (t/lein-test "project" {:ns-sync {:test-cmd [["command"]]}})
                  @state))))))
