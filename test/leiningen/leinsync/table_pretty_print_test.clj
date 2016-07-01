(ns leiningen.leinsync.table-pretty-print-test
  (:require [clojure.test :refer :all]
            [leiningen.leinsync.table-pretty-print :as pp]))

(deftest ^:unit print-table
  (testing "rows with the same structure"
    (let [print-state (atom [])
          print-fn (fn info [& args] (swap! print-state conj args))
          rows [{:k1 :v1} {:k2 :v2} {:k3 :v3}]
          _ (pp/print-table rows false print-fn)]
      (is (= [nil
              ["| :k1 | :k2 | :k3 |"]
              ["|-----+-----+-----|"]
              ["| :v1 |     |     |"]
              ["|     | :v2 |     |"]
              ["|     |     | :v3 |"]]
             @print-state))))

  (testing "rows with different structure"
    (let [print-state (atom [])
          print-fn (fn info [& args] (swap! print-state conj args))
          rows [{:k1 :v1} {:k1 :v1 :k2 :k2} {:k3 :v3}]
          _ (pp/print-table rows false print-fn)]
      (is (= [nil
              ["| :k1 | :k2 | :k3 |"]
              ["|-----+-----+-----|"]
              ["| :v1 |     |     |"]
              ["| :v1 | :k2 |     |"]
              ["|     |     | :v3 |"]]
             @print-state)))))
