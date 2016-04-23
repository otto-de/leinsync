(ns leiningen.collections-test
  (:require [clojure.test :refer :all]
            [leiningen.namespaces :as coll]))


(deftest ^:unit flap-map-1
  (is (= '([:a :1] [:a :2] [:a :3] [:b :1] [:b :2] [:b :3] [:c :1] [:c :2] [:c :3])
         (coll/cartesian-product '(:a :b :c) '(:1 :2 :3)))))

(deftest ^:unit flap-map-2
  (is (= nil
         (coll/cartesian-product '(:a :b :c) '()))))

(deftest ^:unit flap-map-3
  (is (= nil
         (coll/cartesian-product '() '(:1 :2 :3)))))

(deftest ^:unit flap-map-4
  (is (= '([:a :1])
         (coll/cartesian-product '(:a) '(:1)))))

