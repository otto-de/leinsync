(ns leiningen.leinsync.deps-test
  (:require [clojure.test :refer :all]
            [leiningen.leinsync.deps :as d]))

(deftest ^:unit flat-deps-list
  (is (= {:dep-1 {:k :v-1}
          :dep-2 {:k :v-2}}
         (d/flat-deps-list :k [[:dep-1 :v-1]
                               [:dep-2 :v-2]]))))

(deftest ^:unit deps->project
  (is (= [[:dep-1 {:deps-project-1 :v-1}]
          [:dep-2 {:deps-project-1 :v-2}]
          [:dep-3 {:deps-project-1 :v-3}]
          [:dep-1 {:deps-project-2 :v-1}]
          [:dep-1 {:deps-project-3 :v-1}]
          [:dep-2 {:deps-project-3 :v-2}]
          [:dep-4 {:deps-project-3 :v-5}]]
         (d/deps->project
          {:deps-project-1 {:dependencies [[:dep-1 :v-1]
                                           [:dep-2 :v-2]
                                           [:dep-3 :v-3]]}
           :deps-project-2 {:dependencies [[:dep-1 :v-1]]}
           :deps-project-3 {:dependencies [[:dep-1 :v-1]
                                           [:dep-2 :v-2]
                                           [:dep-4 :v-5]]}}))))

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
  (is (= [{:deps-project-1 :v-1
           :deps-project-2 :v-1
           :deps-project-3 :v-1
           :name           :dep-1}
          {:deps-project-1 :v-2, :deps-project-3 :v-2, :name :dep-2}
          {:deps-project-1 :v-3, :name :dep-3}
          {:deps-project-3 :v-5, :name :dep-4}]
         (d/pretty-print-structure
          {:dep-1 {:deps-project-1 :v-1, :deps-project-2 :v-1, :deps-project-3 :v-1}
           :dep-2 {:deps-project-1 :v-2, :deps-project-3 :v-2}
           :dep-3 {:deps-project-1 :v-3}
           :dep-4 {:deps-project-3 :v-5}}))))