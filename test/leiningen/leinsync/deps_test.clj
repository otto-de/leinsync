(ns leiningen.leinsync.deps-test
  (:require [clojure.test :refer :all]
            [leiningen.leinsync.deps :as d]))

(deftest ^:unit flat-deps-list
  (is (= {:dep-1 {:k :v-1}
          :dep-2 {:k :v-2}}
         (d/flat-deps-list :k
                           [[:dep-1 :v-1]
                            [:dep-2 :v-2]]))))

(deftest ^:unit deps->project
  (let [deps-map {:deps-project-1 {:dependencies [[:dep-1 :v-1]
                                                  [:dep-2 :v-2]
                                                  [:dep-3 :v-3]]}
                  :deps-project-2 {:dependencies [[:dep-1 :v-1]]}
                  :deps-project-3 {:dependencies [[:dep-1 :v-1]
                                                  [:dep-2 :v-2]
                                                  [:dep-4 :v-5]]}}]
    (is (= [[:dep-1 {:deps-project-1 :v-1}]
            [:dep-2 {:deps-project-1 :v-2}]
            [:dep-3 {:deps-project-1 :v-3}]
            [:dep-1 {:deps-project-2 :v-1}]
            [:dep-1 {:deps-project-3 :v-1}]
            [:dep-2 {:deps-project-3 :v-2}]
            [:dep-4 {:deps-project-3 :v-5}]]
           (d/deps->project deps-map)))))

(deftest ^:unit merge-deps
  (is (= {:dep-1 {:deps-project-1 :v-1, :deps-project-2 :v-1, :deps-project-3 :v-1}
          :dep-2 {:deps-project-1 :v-2, :deps-project-3 :v-2}
          :dep-3 {:deps-project-1 :v-3}
          :dep-4 {:deps-project-3 :v-5}}
         (d/merge-deps
          [[:dep-1 {:deps-project-1 :v-1}]
           [:dep-2 {:deps-project-1 :v-2}]
           [:dep-3 {:deps-project-1 :v-3}]
           [:dep-1 {:deps-project-2 :v-1}]
           [:dep-1 {:deps-project-3 :v-1}]
           [:dep-2 {:deps-project-3 :v-2}]
           [:dep-4 {:deps-project-3 :v-5}]]))))

(deftest ^:unit merge-deps
  (let [m {:dep-1 :v-1
           :dep-2 :v-2
           :dep-3 :v-3
           :dep-4 :v-4
           :dep-5 :v-5}]
    (is (= [{:deps-project-1 :v-1
             :deps-project-2 :v-1
             :deps-project-3 :v-1
             :last-version   :v-1
             :name           :dep-1}
            {:deps-project-1 :v-2
             :deps-project-3 :v-2
             :last-version   :v-2
             :name           :dep-2}
            {:deps-project-1 :v-3
             :last-version   :v-3
             :name           :dep-3}
            {:deps-project-3 :v-4
             :last-version   :v-4
             :name           :dep-4}
            {:deps-project-1 "==> :v-6"
             :deps-project-3 "==> :v-2"
             :last-version   :v-5
             :name           :dep-5}]
           (->> {:dep-1 {:deps-project-1 :v-1, :deps-project-2 :v-1, :deps-project-3 :v-1}
                 :dep-2 {:deps-project-1 :v-2, :deps-project-3 :v-2}
                 :dep-3 {:deps-project-1 :v-3}
                 :dep-4 {:deps-project-3 :v-4}
                 :dep-5 {:deps-project-1 :v-6, :deps-project-3 :v-2}}
                (d/pretty-print-structure (fn [x] m))
                (sort-by :name))))))

(deftest ^:unit has-newer-version?
  (let [last-version :v-3
        m {:deps-project-1 :v-1, :deps-project-2 :v-1, :deps-project-3 :v-1}]
    (is (true? (d/has-newer-version? m last-version))))

  (let [last-version :v-1
        m {:deps-project-1 :v-1, :deps-project-2 :v-1, :deps-project-3 :v-1}]
    (is (false? (d/has-newer-version? m last-version))))

  (let [last-version :unknown
        m {:deps-project-1 :v-2, :deps-project-2 :v-1, :deps-project-3 :v-1}]
    (is (true? (d/has-newer-version? m last-version))))

  (let [last-version :unknown
        m {:deps-project-1 :v-1, :deps-project-2 :v-1, :deps-project-3 :v-1}]
    (is (false? (d/has-newer-version? m last-version)))))

(deftest ^:unit take-repo-url
  (is (= "url-2"
         (d/take-repo-url {:url "url-2"})))
  (is (= "url-2"
         (d/take-repo-url "url-2"))))

(deftest ^:unit repositories-of
  (is (= {"repo-1" "url-1"
          "repo-2" "url-2"
          "repo-3" "url-3"}
         (d/repositories-of {:project-1  {:repositories [["repo-1" "url-1"]]}
                             :project-2 {:repositories [["repo-2" {:url "url-2"}]
                                                        ["repo-3" {:url "url-3"}]]}}))))