## Example
In order to synchronize namespaces between projects, run in the current source project:

    $ cd project-1
    $ lein sync "project-2"
    *********************** UPDATE NAMESPACES ***********************
    *
    * Update ns.namespace-1 to the project PROJECT-2
        
    
    *********************** UPDATE RESOURCES ***********************
    *
    * Update default.edn to the project PROJECT-2
    
    ****** Executing tests of project-2    ******        
    ... Executing lein test
    
    |  :project | :result |  :lein test |
    |-----------+---------+-------------|
    | project-2 | :failed | ==> :failed |
    |-----------------------------------|
    
    * Please have a look  at the failed project(s): project-2
    
    
    
    $ lein sync project-2 --list
    
    * List of namespaces
         - X                          :  the namespace/resource does not exist in the project although it has been specified
         - hash-value (.i.e ddfa3d66) :  the namespace/resource is defined in the project.clj
                                         ==>    hash : means that the resource doesn't match on all projects
                                         =[x]=> hash : means that the resource on this project is different from others
    
    
    | :package |       :name |    :project-1 |    :project-2 |
    |----------+-------------+---------------+---------------|
    |       ns | namespace-1 | =[x]=> 3EC5C1 | =[x]=> 3EC5C1 |
    |       ns | namespace-2 |        7cb5a0 |               |
    
            
            
    
    $ lein sync project-2 --deps global
    
    * List of dependencies of [:dependencies]
       =>  version :  means that the dependency on this project is out-of-date
    
    |                :name | :last-version | :project-1 | :project-2 |
    |----------------------+---------------+------------+------------|
    | :org.clojure/clojure |         1.8.0 |   => 1.7.0 |   => 1.7.0 |
