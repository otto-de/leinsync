# Introduction

[![Join the chat at https://gitter.im/otto-de/leinsync](https://badges.gitter.im/otto-de/leinsync.svg)](https://gitter.im/otto-de/leinsync?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/otto-de/leinsync.svg?branch=master)](https://travis-ci.org/otto-de/leinsync)
[![Coverage Status](https://coveralls.io/repos/github/otto-de/leinsync/badge.svg?branch=master)](https://coveralls.io/github/otto-de/leinsync?branch=master)
[![Dependencies Status](http://jarkeeper.com/otto-de/leinsync/status.svg)](http://jarkeeper.com/otto-de/leinsync)

[![Clojars Project](http://clojars.org/sync/latest-version.svg)](https://clojars.org/sync)

Our system consists of a set of micro services written in clojure.
Although we have codes shared between these clojure projects, we have consciously decided against shared libraries.
By doing in this way, we gained a greater flexibility to adjust/change them.
At the beginning, we synchronized those shared code by hand, which was of course not very comfortable. 
This is the reason why we wrote this plugin `sync`to make this task automatically.

# Sync Plugin
`sync` is a Leiningen plugin to synchronize shared codebase between clojure projects. 

`sync` makes a strong assumption about the file structure of the shared projects which are created by Leiningen, like this:

``` ruby
container-folder/
+-- project-1
    +-- src
    ¦   +-- name.space.1
    ¦   +-- name.space.2
    +-- test
    ¦   +-- name.space-test.1
    ¦   +-- name.space-test.2
    +-- project.clj
+-- project-2
    +-- src
    ¦   +-- name.space.1
    ¦   +-- name.space.2
    +-- test
    ¦   +-- name.space-test.1
    ¦   +-- name.space-test.2
    +-- project.clj
+-- project-3
    +-- src
    ¦   +-- name.space.1
    ¦   +-- name.space.2
    +-- test
    ¦   +-- name.space-test.1
    ¦   +-- name.space-test.2
    +-- project.clj
```

## Usage

* lein sync [options] "project-1,project-2,project-3"

Options:
   + --deps    :  List all profile/global deps on projects.
   + --list    :  List resources to be synchronized.
   + --notest  :  Synchronize shared code base without executing tests on target projects.
   + --test    :  Executing tests on target projects.
   + --reset   :  Reset all uncommitted changes in all target projects.
   + --status  :  Check status on target projects.
   + --commit  :  Commit change on target projects.
   + --pull    :  Pull rebase on target projects.
   + --push    :  Push on target projects.

Define `:ns-sync` configuration in the project.clj of each target project. It has the following options:

  + `:test-cmd` specifies which leiningen tasks will be executed to test target projects after synchronizing.
  + `:namespaces` specifies the namespaces to be synchronized between shared projects.
  + `:resources`  specifies the resources to be synchronized between shared projects.
  
A namespace/resource will be synchronized between 2 projects if and only if they are defined in the both project.clj.

## Example
The `:ns-sync` configuration can be specified like that:

```clojure
:ns-sync { :test-cmd    [["./lein.sh" "profile-1" "test"] 
                         ["./lein.sh" "profile-2"  "test"]]
           :namespaces  ["name.space.1" "name.space.2"]
           :resources   ["resources.1" "resources.2"]}
```

In order to synchronize namespaces and resources between projects, run in the current source project:

    $ cd container-folder/project-1
    $ lein sync "project-2,project-3"
    *********************** UPDATE NAMESPACES ***********************
    *
    * Update name.space.1 to the project project-2
    * Update name.space.1 to the project project-3

    *********************** UPDATE RESOURCES ***********************
    *
    * Update "resources.1 to the project project-2
    * Update "resources.1 to the project project-3

     ****** Executing tests of project-2        ******

    ... Executing  ./lein.sh profile-1 test
    ... Executing  ./lein.sh profile-2 test


     ****** Executing tests of project-3        ******

    ... Executing  ./lein.sh profile-1 test
    ... Executing  ./lein.sh profile-2 test

    | :project | :result | :./lein.sh clean | :./lein.sh test | 
    |----------+---------+------------------+-----------------|
    |project-2 | :passed |          :passed |         :passed |
    |----------------------------------------------------------
    |project-3 | :passed |          :passed |         :passed |
    |---------------------------------------------------------|


    * Tests are passed on project(s): project-2,project-3


    To see changes : lein sync project-2,project-3 --status
    To commit      : lein sync project-2,project-3 --commit
    To push        : lein sync project-2,project-3 --push


`sync` will update the predefined namespaces/resources from project-1 to project-2 and project-3.
It updates only the namespace, which has been defined in both source project and target project.
If a namespace is  only defined in the source or target project, it will be ignored.
Afterwards `sync` will execute tests, which are defined in `:test-cmd` on project-2 project-3 and project-4, to make sure that the update did not break anything.