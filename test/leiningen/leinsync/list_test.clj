(ns leiningen.leinsync.list-test
  (:require [clojure.test :refer :all]
            [leiningen.leinsync.list :as l]
            [leiningen.leinsync.namespaces :as ns]))

(defn- project-occurence-render [existing-path project]
  (if (empty? existing-path)
    {project {:md5 "O X"}}
    {project {:md5 "  X"}}))

(deftest ^:unit unterline-different-values
  (testing "all resources are different"
    (let [m {:a {:md5 "1"}
             :b {:md5 "2"}
             :c {:md5 "1"}
             :d {:md5 "2"}}]
      (is (= {:a (l/mark-value-with l/all-resources-different-marker (:a m))
              :b (l/mark-value-with l/all-resources-different-marker (:b m))
              :c (l/mark-value-with l/all-resources-different-marker (:c m))
              :d (l/mark-value-with l/all-resources-different-marker (:d m))}
             (l/underline-different-values m))))

    (let [m {:a {:md5 "1"}
             :b {:md5 "1"}
             :c {:md5 "1"}
             :d {:md5 "2"}
             :e {:md5 "2"}}]
      (is (= {:a (l/mark-value-with l/all-resources-different-marker (:a m))
              :b (l/mark-value-with l/all-resources-different-marker (:b m))
              :c (l/mark-value-with l/all-resources-different-marker (:c m))
              :d (l/mark-value-with l/all-resources-different-marker (:d m))
              :e (l/mark-value-with l/all-resources-different-marker (:e m))}
             (l/underline-different-values m)))))

  (testing "one resources is different"
    (let [m {:a {:md5 "1"}
             :b {:md5 "1"}
             :c {:md5 "1"}
             :d {:md5 "2"}}]
      (is (= {:a (l/mark-value-with l/all-resources-different-marker (:a m))
              :b (l/mark-value-with l/all-resources-different-marker (:b m))
              :c (l/mark-value-with l/all-resources-different-marker (:c m))
              :d (l/mark-value-with l/one-resource-different-marker (:d m))}
             (l/underline-different-values m)))))

  (testing "edge case where one project does not has this namespace"
    (let [m {:a {:md5 "1"}
             :b {:md5 "1"}
             :c {:md5 "1"}
             :d {:md5 "2"}
             :e ""}]
      (is (= {:a (l/mark-value-with l/all-resources-different-marker (:a m))
              :b (l/mark-value-with l/all-resources-different-marker (:b m))
              :c (l/mark-value-with l/all-resources-different-marker (:c m))
              :d (l/mark-value-with l/one-resource-different-marker (:d m))
              :e (l/mark-value-with l/all-resources-different-marker (:e m))}
             (l/underline-different-values m)))))

  (testing "edge case 2 different entries"
    (let [m {:a {:md5 "1"} :b {:md5 "2"}}]
      (is (= {:a (l/mark-value-with l/one-resource-different-marker (:a m))
              :b (l/mark-value-with l/one-resource-different-marker (:b m))}
             (l/underline-different-values m))))))

(deftest ^:unit occurence-map-for
  (is (= {:name "name", :package "path.to.ns"} (l/resource->package-and-name "path.to.ns.name" ns/namespace-def)))
  (is (= {:name "name", :package ""} (l/resource->package-and-name "name" ns/namespace-def)))
  (is (= {:name "data.csv"} (l/resource->package-and-name "data.csv" ns/resource-def))))

(deftest ^:unit sub-hash-str
  (is (= "12345678" (l/sub-hash-str "123456789" 8)))
  (is (= "1234567" (l/sub-hash-str "1234567" 8)))
  (is (= "X O" (l/sub-hash-str "X O" 8))))

(deftest ^:unit resource-render
  (is (= {:a "X "} (l/resource-render [] :a)))
  (is (= [:md5 :timestamp]
         (keys (:a (l/resource-render ["test-resources/dummy.clj"] :a))))))

(deftest ^:unit display-hash-value
  (is (= {:k1 "1234567"
          :k2 "=> 2016-07-07 a1805c8"}
         (l/display-hash-value
          {:k1 {:md5 "12345678", :timestamp "2016-06-07"}
           :k2 {:marker "=> " :value {:md5       "a1805c81c8ca105a0718db9fa914a3a9"
                                      :timestamp "2016-07-07"}}}
          7))))

(deftest ^:unit mark-as-diffrent
  (is (= {:k1 {:marker "=> " :value {:md5 "12345678"}},
          :k2 {:marker "=> " :value {:md5 "123456789"}}}
         (l/mark-as-different {:k1 {:md5 "12345678"}
                               :k2  {:md5 "123456789"}})))
  (is (= {:k1 {:marker "[x]=> "
               :value  {:md5 "foo"}}
          :k2 {:marker "=> "
               :value  {:md5 "bar"}}}
         (l/mark-as-different {:k1 {:md5 "foo"}
                               :k2  {:md5 "bar"}}
                              #(= "foo" (:md5 %))))))

(deftest ^:unit mark-2-different-values
  (let [marker-fn (fn ([_] {}) ([_ a] {:assertion a}))]
    (testing "if frequency of both vals > 1, no need to mark"
      (let [result (l/mark-2-different-values {:k1 {:md5 "1"}
                                               :k2 {:md5 "1"}
                                               :k3 {:md5 "2"}
                                               :k4 {:md5 "2"}}
                                              ["1" "2"]
                                              marker-fn)]
        (is (true? (empty? result)))))

    (testing "if both vals exist just onetime, mark all of them"
      (let [{assertion :assertion} (l/mark-2-different-values {:k1 {:md5 "1"}
                                                               :k2 {:md5 "2"}}
                                                              ["1" "2"]
                                                              marker-fn)]
        (is (true? (assertion {:md5 "1"})))
        (is (true? (assertion {:md5 "2"})))))

    (testing "mark val whose frequency = 1 "
      (let [{assertion :assertion} (l/mark-2-different-values {:k1 {:md5 "1"}
                                                               :k2 {:md5 "1"}
                                                               :k3 {:md5 "2"}}
                                                              ["1" "2"]
                                                              marker-fn)]
        (is (false? (assertion {:md5 "1"})))
        (is (true? (assertion {:md5 "2"}))))

      (let [{assertion :assertion} (l/mark-2-different-values {:k1 {:md5 "1"}
                                                               :k2 {:md5 "2"}
                                                               :k3 {:md5 "2"}}
                                                              ["1" "2"]
                                                              marker-fn)]
        (is (true? (assertion {:md5 "1"})))
        (is (false? (assertion {:md5 "2"})))))))

(deftest ^:unit build-resource-table
  (testing "print table structure for the namespaces"
    (let [projects {:project-1 {:ns-sync        {:namespaces ["path.to.ns1" "path.to.ns2" "path.to.ns4"]}
                                :source-paths   ["a"]
                                :test-paths     ["b"]
                                :resource-paths ["c"]}
                    :project-2 {:ns-sync        {:namespaces ["path.to.ns1" "path.to.ns3" "path.to.ns4"]}
                                :source-paths   ["d"]
                                :test-paths     ["e"]
                                :resource-paths ["f"]}}]

      (is (= [{:name "ns1", :package "path.to", :project-1 "O X", :project-2 "O X"}
              {:name "ns2", :package "path.to", :project-1 "O X"}
              {:name "ns3", :package "path.to", :project-2 "O X"}
              {:name "ns4", :package "path.to", :project-1 "O X", :project-2 "O X"}]
             (->> (l/build-resource-table projects ns/namespace-def project-occurence-render :all)
                  (sort-by :name))))))

  (testing "print table structure for the resources"
    (let [projects {:project-1 {:ns-sync        {:resources ["r1.xml" "r2.edn" "r3.json"]}
                                :source-paths   ["a"]
                                :test-paths     ["b"]
                                :resource-paths ["c"]}
                    :project-2 {:ns-sync        {:namespaces ["r1.xml" "r3.json" "r4.csv"]}
                                :source-paths   ["d"]
                                :test-paths     ["e"]
                                :resource-paths ["f"]}}]

      (is (= [{:name "csv", :package "r4", :project-2 "O X"}
              {:name "json", :package "r3", :project-2 "O X"}
              {:name "xml", :package "r1", :project-2 "O X"}]

             (->> (l/build-resource-table projects ns/namespace-def project-occurence-render :all)
                  (sort-by :name)))))))

(deftest ^:unit reduce-list-with-option-test
  (let [the-same {:package :p1 :name "ns1" :project-1 "f2a5c" :project-2 "f2a5c"}
        ns-different {:package :p2 :name "ns2" :project-1 "[x] => 2016-12-12 7d441" :project-2 " [x] => 2016-12-08 487f3"}
        data [the-same ns-different]]
    (is (= data (l/reduce-list-with-option data :all)))
    (is (= [ns-different] (l/reduce-list-with-option data :diff)))))