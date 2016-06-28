(ns leiningen.leinsync.deps
  (:require [leiningen.leinsync.table-pretty-print :as pp]))

(defn flat-deps-list [p deps-list]
  (->> deps-list
       (map
        (fn [[dep version]] {:name dep p version}))))

(defn extract-deps-list [projects-desc]
  (reduce-kv
   (fn [m k {deps :dependencies}]
     (into m (flat-deps-list k deps)))
   []
   projects-desc))

(defn check-deps [projects-desc]
  ;(println (->> projects-desc (extract-deps-list)))
  (->> projects-desc
       (extract-deps-list)
       (sort-by :name)
       (pp/print-compact-table)))

