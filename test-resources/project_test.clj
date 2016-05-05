(def version (or (System/getenv "VERSION_NUMBER") "LOCAL"))

(defproject test version
  :description "des"
  :repositories []

  :dependencies [[org.clojure/clojure "1.8.0"]]

  :exclusions [[org.slf4j/slf4j-nop]
               [org.slf4j/log4j-over-slf4j]
               [org.slf4j/slf4j-log4j12]
               ]
  :target-path "target/%s"
  :sync ["cool.ns.1"
         "cool.ns.2"
         "cool.ns.3"]
  :test-selectors {:default   (constantly true)
                   :unit      :unit
                   :component :component
                   :focused   :focused
                   :all       (constantly true)}
  :aliases {}

  :source-paths ["folder1" "folder2"]
  :test-paths   ["testfolder1" "testfolder2"]

  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies [[org.clojure/test.check "0.9.0"]]
                       :plugins      []}
             :test    {:resource-paths ["test-resources"]}})
