
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

# Sets directory where log files will be written for Jetty web server
runserver.log.dir = /tmp/logs

# Sets port Jetty web service will be run under
runserver.port = 8081

# sets Jetty Context Path for Enrichment
runserver.contextpath = /

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
  "geneMapList" : null,
  "databaseUniqueGeneCount" : null,
  "universeUniqueGeneCount" : 0,
  "databaseConnectionMap" : {
    "89a90a24-2fa8-4a57-ae4b-7c30a180e8e6" : {
      "password" : "somepassword",
      "user" : "bob",
      "server" : "dev.ndexbio.org",
      "networkSetId" : "d718366d-34e0-48cd-81bf-211a8b9a3fde"
    },
    "e508cf31-79af-463e-b8b6-ff34c87e1734" : {
      "password" : "somepassword",
      "user" : "bob",
      "server" : "dev.ndexbio.org",
      "networkSetId" : "e2ce01a3-5ce0-4d9e-be06-e20dad286d76"
    }
  },
  "results" : [ {
    "name" : "signor",
    "description" : "This is a description of a signor database",
    "numberOfNetworks" : "50",
    "uuid" : "89a90a24-2fa8-4a57-ae4b-7c30a180e8e6"
  }, {
    "name" : "ncipid",
    "description" : "This is a description of a ncipid database",
    "numberOfNetworks" : "200",
    "uuid" : "e508cf31-79af-463e-b8b6-ff34c87e1734"
  } ]
}
```

The **databaseConnectionMap** section is internal and contains NDEx connection information
that needs to be updated.The networks for the database will be all networks
under network set specified by value of **networkSetId**

The **results** section is what will be returned to caller on service. During
actual database creation the **numberOfNetworks** will be updated, but its
up to you to set name and description and to pick a **uuid** that matches
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

COPYRIGHT AND LICENSE
=====================

TODO

Acknowledgements
================

TODO
