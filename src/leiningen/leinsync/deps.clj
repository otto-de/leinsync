(ns leiningen.leinsync.deps
  (:require [ancient-clj.core :as ancient]
            [leiningen.core.main :as m]
            [leiningen.leinsync.table-pretty-print :as pp]))

(def different-marker "=> ")
(def ancient-latest-version-fn ancient/latest-version-string!)
(def default-repositories ancient/default-repositories)

(defn repositories-opt [repos]
  (if (empty? repos)
    {:repositories ancient/default-repositories
     :qualified?   false}
    {:repositories repos
     :qualified?   false}))

(defn last-version-of [version-fn repos artifact]
  (try
    (if-let [last-version (version-fn artifact (repositories-opt repos))]
      last-version :unknown)
    (catch Exception _ :unknown)))

(defn flat-deps-list [p deps-list]
  (into {} (map (fn [[dep version]] {(keyword dep) {p version}}) deps-list)))

(defn deps->project [selector projects-desc]
  (reduce-kv
   (fn [m k v]
     (->> selector
          (get-in v)
          (flat-deps-list k)
          (into m)))
   []
   projects-desc))

(defn merge-deps [deps]
  (reduce
   (fn [x [dep data]]
     (assoc x dep (if (contains? x dep)
                    (merge data (get x dep))
                    data)))
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
    (let [last-version (get last-version-map k)]
      (merge {:name         k
              :last-version last-version}
             (if (has-newer-version? v last-version)
               (->> v
                    (vals)
                    (map #(str marker %))
                    (zipmap (keys v)))
               v)))))

(defn parallel-get-version [latest-version-fn repos deps]
  (->> deps
       (map #(future {% (last-version-of latest-version-fn repos %)}))
       (pmap deref)
       (doall)
       (reduce merge)))

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

(defn log-resources-table [selector m]
  (if (seq m)
    (do
      (m/info "* List of dependencies of" selector)
      (m/info "  " different-marker "version :  means that the dependency on this project is out-of-date")
      (pp/print-compact-table m))
    (m/info "* No dependency has been found for" selector)))

(defn check-dependencies-of [projects-desc selector]
  (let [enrich-version-fn (partial parallel-get-version ancient-latest-version-fn (repositories-of projects-desc))]
    (->> projects-desc
         (deps->project selector)
         (merge-deps)
         (pretty-print-structure enrich-version-fn)
         (log-resources-table selector))))