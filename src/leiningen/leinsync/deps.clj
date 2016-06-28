(ns leiningen.leinsync.deps
  (:require [leiningen.leinsync.table-pretty-print :as pp]))

(defn flat-deps-list [p deps-list]
  (into {}
        (map
         (fn [[dep version]] {(keyword dep) {p version}})
         deps-list)))

(defn deps->project [projects-desc]
  (reduce-kv
   (fn [m k {deps :dependencies}]
     (into m (flat-deps-list k deps)))
   []
   projects-desc))

(defn merge-deps [deps]
  (reduce
   (fn [x1 [dep data]]
     (if (contains? x1 dep)
       (assoc x1 dep (merge data (get x1 dep)))
       (assoc x1 dep data)))
   {}
   deps))

(defn pretty-print-structure [deps]
  (->> deps
       (seq)
       (map (fn [[k v]] (merge {:name k} v)))))

(defn check-deps [projects-desc]
  (->> projects-desc
       (deps->project)
       (merge-deps)
       (pretty-print-structure)
       (pp/print-compact-table)))

