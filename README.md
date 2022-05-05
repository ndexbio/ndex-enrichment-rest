
[jetty]: http://eclipse.org/jetty/
[maven]: http://maven.apache.org/
[java]: https://www.oracle.com/java/index.html
[git]: https://git-scm.com/
[ndex]: https://ndexbio.org
[make]: https://www.gnu.org/software/make

[NDEx][ndex] Pathway Relevance REST Service
=====================================

[![Build Status](https://app.travis-ci.com/cytoscape/ndex-enrichment-rest.svg?branch=master)](https://app.travis-ci.com/cytoscape/ndex-enrichment-rest)

Provides a pathway relevance REST service using select networks from [NDEx][ndex] as a backend.
This service runs using an embedded [Jetty][jetty] server and is invoked
from the command line. 


Requirements
============

* Centos 7+, Ubuntu 12+, and most other linux distributions should work
* [Java][java] 11+ **(jdk to build)**
* [Make][make] **(to build)**
* [Maven][maven] 3.6 or higher **(to build)**


Building [NDEx][ndex] Pathway Relevance REST Service
=====================================

Commands build [NDEx][ndex] Pathway Relevance REST Service assuming machine has [Git][git] command line tools 
installed and above Java modules have been installed.

```Bash
# In lieu of git one can just download repo and unzip it
git clone https://github.com/ndexbio/ndex-enrichment-rest.git

cd ndex-enrichment-rest
mvn clean test install
```

The above command will create a jar file under **target/** named  
**ndex-enrichment-rest-\<VERSION\>-jar-with-dependencies.jar** that
is a command line application


Running [NDEx][ndex] Pathway Relevance REST Service
========================================================

The following steps cover how to create the Enrichment database.
In the steps below **enrichment.jar** refers to the jar
created previously named **ndex-enrichment-rest-\<VERSION\>-jar-with-dependencies.jar**

### Step 1 Create directories and configuration file

```bash
# create directory
mkdir -p enrichdb/logs enrichdb/db enrichdb/tasks
cd enrichdb

# Generate template configuration file
java -jar enrichment.jar --mode exampleconf > enrichment.conf
```

The `enrichment.conf` file will look like the following:

```bash
# Example configuration file for Enrichment service

# Sets Enrichment database directory
enrichment.database.dir = /tmp/db

# Sets Enrichment task directory where results from queries are stored
enrichment.task.dir = /tmp/tasks

# Sets HOST URL prefix (value is prefixed to Location header when query is invoked. Can be commented out)
# enrichment.host.url = http://ndexbio.org

# Sets directory where log files will be written for Jetty web server
runserver.log.dir = /tmp/logs

# Sets port Jetty web service will be run under
runserver.port = 8081

# sets Jetty Context Path for Enrichment
runserver.contextpath = /

# Valis log levels DEBUG INFO WARN ERROR ALL
runserver.log.level = INFO

# Number of workers in thread pool
enrichment.number.workers = 1

# number of results to return for a query
number.returned.results = 25

# Algorithm to use to sort results supported values (pvalue, similarity)
sort.algorithm = similarity
```

Replace **/tmp** paths with full path location to **enrichdb** directory 
created earlier.

### Step 2 Create dbresults.json file

This file contains information about networks on [NDEx][ndex] to 
use to generate the Enrichment database.

Run the following to create an example **dbresults.json** file:

```bash
java -jar enrichment.jar --mode exampledbresults > dbresults.json

```

The **dbresults.json** file from above command will look like this (``null``/``0`` value fields removed for clarity):

```bash
{
  "databaseConnectionMap" : {
    "<UUID to identify entry in databaseConnectionMap>" : {
      "password" : "<NDEx account password>",
      "user" : "<NDEx account username>",
      "server" : "<NDEx server ie ndexbio.org>",
      "networkSetId" : "<NDEx networkset UUID ie f884cd40-5426-49e6-a311-fc046802b5f6>"
    },
    "e508cf31-79af-463e-b8b6-ff34c87e1734" : {
      "password" : "somepassword",
      "user" : "bob",
      "server" : "dev.ndexbio.org",
      "networkSetId" : "bf0616dd-5d7e-403a-92f3-6e12cc02eb37"
    }
  },
  "networksToExclude" : [ "<UUID of network in NDEx, if here network will be excluded>", "4671adc9-670d-474c-84db-37774fc885ba" ],
  "results" : [ {
    "name" : "<Name of source/database for these networks>",
    "description" : "<Brief descriptiopn of this source/database of networks>",
    "imageURL" : "<URL to png or svg to use as image icon for networks>",
    "uuid" : "<UUID to identify entry in databaseConnectionMap>",
  }, {
    "name" : "ncipid",
    "description" : "This is a description of a ncipid database",
    "imageURL" : "http://www.home.ndexbio.org/img/pid-logo-ndex.jpg",
    "uuid" : "e508cf31-79af-463e-b8b6-ff34c87e1734",
  } ]
}

```

Description of **dbresults.json**

* ``databaseConnectionMap: {...}``

   Contains a map of [NDEx][ndex] credentials. This map is referenced by the 
   ``uuid`` field under ``results``
   
   * ``password`` - [NDEx][ndex] account password
   * ``username`` - [NDEx][ndex] account username
   * ``server`` - [NDEx][ndex] server to use ie ``ndexbio.org``
   * ``networkSetId`` - UUID of networkset on [NDEx][ndex] server. This networkset
                        should contain the networks for a given database/source
                        
* ``networksToExclude``

  List of zero or more of networks in [NDEx][ndex] to exclude. The value set should
  be an [NDEx][ndex] UUID for the network.
  
* ``results [...]``

  Contains a list of data sources or databases of networks.
  
  * ``name`` - Name of network database/source
  * ``description`` - Description of network database/source
  * ``imageURL`` - URL to png/svg image used as icon in UI for networks if `__iconurl` is
     **NOT** set on the given network
  * ``uuid`` - ID of credentials in ``databaseConnectionMap`` which has a ``networkSetId`` field
               that denotes contains the networks for this network database/source


 ### Step 3 create database

To create the database run the following command:
 
 ```bash
java -jar enrichment.jar --mode createdb --conf enrichment.conf --dbresults dbresults.json
```

The above command will read the configuration and **dbresults.json** file
and query [NDEx][ndex] for networks downloading them to folders matching **uuid**
values under the **enrichment.database.dir** directory.

### Step 4 Run the service

```bash
jav -jar enrichment.jar --mode runserver --conf enrichment.conf
```

More Information
=================

[Visit our wiki for more information](https://github.com/cytoscape/ndex-enrichment-rest/wiki)

COPYRIGHT AND LICENSE
=====================

TODO

Acknowledgements
================

TODO
