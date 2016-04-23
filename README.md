# sync

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

to use sync please checkout the project and run:

    $ lein install
    Created ...sync/target/sync-0.1.0-SNAPSHOT.jar
    Wrote ...sync/pom.xml
    Installed jar and pom into local repo.

    $ echo "{:user  {:plugins  [[sync  \"0.1.0-SNAPSHOT\"]] :dependencies  []}}" > ~/.lein/profiles.clj

now sync has already been installed in your local repository

put `:sync ["namespace.to.be.sync.1" "namespace.to.be.sync.2"]` in your project.clj.

In order to synchronize namespaces between projects, run in the current source project:

    $ cd big-repository/project1
    $ lein sync "project2,project3,project4"

alternatively, you can define the namespaces explicitly:

    $ lein sync "project2,project3,project4" "namespace.to.be.sync.1,namespace.to.be.sync.2"
