(defproject project-1 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :main ^:skip-aot project-1.core
  :target-path "target/%s"

  :resource-paths  ["resources"]

  ;define here the namespaces which will be updated by leinsync
  :ns-sync {:namespaces  ["ns.namespace-1" "ns.namespace-2"]           
            :test-cmd    [["lein" "test"]]
            :resources   ["default.edn"] } 
  :profiles {:uberjar {:aot :all}
             :dev     {:plugins [[sync "0.9.6"]]}})
