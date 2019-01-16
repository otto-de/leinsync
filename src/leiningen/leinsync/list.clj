(ns leiningen.leinsync.list
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [digest :as d]
            [leiningen.core.main :as m]
            [leiningen.leinsync.namespaces :as ns]
            [leiningen.leinsync.utils :as u]
            [leiningen.leinsync.table-pretty-print :as pp]
            [leiningen.leinsync.git :as git]
            [leiningen.leinsync.packages :as p]))

(def hash-length 5)
(def all-resources-different-marker "=> ")
(def one-resource-different-marker "[x]=> ")
(def empty-occurrence-str "X ")

(defn aggregate [result [namespace project]]
  (assoc result namespace (if (contains? result namespace)
                            (merge project (get result namespace))
                            project)))

(defn merge-project-occurrence
  ([data] (if (seq data) (merge-project-occurrence data {})))
  ([[first-result & rest-result] result]
   (let [aggregated-result (aggregate result first-result)]
     (if (empty? rest-result)
       aggregated-result
       (recur rest-result aggregated-result)))))

(defn md5-hash [paths]
  (->> paths
       (map #(d/digest "md5" (io/as-file %)))
       (str/join " | ")))

(defn resource-render [paths project]
  {project (if (empty? paths)
             empty-occurrence-str
             {:md5       (md5-hash paths)
              :timestamp (git/last-commit-date (first paths))})})

(defn resource-occurrence [resource project project-desc render]
  (let [all-paths (concat (ns/resource->target-path resource (name project) project-desc)
                          (ns/namespace->target-path resource (name project) project-desc))]
    (render (filter u/exists? all-paths) project)))

(defn resource->project [project project-desc render]
  (fn [resource]
    [(keyword resource)
     (resource-occurrence resource project project-desc render)]))

(defn get-resource-list [desc selector]
  (let [resource-def (get-in desc selector)]
    (if (= selector p/package-def)
      (p/get-package-resource-list desc)
      resource-def)))

(defn resource-name->project [projects selector render]
  (reduce-kv
   (fn [m project desc]
     (let [resource-tuple (map (resource->project project desc render) (get-resource-list desc selector))]
       (if (not-empty resource-tuple)
         (into m resource-tuple)
         m)))
   []
   projects))

(defn resource->package-and-name [k selector]
  (if (contains? #{ns/namespace-def p/package-def} selector)
    (let [name-segments (str/split (name k) #"\.")]
      {:package (str/join "." (drop-last name-segments))
       :name    (last name-segments)})
    {:name (name k)}))

(defn mark-value-with
  ([marker v]
   (if (empty? v)
     ""
     {:marker marker :value v}))
  ([assertion-marker standard-marker assertion v]
   (if (assertion v)
     (mark-value-with assertion-marker v)
     (mark-value-with standard-marker v))))

(defn mark-as-different
  ([m]
   (zipmap (keys m)
           (map (partial mark-value-with
                         all-resources-different-marker)
                (vals m))))
  ([m assertion]
   (zipmap (keys m)
           (map (partial mark-value-with
                         one-resource-different-marker
                         all-resources-different-marker
                         assertion)
                (vals m)))))

(defn mark-2-different-values [m [first-v second-v] marker-fn]
  (let [not-empty-values (remove empty? (map :md5 (vals m)))
        first-freq (count (filter #(= first-v %) not-empty-values))
        second-freq (count (filter #(= second-v %) not-empty-values))]
    (cond
      (and (not= first-freq 1) (not= second-freq 1)) (marker-fn m)
      (= first-freq second-freq) (marker-fn m #(or (= (:md5 %) first-v) (= (:md5 %) second-v)))
      (> first-freq second-freq) (marker-fn m #(= (:md5 %) second-v))
      (< first-freq second-freq) (marker-fn m #(= (:md5 %) first-v))
      :else m)))

(defn underline-different-values [m]
  (let [unique-v (->> m
                      (vals)
                      (map :md5)
                      (filter not-empty)
                      (distinct))]
    (cond
      (= 1 (count unique-v)) m
      (= 2 (count unique-v)) (mark-2-different-values m unique-v mark-as-different)
      :else (mark-as-different m))))

(defn sub-hash-str [hash-str desired-length]
  (subs hash-str 0 (min desired-length (count hash-str))))

(defn compact-hash-str [desired-length m]
  (if (map? m)
    (if (contains? m :marker)
      (str
       (:marker m)
       (get-in m [:value :timestamp]) " "
       (sub-hash-str (get-in m [:value :md5] "?????") desired-length))
      (sub-hash-str (:md5 m) desired-length))
    m))

(defn display-hash-value [v desired-length]
  (zipmap (keys v) (map (partial compact-hash-str desired-length) (vals v))))

(defn pretty-print-structure [data selector desired-length]
  (if (not-empty data)
    (reduce-kv
     (fn [m k v]
       (conj m (merge
                (resource->package-and-name k selector)
                (-> v
                    (underline-different-values)
                    (display-hash-value desired-length)))))
     []
     data)))

(defn has-no-difference? [m]
  (->> (dissoc m :package :name)
       (vals)
       (map #(or (str/includes? % all-resources-different-marker)
                 (str/includes? % one-resource-different-marker)))
       (reduce #(or %1 %2))))

(defn reduce-list-with-option [coll option]
  (if (= :diff option)
    (filter has-no-difference? coll)
    coll))

(defn build-resource-table [projects selector render option]
  (-> projects
      (resource-name->project selector render)
      (merge-project-occurrence)
      (pretty-print-structure selector hash-length)
      (reduce-list-with-option option)))

(defn log-resources-table [coll resource-name]
  (when (seq coll)
    (m/info "\n* List of" resource-name)
    (m/info "     -" empty-occurrence-str
            "                        :  the package/namespace/resource does not exist in the project although it has been specified")
    (m/info "     - hash-value (.i.e ddfa3d66) :  the package/namespace/resource is defined in the project.clj")
    (m/info "                                     =>    last-commit-date hash : means that the resource doesn't match on all projects")
    (m/info "                                     [x]=> last-commit-date hash : means that the resource on this project is different from others")
    (pp/print-compact-table (sort-by (juxt :package :name) coll))))

(defn list-resources [projects-desc selector option]
  (-> projects-desc
      (build-resource-table selector resource-render option)
      (log-resources-table (name (last selector)))))