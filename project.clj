(defproject sync "0.8.0"
  :description "sync is a Leiningen plugin to sync same code base between different clojure projects"
  :url "https://github.com/otto-de/leinsync"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :eval-in-leiningen true
  :test-selectors {:default (constantly true)
                   :unit    :unit
                   :focused :focused}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/math.combinatorics "0.1.1"]
                 [com.sun.jna/jna "3.0.9"]
                 [com.github.jnr/jnr-posix "3.0.29"]
                 [org.clojure/tools.cli "0.3.3"]]
  :profiles {:uberjar {:aot :all}
             :test    {:resource-paths ["test-resources"]}
             :dev     {:dependencies [[pjstadig/humane-test-output "0.7.0"]]
                       :plugins      [[com.jakemccrary/lein-test-refresh "0.14.0"]]}})
