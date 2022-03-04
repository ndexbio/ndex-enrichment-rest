
[jetty]: http://eclipse.org/jetty/
[maven]: http://maven.apache.org/
[java]: https://www.oracle.com/java/index.html
[git]: https://git-scm.com/
[ndex]: https://ndexbio.org
[make]: https://www.gnu.org/software/make

[NDEx][ndex] Pathway Relevance REST Service
=====================================

[![Build Status](https://travis-ci.org/cytoscape/ndex-enrichment-rest.svg?branch=master)](https://travis-ci.org/cytoscape/ndex-enrichment-rest) 
[![Coverage Status](https://coveralls.io/repos/github/cytoscape/ndex-enrichment-rest/badge.svg)](https://coveralls.io/github/cytoscape/ndex-enrichment-rest)

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

### Step 2 Create databaseresults.json file

This file contains
information about networks on NDEx to use to generate the Enrichment
database.

Run the following to create an example **databaseresults.json** file:

```bash
java -jar enrichment.jar --mode exampledbresults > databaseresults.json

```

The **databaseresults.json** file will look like this:

```bash
{
  "databaseConnectionMap" : {
    "89a90a24-2fa8-4a57-ae4b-7c30a180e8e6" : {
      "password" : "somepassword",
      "user" : "bob",
      "server" : "dev.ndexbio.org",
      "networkSetId" : "f884cd40-5426-49e6-a311-fc046802b5f6"
    },
    "e508cf31-79af-463e-b8b6-ff34c87e1734" : {
      "password" : "somepassword",
      "user" : "bob",
      "server" : "dev.ndexbio.org",
      "networkSetId" : "bf0616dd-5d7e-403a-92f3-6e12cc02eb37"
    }
  },
  "networksToExclude" : [ "4671adc9-670d-474c-84db-37774fc885ba", "309e834a-3005-41f2-8d28-46f2594aaaa8" ],
  "universeUniqueGeneCount" : 0,
  "databaseUniqueGeneCount" : null,
  "geneMapList" : null,
  "idfMap" : null,
  "totalNetworkCount" : 0,
  "networkGeneList" : null,
  "networkToGeneToNodeMap" : null,
  "results" : [ {
    "numberOfNetworks" : null,
    "name" : "signor",
    "description" : "This is a description of a signor database",
    "uuid" : "89a90a24-2fa8-4a57-ae4b-7c30a180e8e6",
    "url" : null,
    "networks" : [ {
      "name" : "Network Name",
      "description" : "Network description",
      "uuid" : "640e2cef-795d-11e8-a4bf-0ac135e8bacf",
      "url" : "http://www.ndexbio.org/#/network/640e2cef-795d-11e8-a4bf-0ac135e8bacf",
      "imageUrl" : "http://www.home.ndexbio.org/img/pid-logo-ndex.jpg",
      "geneCount" : 0,
      "nodeCount" : 0,
      "edgeCount" : 0
    } ],
    "imageURL" : "http://signor.uniroma2.it/img/signor_logo.png"
  }, {
    "numberOfNetworks" : null,
    "name" : "ncipid",
    "description" : "This is a description of a ncipid database",
    "uuid" : "e508cf31-79af-463e-b8b6-ff34c87e1734",
    "url" : null,
    "networks" : [ {
      "name" : "Network Name",
      "description" : "Network description",
      "uuid" : "640e2cef-795d-11e8-a4bf-0ac135e8bacf",
      "url" : "http://www.ndexbio.org/#/network/640e2cef-795d-11e8-a4bf-0ac135e8bacf",
      "imageUrl" : "http://www.home.ndexbio.org/img/pid-logo-ndex.jpg",
      "geneCount" : 0,
      "nodeCount" : 0,
      "edgeCount" : 0
    } ],
    "imageURL" : "http://www.home.ndexbio.org/img/pid-logo-ndex.jpg"
  } ]
}

```

The **databaseConnectionMap** section is internal and contains NDEx connection information
that needs to be updated.The networks for the database will be all networks
under network set specified by value of **networkSetId**

The **results** section is what will be returned to caller on service. During
database creation the **numberOfNetworks**, **networks**, **url** will be updated, but its
up to you to set **name** and **description** and to pick a **uuid** that matches
the values under **databaseConnectionMap**

 ### Step 3 create database

To create the database run the following command:
 
 ```bash
java -jar enrichment.jar --mode createdb --conf enrichment.conf --dbresults databaseresults.json
```

The above command will read the configuration and **databaseresults.json** 
and query NDEx for networks downloading them to folders matching **uuid**
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
