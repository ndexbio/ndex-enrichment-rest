
[jetty]: http://eclipse.org/jetty/
[maven]: http://maven.apache.org/
[java]: https://www.oracle.com/java/index.html
[git]: https://git-scm.com/

[make]: https://www.gnu.org/software/make

NDEx Enrichment REST Service
============================

[![Build Status](https://travis-ci.org/ndexbio/ndex-enrichment-rest.svg?branch=master)](https://travis-ci.org/ndexbio/ndex-enrichment-rest) 
[![Coverage Status](https://coveralls.io/repos/github/ndexbio/ndex-enrichment-rest/badge.svg)](https://coveralls.io/github/ndexbio/ndex-enrichment-rest)

Provides enrichment REST service using select networks from NDEx as a backend.
This service runs using an embedded [Jetty][jetty] server and is invoked
from the command line. 


Requirements
============

* Centos 6+, Ubuntu 12+, and most other linux distributions should work
* [Java][java] 8+ **(jdk to build)**
* [Make][make] **(to build)**
* [Maven][maven] 3.3 or higher **(to build)** -- tested with 3.6

Special software to install (cause we haven't put these into maven central)

* [ndex-enrichment-rest-model](https://github.com/ndexbio/ndex-enrichment-rest-model) built and installed via `mvn install`
* [ndex-object-model](https://github.com/ndexbio/ndex-object-model) built and installed via `mvn install`
* [ndex-java-client](https://github.com/ndexbio/ndex-java-client) built and installed via `mvn install`


Building NDEx Enrichment REST Service
=====================================

Commands build NDEx Enrichment REST Service assuming machine has [Git][git] command line tools 
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


Running Enrichment REST Service
===============================

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

```

Replace **/tmp** paths with full path location to **enrichdb** directory 
created earlier.

### Step 2 Create databaseresults.json file

This file (which resides under **enrichment.task.dir**) contains
information about networks on NDEx to use to generate the Enrichment
database.

Run the following to create an example **databaseresults.json** file:

```bash
java -jar enrichment.jar --mode exampledbresults > db/databaseresults.json

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
  "networkToGeneToNodeMap" : null,
  "results" : [ {
    "description" : "This is a description of a signor database",
    "uuid" : "89a90a24-2fa8-4a57-ae4b-7c30a180e8e6",
    "url" : null,
    "networks" : null,
    "imageURL" : "http://signor.uniroma2.it/img/signor_logo.png",
    "numberOfNetworks" : null,
    "name" : "signor"
  }, {
    "description" : "This is a description of a ncipid database",
    "uuid" : "e508cf31-79af-463e-b8b6-ff34c87e1734",
    "url" : null,
    "networks" : null,
    "imageURL" : "http://www.home.ndexbio.org/img/pid-logo-ndex.jpg",
    "numberOfNetworks" : null,
    "name" : "ncipid"
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
java -jar enrichment.jar --mode createdb --conf enrichment.conf 
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

[Visit our wiki for more information](https://github.com/ndexbio/ndex-enrichment-rest/wiki)

COPYRIGHT AND LICENSE
=====================

TODO

Acknowledgements
================

TODO
