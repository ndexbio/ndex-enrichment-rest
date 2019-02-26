
[jetty]: http://eclipse.org/jetty/
[maven]: http://maven.apache.org/
[java]: https://www.oracle.com/java/index.html
[git]: https://git-scm.com/

[make]: https://www.gnu.org/software/make

NDEx Enrichment REST Service
============================

TODO



Requirements
============

* Centos 6+, Ubuntu 12+, and most other linux distributions should work
* [Java][java] 8+ **(jdk to build)**


Usage 
=====




Building NDEx Enrichment REST Service  
=====================================

NDEx Enrichment REST Service build requirements:

* [Java 8+][java] JDK
* [Make][make] **(to build)**
* [Maven][maven] 3.0 or higher **(to build)**


Commands build NDEx Enrichment REST Service assuming machine has [Git][git] command line tools 
installed:

```Bash
# In lieu of git one can just download repo and unzip it
git clone https://github.com/coleslaw481/ndex-enrichment-rest.git

cd ndex-enrichment-rest
mvn clean test install

# to try out locally run
mvn jetty:run

# or to directly run the war
make runwar

# then visit http://localhost:8080/enrichment/status on your browser
```

The above command will create a jar file under **target/** named 
**ndex-enrichment-rest.war** as well as a command line application 
named **ndex-enrichment-rest-\<VERSION\>-jar-with-dependencies.jar**




COPYRIGHT AND LICENSE
=====================

TODO

Acknowledgements
================

TODO
