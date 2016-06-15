(ns leiningen.namespaces
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [leiningen.utils :as u]
            [leiningen.project-reader :as pr]
            [leiningen.core.main :as m]))

(def namespace-def [:ns-sync :namespaces])
(def resource-def [:ns-sync :resources])
(def src-path-def [:source-paths])
(def test-path-def [:test-paths])
(def resource-path-def [:resource-paths])

(defn sync-get-in [project-clj test-path-def default]
  (-> (get-in project-clj test-path-def default)
      (distinct)))

(defn test-or-source-namespace [namespace project-clj]
  (if (or (.endsWith namespace "-test")
          (.contains namespace "test"))
    (sync-get-in project-clj test-path-def ["test"])
    (sync-get-in project-clj src-path-def ["src"])))

(defn namespace->path [path project-desc]
  {:src-or-test    (test-or-source-namespace path project-desc)
   :namespace-path (-> path
                       (str/replace #"-" "_")
                       (str/replace #"\." "/")
                       (str ".clj"))})

(defn path->namespace [path project-desc]
  (let [source-folders (set (concat (get-in project-desc src-path-def)
                                    (get-in project-desc test-path-def)))
        resources-folders (set (get-in project-desc resource-path-def))
        namespaces-list (set (get-in project-desc namespace-def))
        resources-list (set (get-in project-desc resource-def))
        [folder & path-segments] (str/split (str/replace path #"\.clj" "") #"/")]
    (cond
      (and (contains? source-folders folder)
           (contains? namespaces-list (str/join "." path-segments)))
      {:resource-path folder
       :resource-name (str/join "." path-segments)}
      (and (contains? resources-folders folder)
           (contains? resources-list (str/join "/" path-segments)))
      {:resource-path folder
       :resource-name (str/join "/" path-segments)}

      :else {:resource-path :not-found :resource-name :not-found})))

(defn resource->target-path [resource target-project target-project-desc]
  (let [resource-folders (get-in target-project-desc resource-path-def)]
    (map
     #(str (pr/->target-project-path target-project) "/" % "/" resource)
     resource-folders)))

(defn resource->source-path [resource source-project-desc]
  (map
   #(str % "/" resource)
   (get-in source-project-desc resource-path-def)))

(defn namespace->target-path [namespace target-project target-project-desc]
  (let [{folders :src-or-test
         ns-path :namespace-path} (namespace->path namespace target-project-desc)]
    (map
     #(str (pr/->target-project-path target-project) "/" % "/" ns-path)
     folders)))

(defn namespace->source-path [namespace source-project-desc]
  (let [{folders :src-or-test ns-path :namespace-path} (namespace->path namespace source-project-desc)]
    (map #(str % "/" ns-path) folders)))

(defn update-files! [from-file to-file]
  (io/make-parents (io/file to-file))
  (spit
   (io/file to-file)
   (slurp (io/file from-file))))

(defn should-update? [entry-definition entry target-project-desc]
  (-> target-project-desc
      (get-in entry-definition)
      (set)
      (contains? entry)))

(defn initial-question [namespace project]
  (str "* ==> The location of " namespace " on " (str/upper-case project)
       " could not be determined.\n"
       "      Please choose one of options (a number):"
       (str "\n         + -1 -> to skip updating " namespace)))

(defn log-warning [name target-project]
  (m/info "* WARNING: Could not update" name "on project" target-project
          "\n    ==>" name "may not exist on the source project"))

(defn localtion-question-with
  ([ns project [first & rest]]
   (localtion-question-with (initial-question ns project) 0 first rest))
  ([question index first [ffirst rrest]]
   (if (nil? first)
     question
     (recur (str question "\n         +  " index " -> " first)
            (inc index) ffirst rrest))))

(defn ask-for-localtion
  ([namespace paths] (ask-for-localtion namespace "source project" paths))
  ([namespace project paths]
   (-> namespace
       (localtion-question-with project paths)
       (u/ask-user (partial u/is-number (count paths)))
       (read-string))))

(defn ask-for-source-and-target [name target-project existing-source-paths target-paths]
  (let [source-location (if (= 1 (count existing-source-paths))
                          0 (ask-for-localtion name existing-source-paths))
        target-location (if (= 1 (count target-paths))
                          0 (ask-for-localtion name target-project target-paths))]
    (if (and (>= source-location 0) (>= target-location 0))
      {:source (nth existing-source-paths source-location)
       :target (nth target-paths target-location)}
      {:source :unknown :target :unknown})))

(defn determine-source-target [resource-name
                               existing-source-paths
                               existing-target-paths
                               target-paths
                               target-project
                               ask-for-input]
  (cond
    ;source and target exist and unique
    (and (= 1 (count existing-source-paths))
         (= 1 (count existing-target-paths)))
    {:source (first existing-source-paths)
     :target (first existing-target-paths)}

    ;source  exists, target doen't exist but its location is unique
    (and (= 1 (count existing-source-paths))
         (= 0 (count existing-target-paths))
         (= 1 (count target-paths)))
    {:source (first existing-source-paths)
     :target (first target-paths)}

    ;multiple sources and targets exist so ask user for correct locations
    (<= 1 (count existing-source-paths))
    (ask-for-input resource-name target-project existing-source-paths target-paths)

    ;default
    :else {:source :unknown :target :unknown}))

(defn safe-update! [resource-name target-project source-paths target-paths]
  (m/info "* Update" resource-name "to the project" (str/upper-case target-project))
  (let [existing-source-paths (filter u/exists? source-paths)
        existing-target-paths (filter u/exists? target-paths)
        {source :source target :target} (determine-source-target resource-name
                                                                 existing-source-paths
                                                                 existing-target-paths
                                                                 target-paths
                                                                 target-project
                                                                 ask-for-source-and-target)]
    (if (and (not= source :unknown) (not= target :unknown))
      (update-files! source target)
      (log-warning resource-name target-project))))

(defn update-name-space! [name-space target-project source-project-desc target-project-desc]
  (if (should-update? namespace-def name-space target-project-desc)
    (safe-update!
     name-space
     target-project
     (namespace->source-path name-space source-project-desc)
     (namespace->target-path name-space target-project target-project-desc))))

(defn update-resource! [resource target-project source-project-desc target-project-desc]
  (if (should-update? resource-def resource target-project-desc)
    (safe-update!
     resource
     target-project
     (resource->source-path resource source-project-desc)
     (resource->target-path resource target-project target-project-desc))))

(defn update-namespaces! [namespaces source-project-desc target-projects-desc]
  (m/info "\n*********************** UPDATE NAMESPACES ***********************\n*")
  (doseq [[namespace target-project] namespaces]
    (update-name-space! namespace target-project source-project-desc (get target-projects-desc (keyword target-project)))))

(defn update-resouces! [resources source-project-desc target-projects-desc]
  (m/info "\n*********************** UPDATE RESOURCES ***********************\n*")
  (doseq [[resource target-project] resources]
    (update-resource! resource target-project source-project-desc (get target-projects-desc (keyword target-project)))))