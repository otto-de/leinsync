(ns leiningen.namespaces
  (:refer-clojure :exclude [run! list])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [leiningen.utils :as u]
            [leiningen.core.project :as p]
            [clojure.pprint :as pp]
            [digest :as d]
            [leiningen.core.main :as m]))

(def namespace-def [:ns-sync :namespaces])
(def resource-def [:ns-sync :resources])
(def test-cmd-def [:ns-sync :test-cmd])
(def src-path-def [:source-paths])
(def test-path-def [:test-paths])
(def resource-path-def [:resource-paths])
(def standard-test-cmd [["./lein.sh" "clean"] ["./lein.sh" "test"]])

;;;; Read target project  helper  ;;;;
(defn ->target-project-path [project-name]
  (str "../" project-name))

(defn read-target-project-clj [p]
  (-> p
      (->target-project-path)
      (str "/project.clj")
      (p/read-raw)))

(defn read-all-target-project-clj [target-projects]
  (zipmap (map keyword target-projects) (map read-target-project-clj target-projects)))

(defn project-clj-of [m p]
  (get-in m [(keyword p)]))

;;;; Sync Logic ;;;;;

(defn test-cmd [target-project]
  (let [cmds (-> target-project
                 (read-target-project-clj)
                 (get-in test-cmd-def))]
    (if (empty? cmds) standard-test-cmd cmds)))

(defn sync-get-in [project-clj test-path-def default]
  (-> (get-in project-clj test-path-def default)
      (distinct)))

(defn test-or-source-namespace [namespace project-clj]
  (if (or (.endsWith namespace "-test")
          (.contains namespace "test"))
    (sync-get-in project-clj test-path-def ["test"])
    (sync-get-in project-clj src-path-def ["src"])))

(defn split-path [path project-desc]
  {:src-or-test    (test-or-source-namespace path project-desc)
   :namespace-path (-> path
                       (str/replace #"-" "_")
                       (str/replace #"\." "/")
                       (str ".clj"))})

(defn resource->target-path [resource target-project target-project-desc]
  (let [resource-folders (get-in target-project-desc resource-path-def)]
    (map
     #(str (->target-project-path target-project) "/" % "/" resource)
     resource-folders)))

(defn resource->source-path [resource source-project-desc]
  (map
   #(str % "/" resource)
   (get-in source-project-desc resource-path-def)))

(defn namespace->target-path [namespace target-project target-project-desc]
  (let [{folders :src-or-test
         ns-path :namespace-path} (split-path namespace target-project-desc)]
    (map
     #(str (->target-project-path target-project) "/" % "/" ns-path)
     folders)))

(defn namespace->source-path [namespace source-project-desc]
  (let [{folders :src-or-test ns-path :namespace-path} (split-path namespace source-project-desc)]
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
    (update-name-space! namespace target-project source-project-desc (project-clj-of target-projects-desc target-project)))
  (m/info "*\n****************************************************************\n"))

(defn update-resouces! [resources source-project-desc target-projects-desc]
  (m/info "\n*********************** UPDATE RESOURCES ***********************\n*")
  (doseq [[resource target-project] resources]
    (update-resource! resource target-project source-project-desc (project-clj-of target-projects-desc target-project)))
  (m/info "*\n****************************************************************\n"))

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

(defn md5-hash [path]
  (d/digest "md5" (io/as-file path)))

(defn project-occurence-render [paths project]
  (let [hash-value (str/join "|" (map md5-hash paths))
        hash-length (dec (count hash-value))
        short-hash-value (subs hash-value (- hash-length 5) hash-length)]
    (if (empty? paths)
      {project (str "O " short-hash-value)}
      {project short-hash-value})))

(defn resource-occurence [resource project project-desc render]
  (let [paths (concat (resource->target-path resource (name project) project-desc)
                      (namespace->target-path resource (name project) project-desc))
        existing-path (filter u/exists? paths)]
    (render existing-path project)))

(defn map-ns->project [project project-desc empty-project-occurence render]
  (fn [resource]
    [(keyword resource)
     (merge-with
      str
      (resource-occurence resource project project-desc render)
      empty-project-occurence)]))

(defn resource-name->project [projects selector render]
  (let [empty-occurence (empty-project-occurence projects "")]
    (reduce-kv
     (fn [m project desc]
       (into m (map
                (map-ns->project project desc empty-occurence render)
                (get-in desc selector))))
     []
     projects)))

(defn occurence-map-for [k selector]
  (if (= selector namespace-def)
    (let [name-segments (str/split (name k) #"\.")]
      {:package (str/join "." (drop-last name-segments))
       :name (last name-segments)})
    {:name (name k)}))

(defn ->pretty-print-structure [data selector]
  (reduce-kv
   (fn [m k v] (conj m (merge
                        (occurence-map-for k selector)
                        v)))
   []
   data))

(defn log-resouces-table [m resource-name]
  (m/info "\n* List of" resource-name)
  (m/info "     - hash-value (.i.e a3d66)  :  the namespace/resource is defined in the project.clj")
  (m/info "     - O  :                     :  the namespace/resource does not exist in the project")
  (pp/print-table (sort-by :name m))
  (m/info "\n"))

(defn build-resource-table [projects selector render]
  (-> projects
      (resource-name->project selector render)
      (merge-project-occurence)
      (->pretty-print-structure selector)))

;;;;; Command Actions ;;;;;

(defn- lein-test [project]
  (m/info "\n... Executing tests of" project "on" (u/output-of (sh/sh "pwd")))
  (let [failed-cmd (->> project
                        (test-cmd)
                        (map u/run-cmd)
                        (filter #(= (:result %) :failed)))]
    (if (empty? failed-cmd)
      (do
        (m/info "===> All tests of" project "are passed\n")
        {:project project :result :passed})
      (do
        (m/info "===> On" project "some tests are FAILED when executing"
                (str/join " and " (map :cmd failed-cmd)) "\n")
        {:project project :result :failed}))))

(defn- reset-project! [project]
  (m/info "\n... Reset changes of" project "on" (u/output-of (sh/sh "pwd")))
  (if (u/is-success? (sh/sh "git" "checkout" "."))
    (m/info "===> Reset all changes")
    (m/info "===> Could NOT reset changes on" project)))

(defn- get-changed-files []
  (u/output-of (sh/sh "git" "diff" "--name-only")))

(defn- diff [project]
  (let [changes (get-changed-files)]
    (if (empty? changes)
      (m/info "* No update has been applied on the project" project "\n")
      (m/info "* Changes on project" project "\n\n" changes))))

(defn- commit-project! [project commit-msg]
  (if (not (empty? (get-changed-files)))
    (let [commit-result (sh/sh "git" "commit" "-am" commit-msg)]
      (if (not (u/is-success? commit-result))
        (m/info "===> Could not commit because" (u/error-of commit-result))
        (m/info "Commited")))
    (m/info "\n* No change to be committed on" project)))

(defn- pull-rebase! [project]
  (m/info "\n* Pull on" project)
  (let [pull-result (sh/sh "git" "pull" "-r")]
    (if (not (u/is-success? pull-result))
      (m/info "===> Could not commit because" (u/error-of pull-result))
      (m/info (u/output-of pull-result)))))

(defn- unpushed-commit []
  (u/output-of (sh/sh "git" "diff" "origin/master..HEAD" "--name-only")))

(defn- push! [p]
  (let [push-result (sh/sh "git" "push" "origin")]
    (if (not (u/is-success? push-result))
      (m/info "===> Could not push on" p "because" (u/error-of push-result))
      (m/info (u/output-of push-result)))))

(defn- check-and-push! [project]
  (if (empty? (unpushed-commit))
    (m/info "\n ===> Nothing to push on" project)
    (if (= "y" (-> (str "\n* Are you sure to push on " project "? (y/n)")
                   (u/ask-user u/yes-or-no)))
      (push! project))))

(defn- status [project]
  (m/info "\n * Status of" project)
  (let [status-result (sh/sh "git" "status")]
    (if (not (u/is-success? status-result))
      (m/info "===> Could not get status because" (u/error-of status-result))
      (m/info (u/output-of status-result)))))

(defn- test-all [projects]
  (doall
   (map
    #(u/run-command-on (->target-project-path %) lein-test %)
    projects)))

(defn- log-test-hints [passed-projects failed-project]
  (when (not (empty? failed-project))
    (m/info "* Please have a look  at the failed project(s):" failed-project))
  (when (not (empty? passed-projects))
    (m/info "* Tests are passed on project(s):" passed-projects "\n\n")
    (m/info "To see changes : lein sync" passed-projects "--diff")
    (m/info "To commit      : lein sync" passed-projects "--commit")
    (m/info "To push        : lein sync" passed-projects "--push")))

(defn list-resources [projects-desc selector]
  (-> projects-desc
      (build-resource-table selector project-occurence-render)
      (log-resouces-table (name (last selector)))))

;;;;; Sync Commands ;;;;;

(defn reset-all! [projects _]
  (doseq [p projects]
    (u/run-command-on (->target-project-path p) reset-project! p)))

(defn commit-all! [projects _]
  (let [commit-msg (->> projects
                        (str/join ",")
                        (str "\nPlease enter the commit message for the projects: ")
                        (u/ask-user))]
    (doseq [p projects]
      (u/run-command-on (->target-project-path p) commit-project! p commit-msg))
    (m/info "\n\nTo push        : lein sync" (str/join "," projects) "--push")))

(defn pull-rebase-all! [projects _]
  (doseq [p projects]
    (u/run-command-on (->target-project-path p) pull-rebase! p)))

(defn push-all! [projects _]
  (doseq [p projects]
    (u/run-command-on (->target-project-path p) check-and-push! p)))

(defn status-all [projects _]
  (doseq [p projects]
    (u/run-command-on (->target-project-path p) status p)))

(defn show-all-diff [projects _]
  (doseq [p projects]
    (u/run-command-on (->target-project-path p) diff p)))

(defn update-projects! [target-projects source-project-desc]
  (let [all-target-projects-desc (read-all-target-project-clj target-projects)
        namespaces (u/cartesian-product (get-in source-project-desc namespace-def) target-projects)
        resources (u/cartesian-product (get-in source-project-desc resource-def) target-projects)]
    (if (not (empty? namespaces)) (update-namespaces! namespaces source-project-desc all-target-projects-desc))
    (if (not (empty? resources)) (update-resouces! resources source-project-desc all-target-projects-desc))))

(defn run-test [target-projects _]
  (let [results (test-all target-projects)]
    (log-test-hints (->> results
                         (filter #(= (:result %) :passed))
                         (map :project)
                         (str/join ","))
                    (->> results
                         (filter #(= (:result %) :failed))
                         (map :project)
                         (str/join ",")))))

(defn list [target-projects {source-project :name}]
  (let [all-projects-desc (-> target-projects
                              (conj source-project)
                              (read-all-target-project-clj))]
    (list-resources all-projects-desc namespace-def)
    (list-resources all-projects-desc resource-def)))