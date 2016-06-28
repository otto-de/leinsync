(ns leiningen.leinsync.deps
  (:require [leiningen.leinsync.table-pretty-print :as pp]
            [ancient-clj.core :as ancient]
            [leiningen.core.main :as m]))

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

(defn pretty-print-structure [enrich-version deps]
  (->> deps
       (seq)
       (map (fn [[k v]]
              (let [last-version (enrich-version k)
                    has-newer-version? (-> v
                                           (vals)
                                           (conj last-version)
                                           (distinct)
                                           (count)
                                           (> 1))]
                (if has-newer-version?
                  (merge {:name k :last-version last-version}
                         (zipmap (keys v)
                                 (map #(str "==> " %) (vals v))))
                  (merge {:name k :last-version last-version} v)))))))

(defn log-resouces-table [m]
  (m/info "\n* List of dependencies")
  (m/info "     - ==> version :  means that the dependency on this project is out-of-date")
  (pp/print-compact-table m)
  (m/info "\n"))

(defn check-deps [projects-desc]
  (->> projects-desc
       (deps->project)
       (merge-deps)
       (pretty-print-structure last-version-of)
       (log-resouces-table)))