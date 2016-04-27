# Introduction

[![Build Status](https://travis-ci.org/otto-de/leinsync.svg?branch=master)](https://travis-ci.org/otto-de/leinsync)
[![Clojars Project](https://img.shields.io/clojars/v/sync.svg)](https://clojars.org/sync)

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
+-- project-2
    +-- src
    ¦   +-- name.space.1
    ¦   +-- name.space.2
    +-- test
    ¦   +-- name.space-test.1
    ¦   +-- name.space-test.2
+-- project-3
    +-- src
    ¦   +-- name.space.1
    ¦   +-- name.space.2
    +-- test
    ¦   +-- name.space-test.1
    ¦   +-- name.space-test.2
+-- project-4
    +-- src
    ¦   +-- name.space.1
    ¦   +-- name.space.2
    +-- test
    ¦   +-- name.space-test.1
    ¦   +-- name.space-test.2
```

## Usage

Define `:ns-sync ["name.space.1" "name.space.2"]` in the project.clj of each.

* lein [options] sync "project-1,project-2,project-3"

* lein [options] sync "project-1,project-2" "name.space.1,name.space.2"

Options:
   + --notest  :  Synchronize shared code base without executing tests on target projects.
   + --reset   :  Reset all uncommitted changes in all target projects.
   + --show    :  Show changes on target projects
   + --commit  :  Commit change on target projects

## Example
In order to synchronize namespaces between projects, run in the current source project:

    $ cd container-folder/project-1
    $ lein sync "project-2,project-3,project-4"
    UPDATE ../container-folder/project-2/name/space/1.clj
    UPDATE ../container-folder/project-3/name/space/1.clj
    UPDATE ../container-folder/project-4/name/space/1.clj
    ... Executing tests of project-2 on ../container-folder/project-2/
    ===> All Tests of project-2 are passed
    ... Executing tests of project-3 on ../container-folder/project-3/
    ===> All Tests of project-3 are passed
    ... Executing tests of project-4 on ../container-folder/project-4/
    ===> All Tests of project-4 are passed


`sync` will update the namespaces from project-1 to project-2 project-3 and project-4.
It updates only the namespace, which has been defined in both source project and target project.
If a namespace is  only defined in the source or target project, it will be ignored.
Afterwards `sync` will execute tests on project-2 project-3 and project-4, to make sure that the update did not break anything.

Alternatively, you can define the namespaces explicitly:

    $ lein sync "project-2,project-3,project-4" "name.space.1"
