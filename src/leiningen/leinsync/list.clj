(ns leiningen.leinsync.list
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [leiningen.leinsync.namespaces :as ns]
            [leiningen.leinsync.utils :as u]
            [digest :as d]
            [leiningen.core.main :as m]
            [leiningen.leinsync.table-pretty-print :as pp]))

(def hash-length 6)
(def all-resources-different-marker "==> ")
(def one-resource-different-marker "=[x]=> ")
(def empty-occurence-str "X ")

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

(defn md5-hash [paths]
  (str/join " | " (map
                   #(d/digest "md5" (io/as-file %))
                   paths)))

(defn resource-render [paths project]
  (if (empty? paths)
    {project empty-occurence-str}
    {project (md5-hash paths)}))

(defn resource-occurence [resource project project-desc render]
  (let [paths (concat (ns/resource->target-path resource (name project) project-desc)
                      (ns/namespace->target-path resource (name project) project-desc))
        existing-paths (filter u/exists? paths)]
    (render existing-paths project)))

(defn resource->project [project project-desc render]
  (fn [resource]
    [(keyword resource)
     (resource-occurence resource project project-desc render)]))

(defn resource-name->project [projects selector render]
  (reduce-kv
   (fn [m project desc]
     (into m (map
              (resource->project project desc render)
              (get-in desc selector))))
   []
   projects))

(defn resource->package-and-name [k selector]
  (if (= selector ns/namespace-def)
    (let [name-segments (str/split (name k) #"\.")]
      {:package (str/join "." (drop-last name-segments))
       :name    (last name-segments)})
    {:name (name k)}))

(defn mark-value-with
  ([marker v]
   (if (empty? v)
     ""
     {:marker marker :value (str/upper-case v)}))
  ([assertion-marker standard-marker assertion v]
   (if (assertion v)
     (mark-value-with assertion-marker v)
     (mark-value-with standard-marker v))))

(defn mark-as-diffrent
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

(defn mark-2-different-values [m [first second] marker-fn]
  (let [[first-freq second-freq] (->> m
                                      (vals)
                                      (remove empty?)
                                      (frequencies)
                                      (vals))]
    (cond
      (and (not= first-freq 1) (not= second-freq 1)) (marker-fn m)
      (= first-freq second-freq) (marker-fn m #(or (= % first) (= % second)))
      (> first-freq second-freq) (marker-fn m #(= % second))
      (< first-freq second-freq) (marker-fn m #(= % first))
      :else m)))

(defn unterline-different-values [m]
  (let [unique-v (->> m
                      (vals)
                      (filter not-empty)
                      (distinct))]
    (cond
      (= 1 (count unique-v)) m
      (= 2 (count unique-v)) (mark-2-different-values m unique-v mark-as-diffrent)
      :else (mark-as-diffrent m))))

(defn sub-hash-str [hash-str desired-length]
  (subs hash-str 0 (min desired-length (count hash-str))))

(defn display-hash-value [v desired-length]
  (zipmap (keys v)
          (map (fn [x]
                 (if (map? x)
                   (str (:marker x) (sub-hash-str (:value x) desired-length))
                   (sub-hash-str x desired-length)))
               (vals v))))

(defn pretty-print-structure [data selector desired-length]
  (reduce-kv
   (fn [m k v] (conj m (merge
                        (resource->package-and-name k selector)
                        (-> v
                            (unterline-different-values)
                            (display-hash-value desired-length)))))
   []
   data))

(defn build-resource-table [projects selector render]
  (-> projects
      (resource-name->project selector render)
      (merge-project-occurence)
      (pretty-print-structure selector hash-length)))

(defn log-resouces-table [m resource-name]
  (m/info "\n* List of" resource-name)
  (m/info "     -" empty-occurence-str
          "                        :  the namespace/resource does not exist in the project although it has been specified")
  (m/info "     - hash-value (.i.e ddfa3d66) :  the namespace/resource is defined in the project.clj")
  (m/info "                                     ==>    hash : means that the resource doesn't match on all projects")
  (m/info "                                     =[x]=> hash : means that the resource on this project is different from others")
  (pp/print-compact-table (sort-by :name m)))

(defn list-resources [projects-desc selector]
  (-> projects-desc
      (build-resource-table selector resource-render)
      (log-resouces-table (name (last selector)))))