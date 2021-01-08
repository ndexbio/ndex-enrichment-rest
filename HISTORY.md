HISTORY
========
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
