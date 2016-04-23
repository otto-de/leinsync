(ns leiningen.constant)

(def clojure-file-ending ".clj")

(def sync-spec-seletor :ns-sync)

(def warning "!*** WARNING ***!: COULD NOT UPDATE BECAUSE: ")

(def usage-msg "Usage:

1. lein sync \"project-1,project-2,project-3\"
==> define :ns-sync [\"namespace.to.be-sync.1\" test:\"namespace.to.be-sync.2\"] in the project.clj

2. lein sync \"project-1,project-2\" \"namespace.to.sync.1,namespace.to.sync.2\"
==> define sync namespaces explicitly with the second parameter")