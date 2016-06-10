# Introduction

[![Build Status](https://travis-ci.org/otto-de/leinsync.svg?branch=master)](https://travis-ci.org/otto-de/leinsync)
[![Clojars Project](https://img.shields.io/clojars/v/sync.svg)](https://clojars.org/sync)
[![Dependencies Status](http://jarkeeper.com/otto-de/leinsync/status.svg)](http://jarkeeper.com/otto-de/leinsync)

Our system consists of a set of small micro services written in clojure.
Although we have codes shared between our clojure projects, we have consciously decided against shared libraries.
By doing in this way, we gained a greater flexibility to adjust/change our micro services.
At the beginning, we synchronized those shared code by hand, which was of course not very comfortable. So we wrote `sync`to make this task automatically.

# Sync Plugin
`sync` is a Leiningen plugin to synchronize shared codebase between clojure projects. `sync` makes a strong assumption about the file structure of  the shared projects, created by Leiningen, like this:

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
   + --list    :  List resources to be synchronized.
   + --notest  :  Synchronize shared code base without executing tests on target projects.
   + --test    :  Executing tests on target projects
   + --reset   :  Reset all uncommitted changes in all target projects.
   + --status  :  Check status on target projects
   + --diff    :  Show changes on target projects
   + --commit  :  Commit change on target projects
   + --pull    :  Pull rebase on target projects
   + --push    :  Push on target projects

Define `:ns-sync` configuration in the project.clj of each target project. It has the following options:

  + `:test-cmd` specifies which leiningen tasks will be executed to test target projects.
  + `:namespaces` specifies the namespaces to be synchronized.
  + `:resources`specifies the resources to be synchronized.

## Example
The `:ns-sync` configuration can be specified like that:

```json
:ns-sync { :test-cmd [["./lein.sh" "profile-1" "test"] ["./lein.sh" "profile-2"  "test"]]
           :namespaces ["name.space.1" "name.space.2"]
           :resources ["resources.1" "resources.2"]}
```

In order to synchronize namespaces and resources between projects, run in the current source project:

    $ cd container-folder/project-1
    $ lein sync "project-2,project-3"
    *********************** UPDATE NAMESPACES ***********************
    *
    * Update name.space.1 to the project project-2
    * Update name.space.1 to the project project-3
    *

    *********************** UPDATE RESOURCES ***********************
    *
    * Update "resources.1 to the project project-2
    * Update "resources.1 to the project project-3
    *


    |****************************| ../project-2   |****************************|
    ... Executing tests of project-2

    ... Executing  ./lein.sh profile-1 test
    ... Executing  ./lein.sh profile-2 test
    ===> All tests of project-2 are passed


    ****************************| ../project-3   |****************************|
    ... Executing tests of project-2

    ... Executing  ./lein.sh profile-1 test
    ... Executing  ./lein.sh profile-2 test
    ===> All tests of project-3 are passed


    * Tests are passed on projects: project-2,project-3


    To see changes : lein sync project-2,project-3 --diff
    To commit      : lein sync project-2,project-3 --commit
    To push        : lein sync project-2,project-3 --push


`sync` will update the namespaces from project-1 to project-2 and project-3.
It updates only the namespace, which has been defined in both source project and target project.
If a namespace is  only defined in the source or target project, it will be ignored.
Afterwards `sync` will execute tests on project-2 project-3 and project-4, to make sure that the update did not break anything.