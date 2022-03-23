package org.ndexbio.enrichment.rest.engine.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.vecmath.GVector;

import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.ndexbio.enrichment.rest.engine.BasicEnrichmentEngineImpl;
import org.ndexbio.ndexsearch.rest.model.DatabaseResult;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.ndexsearch.rest.model.NetworkInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicEnrichmentEngineRunner implements Callable {
	
	static Logger _logger = LoggerFactory.getLogger(BasicEnrichmentEngineRunner.class);
	
	private String _id;
	private EnrichmentQuery _eq;
	private EnrichmentQueryResults _eqr;
	private String _taskDir;
	private String _dbDir;
	private AtomicReference<InternalDatabaseResults> _databaseResults;
	private ConcurrentHashMap<String, ConcurrentHashMap<String, HashSet<String>>> _databases;
	private HashSet<String> _uniqueGenesInUniverse;
	private Comparator<EnrichmentQueryResult> _comparator;
	private int _numResultsToReturn;
	
	/**
	 * Constructor that receives all needed data/objects to run query when call()
	 * method is invoked on this. 
	 * @param id ID of task
	 * @param taskDir directory where tasks reside
	 * @param dbDir database directory
	 * @param databaseResults 
	 * @param databases
	 * @param uniqueGenesInUniverse  Set of genes in the universe (not used right now)
	 * @param comparator Used to sort the results and set rank
	 * @param numResultsToReturn Limit results returned to this
	 * @param eq Query to run
	 * @param eqr Where results from query will be stored
	 */
	public BasicEnrichmentEngineRunner(final String id,
			final String taskDir,
			final String dbDir,
			final AtomicReference<InternalDatabaseResults> databaseResults,
			final ConcurrentHashMap<String, ConcurrentHashMap<String, HashSet<String>>> databases,
			final HashSet<String> uniqueGenesInUniverse,
			final Comparator<EnrichmentQueryResult> comparator,
			final int numResultsToReturn,
			final EnrichmentQuery eq,
			EnrichmentQueryResults eqr) {
		_id = id;
		_taskDir = taskDir;
		_dbDir = dbDir;
		_databaseResults = databaseResults;
		_databases = databases;
		_uniqueGenesInUniverse = uniqueGenesInUniverse;
		_numResultsToReturn = numResultsToReturn;
		_eq = eq;
		_eqr = eqr;
		_comparator = comparator;
	}
	@Override
	public EnrichmentQueryResults call() throws Exception {
		long startTime = System.currentTimeMillis();
		processQuery(_id, _eq, _eqr);
		_logger.info("Processing of {} took {} ms", _id, System.currentTimeMillis() - startTime);
		return _eqr;
	}
	
	/**
	 * Runs enrichment on query
	 * @param id 
	 */
	protected void processQuery(final String id, EnrichmentQuery query, EnrichmentQueryResults eqr){
		File taskDir = new File(this._taskDir + File.separator + id.toString());
		_logger.debug("Creating new task directory:" + taskDir.getAbsolutePath());

		if (taskDir.mkdirs() == false){
			_logger.error("Unable to create task directory: " + taskDir.getAbsolutePath());
			updateEnrichmentQueryResults(EnrichmentQueryResults.FAILED_STATUS, 100, null);
			return;
		}		
                
                // write the query to the filesystem
                saveEnrichmentQueryToFilesystem(query, taskDir.getAbsolutePath() +
                        File.separator + BasicEnrichmentEngineImpl.QUERY_JSON_FILE);
		
		//check gene list
		List<EnrichmentQueryResult> enrichmentResult = new LinkedList<>();
		// @TODO enable filtering of query genes by genes in universe which is not being done
		//       cause _uniqueGenesInUniverse has not been populated. 
		SortedSet<String> uniqueGeneList = query.getGeneList(); // getUniqueQueryGeneSetFilteredByUniverseGenes(query.getGeneList());
		if (query.getDatabaseList() != null){
			for (String databaseName : query.getDatabaseList()){
				long startTime = System.currentTimeMillis();
				DatabaseResult dbres = getDatabaseResultFromDb(databaseName);

				if (dbres == null){
					_logger.error("No database matching: " + databaseName + " found. Skipping");
					continue;
				}
				_logger.debug("Querying database: " + databaseName);

				HashMap<String, HashSet<String>> networkMap = remapNetworksToGenes(dbres.getUuid(), uniqueGeneList);
				if (networkMap == null){
					continue;
				}

				// generate EnrichmentQueryResult from networkMap
				long addAllStart = System.currentTimeMillis();
				enrichmentResult.addAll(getEnrichmentQueryResultObjectsFromNetworkMap(taskDir, dbres, networkMap, uniqueGeneList));
				_logger.info("For task {} and {} with {} networks took {} ms to enrichmentResult.addAll(getEnrichmentQueryResultObjectsFromNetworkMap(...",
						new Object[]{id, databaseName, networkMap.size(), System.currentTimeMillis() - addAllStart});
				_logger.info("For task {} and {} took {} ms to process",
						new Object[]{id, databaseName, System.currentTimeMillis() - startTime});
			}
		} else {
			_logger.warn("No databases in query {} ", id);
		}
		long sortStart = System.currentTimeMillis();
		sortEnrichmentQueryResultAndSetRank(enrichmentResult);
		_logger.info("For task {} to sort results took {} ms" ,id , System.currentTimeMillis() - sortStart);
		

		// combine all EnrichmentQueryResults generated above and create
		// EnrichmentQueryResults object and store in _queryResults 
		// replacing any existing entry
		updateEnrichmentQueryResults(EnrichmentQueryResults.COMPLETE_STATUS, 100, enrichmentResult);
	}
	
        protected void saveEnrichmentQueryToFilesystem(final EnrichmentQuery query, final String destPath){
		if (query == null){
			return;
		}
		File destFile = new File(destPath);
		ObjectMapper mappy = new ObjectMapper();
		try (FileOutputStream out = new FileOutputStream(destFile)){
			mappy.writeValue(out, query);
		} catch(IOException io){
			_logger.error("Caught exception writing " + destFile.getAbsolutePath(), io);
		}
	}
        
	/**
	 * Filters {@code queryGeneList} by known genes in universe
	 * @param queryGeneList query genes
	 * @return sorted set of filtered genes
	 */
	protected SortedSet<String> getUniqueQueryGeneSetFilteredByUniverseGenes(SortedSet<String> queryGeneList){
		SortedSet<String> filteredUniqueGenes = new TreeSet<>();
		if (queryGeneList == null){
			return filteredUniqueGenes;
		}
		for(String gene : queryGeneList){
			if (_uniqueGenesInUniverse.contains(gene)){
				filteredUniqueGenes.add(gene);
			}
		}
		return filteredUniqueGenes;
	}
	
	/**
	 * Updates EnrichmentQueryResults passed into constructor with status, progress
	 * and results passed into this method. Previous data in EnrichmentQueryResults
	 * is replaced. If the number of results in result list exceeds number of results
	 * to return set via the constructor only the number of results will be stored
	 * in the EnrichmentQueryResults object
	 * 
	 * @param status New status
	 * @param progress New progress
	 * @param result New list of results, already sorted with rank set. 
	 */
	protected void updateEnrichmentQueryResults(final String status, int progress,
			List<EnrichmentQueryResult> result) {
		_eqr.setProgress(progress);
		_eqr.setStatus(status);
		List<EnrichmentQueryResult> subResult;
		if (result != null) {
			_logger.info("Result size: {} ", result.size());
			if (result.size() > _numResultsToReturn){
				_logger.info("Result has {} hits keeping only best {}",
						result.size(), _numResultsToReturn);
				subResult = result.subList(0, _numResultsToReturn);
			} else {
				subResult = result;
			}
			_eqr.setNumberOfHits(subResult.size());
			_eqr.setResults(subResult);
		}
		_eqr.setWallTime(System.currentTimeMillis() - _eqr.getStartTime());
	}
	
	/**
	 * Examines databaseResults passed invia constructor for first DatabaseResult
	 * that matches (ignore case) dbName passed into this method. 
	 * @param dbName
	 * @return DatabaseResult matching dbName or null if none found.
	 */
	protected DatabaseResult getDatabaseResultFromDb(final String dbName){
		String dbNameLower = dbName.toLowerCase();
		for (DatabaseResult res : _databaseResults.get().getResults()){
			if (res.getName().toLowerCase().equals(dbNameLower)){
				return res;
			}
		}
		return null;
	}
	
	/**
	 * Uses {@link #_databases} data structure to build a HashMap from database
	 * with {@code databaseId} id where the
	 * key is the id of the network and value is a set of genes that were
	 * found on that network
	 * @param databaseId id of database
	 * @param uniqueGeneList list of genes that are assumed to be unique and upper cased. 
	 *                 Ideally generated from call to {@link #getUniqueGeneList(java.util.List)}
	 * @return {@link java.util.HashMap} where key is network id and value is a {@link java.util.HashSet<String>} of gene symbols
	 */
	protected HashMap<String, HashSet<String>> remapNetworksToGenes(final String databaseId,
			final SortedSet<String> uniqueGeneList){
		HashMap<String, HashSet<String>> networkMap = new HashMap<>();

		ConcurrentHashMap<String, HashSet<String>> dbMap = _databases.get(databaseId);
		if (dbMap == null){
			_logger.debug("No database with id: {} found. Skipping", databaseId);
			return null;
		}
		for (String gene : uniqueGeneList){
			if (dbMap.containsKey(gene) == false){
				continue;
			}
			HashSet<String> networkSet = dbMap.get(gene);
			for (String network : networkSet){
				HashSet<String> geneSet = networkMap.get(network);
				if (geneSet == null){
					geneSet = new HashSet<>();
					networkMap.put(network, geneSet);
				}
				geneSet.add(gene);
			}    
		}
		return networkMap;
	}
	
	/**
	 * Generates EnrichmentQueryResult objects from data passed in
	 * @param dbres Database associated with this result
	 * @param networkMap Map where key is networkUUID and value is list of genes
	 *                   that were found to match the query on that network
	 * @param uniqueGeneList Unique list of genes
	 * @return List of EnrichmentQueryResult objects 
	 */
	protected List<EnrichmentQueryResult> getEnrichmentQueryResultObjectsFromNetworkMap(File taskDir, DatabaseResult dbres,
			HashMap<String, HashSet<String>> networkMap, SortedSet<String> uniqueGeneList){
		if (dbres == null){
			_logger.error("DatabaseResult is null");
			return null;
		}

		if (networkMap == null){
			_logger.error("Network map is null");
			return null;
		}

		if (uniqueGeneList == null){
			_logger.error("Unique Gene List is null");
			return null;
		}
		NetworkInfo networkInfo = null;
		List<EnrichmentQueryResult> eqrList = new LinkedList<>();
		for (String network : networkMap.keySet()){
			EnrichmentQueryResult eqr = new EnrichmentQueryResult();
			eqr.setDatabaseName(dbres.getName());
			
			eqr.setImageURL(dbres.getImageURL());
			eqr.setDatabaseUUID(dbres.getUuid());
			TreeSet<String> hitGenes = new TreeSet<>(networkMap.get(network));
			eqr.setHitGenes(hitGenes);
			eqr.setNetworkUUID(network);
			// sets url but is doing the old viewer
			eqr.setUrl(getNetworkUrl(dbres.getUrl(), network));

			networkInfo = null;
			for (NetworkInfo ni : dbres.getNetworks()){
				if (ni.getUuid().equals(network)){
					networkInfo = ni;
					// gets url for network from configuration file
					eqr.setUrl(networkInfo.getUrl());
					
					// if the network has an overriding image url use it
					if (networkInfo.getImageUrl() != null
							&& networkInfo.getImageUrl().startsWith("http")){
						eqr.setImageURL(networkInfo.getImageUrl());
					}
                                        
				}
			}
			updateStatsAboutNetwork(eqr, uniqueGeneList, networkInfo);
			eqrList.add(eqr);
		}
		return eqrList;
	}
	
	/**
	 * Gets NDEx URL for a network by prefixing databaseURL + / + networkUuid
	 * and removing networkset from end of URL. If networkset is not in string
	 * then null is returned
	 * 
	 * @param databaseUrl
	 * @param networkUuid
	 * @return URL/network/NETWORKID or null if networkset is not in url
	 */
	protected String getNetworkUrl(String databaseUrl, String networkUuid) {
		int index = databaseUrl.indexOf("#/networkset");
		if (index != -1) {
			return databaseUrl.substring(0, index) + "viewer/networks/" + networkUuid;
		}
		_logger.warn("networkset not in URL: {} for network {}",
				databaseUrl, networkUuid);
		return null;
	}
	
	/**
	 * Updates <b>eqr</b> with pvalue and other stats
	 * @param cxNetwork {@link org.ndexbio.model.cx.NiceCXNetwork} to extract node and edge count from
	 * @param eqr {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResult} to update
	 * @param numGenesInQuery 
	 */
	protected void updateStatsAboutNetwork(EnrichmentQueryResult eqr,
			SortedSet<String> queryGenes, NetworkInfo networkInfo){
		eqr.setNodes(networkInfo.getNodeCount());
		eqr.setEdges(networkInfo.getEdgeCount());
		eqr.setDescription(networkInfo.getName());
		int numHitGenes = eqr.getHitGenes().size();
		int numGenesInQuery = queryGenes.size();
		eqr.setPercentOverlap(Math.round(((float)numHitGenes/(float)numGenesInQuery)*(float)100));
		InternalDatabaseResults idr = (InternalDatabaseResults)this._databaseResults.get();
		int totalGenesInUniverse = idr.getUniverseUniqueGeneCount();
		eqr.setpValue(getPvalue(totalGenesInUniverse, networkInfo.getGeneCount(), 
				numGenesInQuery, numHitGenes));
		eqr.setTotalNetworkCount(idr.getTotalNetworkCount());
		eqr.setSimilarity(getSimilarity(idr.getNetworkGeneList().get(eqr.getNetworkUUID()),
				queryGenes, idr.getIdfMap()));
                eqr.setTotalGeneCount(networkInfo.getGeneCount());
	}
	
	/**
	 * @param totalGenesInNetwork
	 * @param numberGenesInQuery
	 * @param numGenesMatch
	 * @return 
	 */
	protected double getPvalue(int totalGenesInUniverse,
			int totalGenesInNetwork, int numberGenesInQuery,
			int numGenesMatch){
		try {
		HypergeometricDistribution hd = new HypergeometricDistribution(totalGenesInUniverse, 
				totalGenesInNetwork, numberGenesInQuery);
		double pValue = ((double)1.0 - hd.cumulativeProbability(numGenesMatch));
		if (pValue < 0) {
			_logger.warn("Returning 0.0 cause we got a negative value from "
					+ "Hypergeometric distribution totalGenesInUniverse={} totalGenesInNetwork={}"
					+ " numberGenesInQuery={} numGenesMatch={}", 
					new Object[]{totalGenesInUniverse,
						totalGenesInNetwork,numberGenesInQuery, numGenesMatch});
			return 0.0;
		}
		return pValue;
		} catch(NotPositiveException npe){
			_logger.error("Total genes in network is negative", npe);
			
		} catch(NotStrictlyPositiveException nspe){
			_logger.error("Total genes in universe is negative", nspe);
		} catch(NumberIsTooLargeException ntle){
			_logger.error("Number of hits or query size greater then total genes in universe", ntle);
		}
		return Double.MAX_VALUE;
	}
	
	/**
	 * Builds two GVectors, one for the network and one for the
	 * query genes to use for cosine similarity calculation
	 * @param networkGenes
	 * @param queryGenes
	 * @param idfMap
	 * @return 
	 */
	protected double getSimilarity(Set<String> networkGenes,
			SortedSet<String> queryGenes,
			Map<String, Double> idfMap) {

		int size = networkGenes.size() + queryGenes.size();

		GVector networkVector = new GVector(size);
		GVector queryVector = new GVector(size);
		int index = 0;
		double geneVal;
		
		// iterate through all the networkgenes and the values of
		// the idfMap to the networkVector. If the network gene is
		// also in the queryGenes, add the idfMap value to the queryVector
		for (String gene : networkGenes) {
			if (idfMap.containsKey(gene)) {
				geneVal = idfMap.get(gene);
				networkVector.setElement(index, geneVal);
				if (queryGenes.contains(gene)) {
					queryVector.setElement(index, geneVal);
				}
				index++;
			}
		}
		// iterate through the query genes and add any idfMap value of any
		// query genes that are not in the network genes and in the idfMap
		for (String gene : queryGenes) {
			if (!networkGenes.contains(gene) && idfMap.containsKey(gene)) {
				queryVector.setElement(index, idfMap.get(gene));
				index++;
			}
		}
		return getCosineSimilarity(networkVector, queryVector);
	}

	/**
	 * Calculates cosine similarity by taking the dot product of the
	 * two vectors passed in divided by the norm of vec1 x vec2
	 * @param vec1
	 * @param vec2
	 * @return 
	 */
	protected double getCosineSimilarity(GVector vec1, GVector vec2) {
		return (vec1.dot(vec2)) / (vec1.norm() * vec2.norm());
	}
	
	/**
	 * Sorts in place list of {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResult}
	 * by comparator passed in via constructor and sets the rank starting from 0 for 
	 * each object based on sorting
	 * with highest similarity having highest rank which in this case is 0.
	 * @param eqrList list of {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResult} objects to sort 
	 *        by Comparator
	 */
	protected void sortEnrichmentQueryResultAndSetRank(List<EnrichmentQueryResult> eqrList){
		Collections.sort(eqrList, _comparator);
		int rank = 0;
		for(EnrichmentQueryResult eqr : eqrList){
			eqr.setRank(rank++);
		}
	}
}
