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
                (d/pretty-print-structure (fn [x] (get m x)))
                (sort-by :name))))))

(deftest ^:unit last-version-of
  (is (= :unknown (d/last-version-of "not-valid"))))