HISTORY
========

0.8.0 (TBD)
-------------------

* To be consistent with other IQuery services, version bumped up to `0.8.0`

* Bumped jetty version to `9.4.45.v20220203`

* Bumped jackson-databind to `2.9.10.8`

* Migrated client and model objects from https://github.com/cytoscape/ndex-enrichment-rest-client
  and https://github.com/cytoscape/ndex-enrichment-rest-model back into this code base and
  removed the dependencies

* Added new configuration option `select.hit.genes` which if `true` selects the hit
  genes. By default this is `false`

* If `geneAnnotationServices` is set in query then this service will annotate
  nodes in networks with mutation frequencies

* Fixed bug where calling delete endpoint was NOT removing the entry from the
  cache causing inconsistencies on the get endpoints

* Status endpoint now reports number of elements in cache


0.7.0 (2020-12-22)
-------------------

* Added `sort.algorithm` and `number.returned.results` to service configuration that allows
  one to set the algorithm used to sort the results (default similarity) and the number of 
  results returned (default 25) respectively.

* Pathway relevance results limited to 25 by default.

* Default sorting is now similarity and is reflected in rank ordering values. 
  
* Improved performance by generating information needed for similarity calculation when
  index is being created/updated. UD-1555

0.6.7 (2020-11-24)
-------------------

* Updated Jetty to `9.4.32.v20200930` due to bugs in previous version that
  caused service to become unresponsive
