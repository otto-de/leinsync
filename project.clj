(defproject sync "0.9.49"
  :description "sync is a Leiningen plugin to sync same code base between different clojure projects"
  :url "https://github.com/otto-de/leinsync"
  :license {:name "Apache License 2.0"
            :url  "http://www.apache.org/license/LICENSE-2.0.html"}
  :eval-in-leiningen true
  :test-selectors {:default (constantly true)
                   :unit    :unit
                   :focused :focused}

  :plugins [[org.clojure/core.rrb-vector "0.0.13"]]

  :dependencies [[org.clojure/math.combinatorics "0.1.4"]
                 [com.sun.jna/jna "3.0.9"]
                 [com.github.jnr/jnr-posix "3.0.49"]
                 [digest "1.4.8"]
                 [ancient-clj "0.6.15"]
                 [slingshot "0.12.2"]
                 [org.clojure/tools.cli "0.4.1"]]

  :profiles {:uberjar {:aot :all}
             :test    {:resource-paths ["test-resources"]}
             :dev     {:dependencies [[pjstadig/humane-test-output "0.9.0"]]
                       :plugins      [[lein-cljfmt "0.6.3"]
                                      [lein-cloverage "1.0.13"]
                                      [jonase/eastwood "0.3.4"]
                                      [lein-kibit "0.1.6"]]}})
