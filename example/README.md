## Example
In order to synchronize namespaces between projects, run in the current source project:

    $ cd project-1
    $ lein sync "project-2"
    UPDATE ../project-2/src/ns/namespace_1.clj
    UPDATE ../project-2/src/ns/namespace_1.clj
    |===================================| PROJECT |===================================|

    ... Executing tests of project-2 on ..leinsync/example/project-2
    ===> Some Tests of project-2 are FAILED!!!
