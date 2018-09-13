(defproject sync "0.9.44"
  :description "sync is a Leiningen plugin to sync same code base between different clojure projects"
  :url "https://github.com/otto-de/leinsync"
  :license {:name "Apache License 2.0"
            :url  "http://www.apache.org/license/LICENSE-2.0.html"}
  :eval-in-leiningen true
  :test-selectors {:default (constantly true)
                   :unit    :unit
                   :focused :focused}

  :dependencies [[org.clojure/math.combinatorics "0.1.4"]
                 [com.sun.jna/jna "3.0.9"]
                 [com.github.jnr/jnr-posix "3.0.46"]
                 [digest "1.4.8"]
                 [ancient-clj "0.6.15"]
                 [slingshot "0.12.2"]
                 [org.clojure/tools.cli "0.4.0"]]

  :profiles {:uberjar {:aot :all}
             :test    {:resource-paths ["test-resources"]}
             :dev     {:dependencies [[pjstadig/humane-test-output "0.8.3"]]
                       :plugins      [[lein-cljfmt "0.6.0"]
                                      [lein-cloverage "1.0.13"]
                                      [jonase/eastwood "0.2.9"]
                                      [lein-kibit "0.1.6"]]}})
