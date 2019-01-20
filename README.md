# Introduction

[![Build Status](https://travis-ci.org/otto-de/leinsync.svg?branch=master)](https://travis-ci.org/otto-de/leinsync)
[![Coverage Status](https://coveralls.io/repos/github/otto-de/leinsync/badge.svg?branch=master)](https://coveralls.io/github/otto-de/leinsync?branch=master)
[![Dependencies Status](https://versions.deps.co/otto-de/leinsync/status.svg)](https://versions.deps.co/otto-de/leinsync)
[![Downloads](https://versions.deps.co/otto-de/sync/downloads.svg)](https://versions.deps.co/otto-de/sync)

[![Clojars Project](http://clojars.org/sync/latest-version.svg)](https://clojars.org/sync)

Our system consists of a set of micro services written in clojure.
Although we have codes shared between these clojure projects, we have consciously decided against shared libraries.
By doing in this way, we gained a greater flexibility to adjust/change them.
At the beginning, we synchronized those shared code by hand, which was of course not very comfortable. 
This is the reason why we wrote this plugin `sync`to make this task automatically.

`sync` plugin has been successfully tested with the Leiningen version `2.7.1`. If you have any issue dealing with Leiningen, please upgrade your project with the Leiningen version greater or equal to `2.7.1`.

# Sync Plugin
`sync` is a Leiningen plugin to synchronize shared codebase between clojure projects. 

`sync` makes a strong assumption about the file structure of the shared projects which are created by Leiningen, like this:

``` ruby
container-folder/
+-- project-1
    +-- src
    ¦   +-- package.1
    ¦   +---- name.space.1
    ¦   +---- name.space.2
    +-- test
    ¦   +-- name.space-test.1
    ¦   +-- name.space-test.2
    +-- project.clj
+-- project-2
    +-- src
    ¦   +-- package.1
    ¦   +---- name.space.1
    ¦   +---- name.space.2
    +-- test
    ¦   +-- name.space-test.1
    ¦   +-- name.space-test.2
    +-- project.clj
+-- project-3
    +-- src
    ¦   +-- package.1
    ¦   +---- name.space.1
    ¦   +---- name.space.2
    +-- test
    ¦   +-- name.space-test.1
    ¦   +-- name.space-test.2
    +-- project.clj
```

For example, if some code change is made on the  `name.space.1` of the `project-1`, we want to apply this change later on all others projects containing `name.space.1`.

``` ruby
container-folder/
+-- project-1
    +-- src
    ¦   +-- name.space.1 ---------+
    ¦   +-- name.space.2          |
    +-- project.clj               |
+-- project-2                     |
    +-- src                       |
    ¦   +-- name.space.1 <--sync--+        
    +-- project.clj               |
+-- project-3                     |
    +-- src                       |
    ¦   +-- name.space.1 <--sync--+
    ¦   +-- name.space.3       
    +-- project.clj
```


## Usage
To specify which projects should be synchronized, a search string must be passed as argument to `sync`. A search string can be a string concatnation of projects or a java's regex. For example:

* lein sync [options] "project-1,project-2,project-3"
* lein sync [options] "project-.*"
* lein sync "(.*)[^(shouldNotMatch)]$"

Options:
   + --interactive       : Activate the interactive mode to choose projects
   + --deps              : List all profile/global deps on projects.
   + --list              : List resources to be synchronized.
   + --include-namespace : Synchronize only the passed namespaces.
   + --include-resource  : Synchronize only the passed resources.
   + --include-package   : Synchronize only the passed packages.
   + --notest            : Synchronize shared code base without executing tests on target projects.
   + --test              : Executing tests on target projects.
   + --reset             : Reset all uncommitted changes in all target projects.
   + --status            : Check status on target projects.
   + --commit            : Commit change on target projects.
   + --pull              : Pull rebase on target projects.
   + --push              : Push on target projects.

Define `:sync` configuration in the project.clj of each target project. It has the following options:

  + `:test-cmd` specifies which leiningen tasks will be executed to test target projects after synchronizing.
  + `:namespaces` specifies the namespaces to be synchronized between shared projects.
  + `:packages` specifies the packages to be synchronized between shared projects.
  + `:resources`  specifies the resources to be synchronized between shared projects.
  
A namespace/resource/package will be synchronized between 2 projects if and only if they are defined in the both project.clj.

## Example
**Breaking change**: since the version `0.9.47`the selector names `:sync` instead of `:ns-sync`. The `:sync` configuration can be specified like that:

```clojure
:sync { :test-cmd    [["./lein.sh" "profile-1" "test"] 
                      ["./lein.sh" "profile-2" "test"]]
        :namespaces  ["name.space.1" "name.space.2"]
        :packages    ["package.path.1" "package.path.2"]
        :resources   ["resource.1"   "resource.2"]}
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
