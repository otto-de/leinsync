## Example
In order to synchronize namespaces between projects, run in the current source project:

    $ cd project-1
    $ lein sync "project-2"
    *********************** UPDATE NAMESPACES ***********************
    *
    * Update ns.namespace-1 to the project PROJECT-2
    *
    ****************************************************************
    
    
    *********************** UPDATE RESOURCES ***********************
    *
    * Update default.edn to the project PROJECT-2
    *
    ****************************************************************
    
    |****************************| ../project-2 |****************************|
    
    ... Executing tests of project-2 on .../leinsync/example/project-2
    
    ... Executing lein test
    ===> On project-2 some tests are FAILED when executing lein test
