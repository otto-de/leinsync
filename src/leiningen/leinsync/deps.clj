(ns leiningen.leinsync.deps
  (:require [leiningen.leinsync.table-pretty-print :as pp]
            [ancient-clj.core :as ancient]
            [leiningen.core.main :as m]))

(def different-marker "=> ")
(def ancient-latest-version-fn ancient/latest-version-string!)
(def default-repositories ancient/default-repositories)

(defn repositories-opt [repos]
  (if (empty? repos)
    {:repositories ancient/default-repositories
     :qualified? false}
    {:repositories repos
     :qualified? false}))

(defn last-version-of [version-fn repos artifact]
  (try
    (if-let [last-version (version-fn artifact (repositories-opt repos))]
      last-version :unknown)
    (catch Exception _ :unknown)))

(defn flat-deps-list [p deps-list]
  (into {}
        (map
         (fn [[dep version]]
           {(keyword dep) {p version}})
         deps-list)))

(defn deps->project [selector projects-desc]
  (reduce-kv
   (fn [m k v]
     (into m (flat-deps-list k (get-in v selector))))
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

(defn parallel-get-version [latest-version-fn repos deps]
  (let [tasks (map #(future {% (last-version-of latest-version-fn repos %)}) deps)]
    (reduce merge (doall (pmap deref tasks)))))

(defn pretty-print-structure [enrich-version-fn deps]
  (let [last-version-map (enrich-version-fn (keys deps))]
    (->> deps
         (seq)
         (map (mark-for-possible-update last-version-map different-marker)))))

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

(defn log-resouces-table [selector m]
  (m/info "* List of dependencies of" selector)
  (m/info "  " different-marker "version :  means that the dependency on this project is out-of-date")
  (pp/print-compact-table m))

(defn check-dependencies-of [projects-desc selector]
  (let [enrich-version-fn (partial parallel-get-version ancient-latest-version-fn (repositories-of projects-desc))]
    (->> projects-desc
         (deps->project selector)
         (merge-deps)
         (pretty-print-structure enrich-version-fn)
         (log-resouces-table selector))))