(ns leiningen.program-test
  (:require [clojure.test :refer :all]
            [leiningen.sync :as s]))

(deftest ^:unit test-split-string
  (is (= ["project1" "project2" "project3"]
         (s/split "project1,project2,project3")))

  (is (= ["de.otto.one.cool.ns1" "de.otto.one.cool.ns2"]
         (s/split "de.otto.one.cool.ns1,de.otto.one.cool.ns2"))))
