(ns leiningen.leinsync.deps
  (:require [leiningen.leinsync.table-pretty-print :as pp]
            [ancient-clj.core :as ancient]))

(defn last-version-of [artifact]
  (if-let [last-version (ancient/latest-version-string! artifact)]
    last-version ""))

(defn check-version-for-update [artifact version]
  (let [last-version (last-version-of artifact)]
    (if (or (empty? last-version) (= version last-version))
      version
      (str version " ==> " last-version))))

(defn flat-deps-list [p deps-list enrich-version-fn]
  (into {}
        (map
         (fn [[dep version]]
           {(keyword dep) {p (enrich-version-fn dep version)}})
         deps-list)))

(defn deps->project [projects-desc enrich-version-fn]
  (reduce-kv
   (fn [m k {deps :dependencies}]
     (into m (flat-deps-list k deps enrich-version-fn)))
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
  (-> (deps->project projects-desc check-version-for-update)
      (merge-deps)
      (pretty-print-structure)
      (pp/print-compact-table)))