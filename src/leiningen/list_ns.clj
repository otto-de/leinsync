(ns leiningen.list-ns
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [leiningen.namespaces :as ns]
            [leiningen.utils :as u]
            [digest :as d]
            [leiningen.core.main :as m]
            [clojure.pprint :as pp]))

(def hash-length 8)
(def all-resources-different-marker "==> ")
(def one-resource-different-marker "=[x]=> ")
(def empty-occurence-str "X ")

(defn log-resouces-table [m resource-name]
  (m/info "\n* List of" resource-name)
  (m/info "     -" empty-occurence-str
          "                        :  the namespace/resource does not exist in the project although it has been specified")
  (m/info "     - hash-value (.i.e ddfa3d66) :  the namespace/resource is defined in the project.clj")
  (m/info "                                     ==>    hash : means that the resource doesn't match on all projects")
  (m/info "                                     =[x]=> hash : means that the resource on this project is different from others")
  (pp/print-table (sort-by :name m))
  (m/info "\n"))

(defn aggregate [result [namespace project]]
  (let [project-occurence (if (contains? result namespace)
                            (merge-with str project (get result namespace))
                            project)]
    (assoc result namespace project-occurence)))

(defn merge-project-occurence
  ([data] (merge-project-occurence data {}))
  ([[first & rest] result]
   (let [aggregated-result (aggregate result first)]
     (if (empty? rest)
       aggregated-result
       (recur rest aggregated-result)))))

(defn empty-project-occurence [projects initial-value]
  (zipmap
   (keys projects)
   (take (count projects) (repeat initial-value))))

(defn md5-hash
  ([paths] (str/join " | " (map #(md5-hash % hash-length) paths)))
  ([path length]
   (let [hash-value (d/digest "md5" (io/as-file path))
         hash-length (dec (count hash-value))]
     (subs hash-value (max 0 (- hash-length length)) hash-length))))

(defn project-occurence-render [paths project]
  (if (empty? paths)
    {project empty-occurence-str}
    {project (md5-hash paths)}))

(defn resource-occurence [resource project project-desc render]
  (let [paths (concat (ns/resource->target-path resource (name project) project-desc)
                      (ns/namespace->target-path resource (name project) project-desc))
        existing-paths (filter u/exists? paths)]
    (render existing-paths project)))

(defn resource->project [project project-desc empty-project-occurence render]
  (fn [resource]
    [(keyword resource) (merge-with
                         str
                         (resource-occurence resource project project-desc render)
                         empty-project-occurence)]))

(defn resource-name->project [projects selector render]
  (let [empty-occurence (empty-project-occurence projects "")]
    (reduce-kv
     (fn [m project desc]
       (into m (map
                (resource->project project desc empty-occurence render)
                (get-in desc selector))))
     []
     projects)))

(defn occurence-map-for [k selector]
  (if (= selector ns/namespace-def)
    (let [name-segments (str/split (name k) #"\.")]
      {:package (str/join "." (drop-last name-segments))
       :name    (last name-segments)})
    {:name (name k)}))

(defn mark-value-with
  ([marker v] (if (empty? v) "" (str marker (str/upper-case v))))
  ([assertion-marker standard-marker assertion v]
   (if (assertion v)
     (mark-value-with assertion-marker v)
     (mark-value-with standard-marker v))))

(defn mark-as-diffrent
  ([m] (zipmap (keys m)
               (map (partial mark-value-with all-resources-different-marker)
                    (vals m))))
  ([m first-val second-val]
   (let [not-empty-values (remove empty? (vals m))
         first-val-occurence (count (remove #(= first-val %) not-empty-values))
         second-val-occurence (count (remove #(= second-val %) not-empty-values))]
     (if (or (= first-val-occurence 1) (= second-val-occurence 1))
       (cond
         (< first-val-occurence second-val-occurence)
         (zipmap (keys m)
                 (map (partial mark-value-with
                               one-resource-different-marker
                               all-resources-different-marker
                               #(= % second-val))
                      (vals m)))
         (> first-val-occurence second-val-occurence)
         (zipmap (keys m)
                 (map (partial mark-value-with
                               one-resource-different-marker
                               all-resources-different-marker
                               #(= % first-val))
                      (vals m)))
         :else m)
       (mark-as-diffrent m)))))

(defn unterline-different-values [m]
  (let [unique-values (->> m
                           (vals)
                           (filter not-empty)
                           (set))]
    (cond
      (= 1 (count unique-values)) m
      (= 2 (count unique-values))
      (mark-as-diffrent m (first unique-values) (second unique-values))
      :else (mark-as-diffrent m))))

(defn pretty-print-structure [data selector]
  (reduce-kv
   (fn [m k v] (conj m (merge
                        (occurence-map-for k selector)
                        (unterline-different-values v))))
   []
   data))

(defn build-resource-table [projects selector render]
  (-> projects
      (resource-name->project selector render)
      (merge-project-occurence)
      (pretty-print-structure selector)))

(defn list-resources [projects-desc selector]
  (-> projects-desc
      (build-resource-table selector project-occurence-render)
      (log-resouces-table (name (last selector)))))