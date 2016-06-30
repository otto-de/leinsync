(ns leiningen.leinsync.deps
  (:require [leiningen.leinsync.table-pretty-print :as pp]
            [ancient-clj.core :as ancient]
            [leiningen.core.main :as m]))

(def different-marker "==> ")

(defn repositories-opt [repos]
  (if (empty? repos)
    {:repositories ancient/default-repositories}
    {:repositories repos}))

(defn last-version-of [repos artifact]
  (try
    (if-let [last-version (ancient/latest-version-string!
                           artifact
                           (repositories-opt repos))]
      last-version :unknown)
    (catch Exception _ :unknown)))

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

(defn mark-for-possible-update [last-version-map marker]
  (fn [[k v]]
    (let [last-version (get last-version-map k)
          deps-info {:name k :last-version last-version}]
      (if (has-newer-version? v last-version)
        (merge deps-info (zipmap (keys v) (map #(str marker %) (vals v))))
        (merge deps-info v)))))

(defn parallel-get-version [repos deps]
  (let [tasks (map #(future {% (last-version-of repos %)}) deps)]
    (reduce merge (doall (pmap deref tasks)))))

(defn pretty-print-structure [enrich-version-fn deps]
  (let [last-version-map (enrich-version-fn (keys deps))]
    (->> deps
         (seq)
         (map (mark-for-possible-update last-version-map different-marker)))))

(defn log-resouces-table [m]
  (m/info "\n* List of dependencies")
  (m/info "      ==> version :  means that the dependency on this project is out-of-date")
  (pp/print-compact-table m)
  (m/info "\n"))

(defn take-repo-url [r]
  (if (map? r) (:url r) r))

(defn repositories-of [projects-desc]
  (->> projects-desc
       (vals)
       (map :repositories)
       (reduce concat)
       (reduce concat)
       (map take-repo-url)
       (apply hash-map)))

(defn check-deps [projects-desc]
  (let [enrich-version-fn (partial parallel-get-version (repositories-of projects-desc))]
    (->> projects-desc
         (deps->project)
         (merge-deps)
         (pretty-print-structure enrich-version-fn)
         (log-resouces-table))))