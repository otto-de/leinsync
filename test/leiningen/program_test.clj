(ns leiningen.program-test
  (:require [clojure.test :refer :all]
            [leiningen.utils :as u]))

(deftest ^:unit test-split-string
  (is (= ["project1" "project2" "project3"]
         (u/split "project1,project2,project3")))

  (is (= ["de.otto.one.cool.ns1" "de.otto.one.cool.ns2"]
         (u/split "de.otto.one.cool.ns1,de.otto.one.cool.ns2"))))

(deftest ^:unit test-format-str
  (is (= "123  "
         (u/format-str "123" 5)))

  (is (= "123.."
         (u/format-str "1234567" 5)))

  (is (= "123456"
         (u/format-str "123456" 6))))
