(defproject sync "0.9.34-SNAPSHOT"
  :description "sync is a Leiningen plugin to sync same code base between different clojure projects"
  :url "https://github.com/otto-de/leinsync"
  :license {:name "Apache License 2.0"
            :url  "http://www.apache.org/license/LICENSE-2.0.html"}
  :eval-in-leiningen true
  :test-selectors {:default (constantly true)
                   :unit    :unit
                   :focused :focused}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/math.combinatorics "0.1.3"]
                 [com.sun.jna/jna "3.0.9"]
                 [com.github.jnr/jnr-posix "3.0.31"]
                 [digest "1.4.5"]
                 [ancient-clj "0.3.14"]
                 [org.clojure/tools.cli "0.3.5"]]

  :profiles {:uberjar {:aot :all}
             :test    {:resource-paths ["test-resources"]}
             :dev     {:dependencies [[pjstadig/humane-test-output "0.8.1"]]
                       :plugins      [[lein-cljfmt "0.5.6"]
                                      [lein-cloverage "1.0.8"]
                                      [jonase/eastwood "0.2.3"]
                                      [lein-kibit "0.1.2"]]}})
