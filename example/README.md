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
    
    $ lein sync project-2 --list

      * List of namespaces
           - X  :  the namespace/resource is defined in the project.clj
           - O  :  the namespace/resource does not exist in the project
      
      | :package |       :name | :project-1 | :project-2 |
      |----------+-------------+------------+------------|
      |       ns | namespace-1 |          X |          X |
      |       ns | namespace-2 |          X |            |
      
      
      
      * List of resources
           - X  :  the namespace/resource is defined in the project.clj
           - O  :  the namespace/resource does not exist in the project
      
      |       :name | :project-1 | :project-2 |
      |-------------+------------+------------|
      | default.edn |          X |          X |
                      

    
    
    
    
    
    
    
    
    
