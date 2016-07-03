(defproject sync "0.9.31-SNAPSHOT"
  :description "sync is a Leiningen plugin to sync same code base between different clojure projects"
  :url "https://github.com/otto-de/leinsync"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :eval-in-leiningen true
  :test-selectors {:default (constantly true)
                   :unit    :unit
                   :focused :focused}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/math.combinatorics "0.1.3"]
                 [com.sun.jna/jna "3.0.9"]
                 [com.github.jnr/jnr-posix "3.0.29"]
                 [digest "1.4.4"]
                 [ancient-clj "0.3.14"]
                 [org.clojure/tools.cli "0.3.5"]
                 [com.fasterxml.jackson.core/jackson-core "2.2.3"]
                 [com.fasterxml.jackson.core/jackson-databind "2.2.3"]]

  :profiles {:uberjar {:aot :all}
             :test    {:resource-paths ["test-resources"]}
             :dev     {:dependencies [[pjstadig/humane-test-output "0.8.0"]]
                       :plugins      [[lein-cljfmt "0.5.3"]
                                      [lein-cloverage "1.0.6"]
                                      [jonase/eastwood "0.2.3"]]}})
