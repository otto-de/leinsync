(defproject project-2 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :main ^:skip-aot project-2.core
  :target-path "target/%s"

  ;define here the namespaces which will be updated by leinsync
  :ns-sync ["ns.namespace-1"]
  :profiles {:uberjar {:aot :all}
             :dev     {:plugins [[sync "0.9.3"]]}})