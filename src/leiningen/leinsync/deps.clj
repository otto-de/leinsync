(ns leiningen.leinsync.deps
  (:require [leiningen.leinsync.table-pretty-print :as pp]
            [ancient-clj.core :as ancient]
            [leiningen.core.main :as m]))

(def different-marker "==> ")

(defn last-version-of [artifact]
  (if-let [last-version (ancient/latest-version-string! artifact)]
    last-version :unknown))

(defn flat-deps-list [p deps-list]
  (into {}
        (map
         (fn [[dep version]]
           {(keyword dep) {p version}})
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

(defn has-newer-version? [m last-version]
  (->> m
       (vals)
       (concat [last-version])
       (distinct)
       (filter #(not= :unknown %))
       (count)
       (< 1)))

(defn mark-for-possible-update [get-version marker]
  (fn [[k v]]
    (let [last-version (get-version k)
          deps-info {:name k :last-version last-version}]
      (if (has-newer-version? v last-version)
        (merge deps-info (zipmap (keys v) (map #(str marker %) (vals v))))
        (merge deps-info v)))))

(defn pretty-print-structure [enrich-version deps]
  (->> deps
       (seq)
       (map (mark-for-possible-update enrich-version different-marker))))

(defn log-resouces-table [m]
  (m/info "\n* List of dependencies")
  (m/info "      ==> version :  means that the dependency on this project is out-of-date")
  (pp/print-compact-table m)
  (m/info "\n"))

(defn check-deps [projects-desc]
  (->> projects-desc
       (deps->project)
       (merge-deps)
       (pretty-print-structure last-version-of)
       (log-resouces-table)))