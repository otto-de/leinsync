(ns leiningen.leinsync.list
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [digest :as d]
            [leiningen.leinsync.namespaces :as ns]
            [leiningen.leinsync.utils :as u]
            [leiningen.core.main :as m]
            [leiningen.leinsync.table-pretty-print :as pp]
            [leiningen.leinsync.git :as git]))

(def hash-length 5)
(def all-resources-different-marker "=> ")
(def one-resource-different-marker "[x]=> ")
(def empty-occurence-str "X ")

(defn aggregate [result [namespace project]]
  (assoc result namespace (if (contains? result namespace)
                            (merge-with str project (get result namespace))
                            project)))

(defn merge-project-occurence
  ([data] (merge-project-occurence data {}))
  ([[first & rest] result]
   (let [aggregated-result (aggregate result first)]
     (if (empty? rest)
       aggregated-result
       (recur rest aggregated-result)))))

(defn md5-hash [paths]
  (->> paths
       (map #(d/digest "md5" (io/as-file %)))
       (str/join " | ")))

(defn resource-render [paths project]
  {project (if (empty? paths)
             empty-occurence-str
             {:md5       (md5-hash paths)
              :timestamp (git/last-commit-date (first paths))})})

(defn resource-occurence [resource project project-desc render]
  (let [all-paths (concat (ns/resource->target-path resource (name project) project-desc)
                          (ns/namespace->target-path resource (name project) project-desc))]
    (render (filter u/exists? all-paths) project)))

(defn resource->project [project project-desc render]
  (fn [resource]
    [(keyword resource)
     (resource-occurence resource project project-desc render)]))

(defn resource-name->project [projects selector render]
  (reduce-kv
   (fn [m project desc]
     (->> (get-in desc selector)
          (map (resource->project project desc render))
          (into m)))
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
     {:marker marker :value v}))
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

(defn unterline-different-values [m]
  (let [unique-v (->> m
                      (vals)
                      (map :md5)
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
                 (if (contains? x :marker)
                   (str
                    (:marker x)
                    (get-in x [:value :timestamp]) " "
                    (sub-hash-str (get-in x [:value :md5]) desired-length))
                   (sub-hash-str (:md5 x) desired-length)))
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

(defn has-no-difference? [m]
  (->>
   (dissoc m :package :name)
   (vals)
   (map #(or (u/includes? % all-resources-different-marker)
             (u/includes? % one-resource-different-marker)))
   (reduce #(or %1 %2))))

(defn reduce-list-with-option [coll option]
  (if (= :diff option)
    (filter has-no-difference? coll)
    coll))

(defn build-resource-table [projects selector render option]
  (-> projects
      (resource-name->project selector render)
      (merge-project-occurence)
      (pretty-print-structure selector hash-length)
      (reduce-list-with-option option)))

(defn log-resouces-table [coll resource-name]
  (when (seq coll)
    (m/info "\n* List of" resource-name)
    (m/info "     -" empty-occurence-str
            "                        :  the namespace/resource does not exist in the project although it has been specified")
    (m/info "     - hash-value (.i.e ddfa3d66) :  the namespace/resource is defined in the project.clj")
    (m/info "                                     =>    last-commit-date hash : means that the resource doesn't match on all projects")
    (m/info "                                     [x]=> last-commit-date hash : means that the resource on this project is different from others")
    (pp/print-compact-table (sort-by :name coll))))

(defn list-resources [projects-desc selector option]
  (-> projects-desc
      (build-resource-table selector resource-render option)
      (log-resouces-table (name (last selector)))))