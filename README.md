# sync

[![Clojars Project](https://img.shields.io/clojars/v/sync.svg)](https://clojars.org/sync)

`sync` is a Leiningen plugin to synchronize the shared codebase between different clojure Leiningen projects.

`sync` makes an assumption about the file structure of the shared Leiningen projects like this:

---+ big-repository

--------+ project1

----------------+src

----------------+test

--------+ project2

----------------+src

----------------+test

--------+ project3

----------------+src

----------------+test

--------+ project4

----------------+src

----------------+test

## Usage

put `:ns-sync ["namespace.to.be.sync.1" "namespace.to.be.sync.2"]` in your project.clj.

In order to synchronize namespaces between projects, run in the current source project:

    $ cd big-repository/project1
    $ lein sync "project2,project3,project4"

Alternatively, you can define the namespaces explicitly:

    $ lein sync "project2,project3,project4" "namespace.to.be.sync.1,namespace.to.be.sync.2"
