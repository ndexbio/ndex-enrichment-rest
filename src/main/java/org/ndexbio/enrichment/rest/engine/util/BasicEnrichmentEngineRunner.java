package org.ndexbio.enrichment.rest.engine.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
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
import org.ndexbio.cxio.aspects.datamodels.NodeAttributesElement;
import org.ndexbio.cxio.aspects.datamodels.NodesElement;
import org.ndexbio.enrichment.rest.model.DatabaseResult;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.enrichment.rest.model.comparators.EnrichmentQueryResultByPvalue;
import org.ndexbio.model.cx.NiceCXNetwork;
import org.ndexbio.rest.client.NdexRestClientUtilities;
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
	private EnrichmentQueryResultByPvalue _pvalueComparator;
	
	public BasicEnrichmentEngineRunner(final String id,
			final String taskDir,
			final String dbDir,
			final AtomicReference<InternalDatabaseResults> databaseResults,
			final ConcurrentHashMap<String, ConcurrentHashMap<String, HashSet<String>>> databases,
			final EnrichmentQuery eq,
			EnrichmentQueryResults eqr) {
		_id = id;
		_taskDir = taskDir;
		_dbDir = dbDir;
		_databaseResults = databaseResults;
		_databases = databases;
		_eq = eq;
		_eqr = eqr;
		_pvalueComparator = new EnrichmentQueryResultByPvalue();
	}
	@Override
	public EnrichmentQueryResults call() throws Exception {
		processQuery(_id, _eq, _eqr);
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
			//updateEnrichmentQueryResultsInDb(id, EnrichmentQueryResults.FAILED_STATUS, 100, null);
			updateEnrichmentQueryResults(EnrichmentQueryResults.FAILED_STATUS, 100, null);
			return;
		}

		//check gene list
		List<EnrichmentQueryResult> enrichmentResult = new LinkedList<EnrichmentQueryResult>();
		for (String databaseName : query.getDatabaseList()){
			DatabaseResult dbres = getDatabaseResultFromDb(databaseName);

			if (dbres == null){
				_logger.error("No database matching: " + databaseName + " found. Skipping");
				continue;
			}
			_logger.debug("Querying database: " + databaseName);
			SortedSet<String> uniqueGeneList = query.getGeneList();
			HashMap<String, HashSet<String>> networkMap = remapNetworksToGenes(dbres.getUuid(), uniqueGeneList);
			if (networkMap == null){
				continue;
			}
			// generate EnrichmentQueryResult from networkMap
			enrichmentResult.addAll(getEnrichmentQueryResultObjectsFromNetworkMap(taskDir, dbres, networkMap, uniqueGeneList));
		}
		sortEnrichmentQueryResultByPvalueAndRank(enrichmentResult);

		// combine all EnrichmentQueryResults generated above and create
		// EnrichmentQueryResults object and store in _queryResults 
		// replacing any existing entry
		//updateEnrichmentQueryResultsInDb(id, EnrichmentQueryResults.COMPLETE_STATUS, 100, enrichmentResult);
		updateEnrichmentQueryResults(EnrichmentQueryResults.COMPLETE_STATUS, 100, enrichmentResult);
		//saveEnrichmentQueryResultsToFilesystem(id);
	}
	
	protected void updateEnrichmentQueryResults(final String status, int progress,
			List<EnrichmentQueryResult> result) {
		_eqr.setProgress(progress);
		_eqr.setStatus(status);
		if (result != null) {
			_eqr.setNumberOfHits(result.size());
			_eqr.setResults(result);
		}
		_eqr.setWallTime(System.currentTimeMillis() - _eqr.getStartTime());
	}
	
	/*
	protected void updateEnrichmentQueryResultsInDb(final String id,
			final String status, int progress,
			List<EnrichmentQueryResult> result){
		EnrichmentQueryResults eqr = getEnrichmentQueryResultsFromDb(id);

		eqr.setProgress(progress);
		eqr.setStatus(status);
		if (result != null){
			eqr.setNumberOfHits(result.size());
			eqr.setResults(result);
		}
		eqr.setWallTime(System.currentTimeMillis() - eqr.getStartTime());
		_queryResults.merge(id, eqr, (oldval, newval) -> newval.updateStartTime(oldval));  
	}*/
	
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
	protected HashMap<String, HashSet<String>> remapNetworksToGenes(final String databaseId, final SortedSet<String> uniqueGeneList){
		HashMap<String, HashSet<String>> networkMap = new HashMap<String, HashSet<String>>();

		ConcurrentHashMap<String, HashSet<String>> dbMap = _databases.get(databaseId);
		if (dbMap == null){
			_logger.debug("No database with id: " + databaseId + " found. Skipping");
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
					geneSet = new HashSet<String>();
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
		int numGenesInQuery = uniqueGeneList.size();
		List<EnrichmentQueryResult> eqrList = new LinkedList<EnrichmentQueryResult>();
		for (String network : networkMap.keySet()){
			EnrichmentQueryResult eqr = new EnrichmentQueryResult();
			eqr.setDatabaseName(dbres.getName());
			eqr.setImageURL(dbres.getImageURL());
			eqr.setDatabaseUUID(dbres.getUuid());
			TreeSet<String> hitGenes = new TreeSet<>(networkMap.get(network));
			eqr.setHitGenes(hitGenes);
			eqr.setNetworkUUID(network);
			eqr.setUrl(getNetworkUrl(dbres.getUrl(), network));
			NiceCXNetwork cxNetwork = getNetwork(dbres.getUuid(), network);
			if (cxNetwork == null){
				_logger.error("Unable to get network: " + network + " skipping...");
				continue;
			}

			updateStatsAboutNetwork(cxNetwork, eqr, uniqueGeneList);
			//File destFile = new File(taskDir.getAbsolutePath() + File.separator + network + ".cx");
			//annotateAndSaveNetwork(destFile, cxNetwork, eqr);
			eqrList.add(eqr);
		}
		return eqrList;
	}
	
	protected String getNetworkUrl(String databaseUrl, String networkUuid) {
		int index = databaseUrl.indexOf("networkset");
		if (index != -1) {
			return databaseUrl.substring(0, index) + "network/" + networkUuid;
		}
		return null;
	}
	
	protected NiceCXNetwork getNetwork(final String databaseUUID, final String networkUUID){
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(new File(this._dbDir + File.separator + databaseUUID + File.separator + networkUUID + ".cx"));
			return NdexRestClientUtilities.getCXNetworkFromStream(fis);
		}
		catch(IOException ex){
			_logger.error("error reading ", ex);
		}
		finally {
			try{
				fis.close();
			}
			catch(IOException io){
				_logger.error("unable to close stream", io);
			}
		}
		return null;
	}
	
	/**
	 * Updates <b>eqr</b> with pvalue and other stats
	 * @param cxNetwork {@link org.ndexbio.model.cx.NiceCXNetwork} to extract node and edge count from
	 * @param eqr {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResult} to update
	 * @param numGenesInQuery 
	 */
	protected void updateStatsAboutNetwork(NiceCXNetwork cxNetwork, EnrichmentQueryResult eqr,
			SortedSet<String> queryGenes){
		eqr.setNodes(cxNetwork.getNodes().size());
		eqr.setEdges(cxNetwork.getEdges().size());
		eqr.setDescription(cxNetwork.getNetworkName());
		int numHitGenes = eqr.getHitGenes().size();
		int numGenesInQuery = queryGenes.size();
		eqr.setPercentOverlap(Math.round(((float)numHitGenes/(float)numGenesInQuery)*(float)100));
		InternalDatabaseResults idr = (InternalDatabaseResults)this._databaseResults.get();
		int totalGenesInUniverse = idr.getUniverseUniqueGeneCount();
		eqr.setpValue(getPvalue(totalGenesInUniverse, eqr.getNodes(), numGenesInQuery, numHitGenes));
		eqr.setTotalNetworkCount(idr.getTotalNetworkCount());

		Collection<NodesElement> networkNodes = new LinkedList<>();
		for (NodesElement node : cxNetwork.getNodes().values()) {
			if (node.getNodeName() != null) {
				networkNodes.add(node);
			}
		}
		eqr.setSimilarity(getSimilarity(networkNodes, cxNetwork.getNodeAttributes(), queryGenes, idr.getNetworkToGeneToNodeMap().get(eqr.getNetworkUUID()), idr.getIdfMap()));
	}
	
	/**
	 * @param totalGenesInNetwork
	 * @param numberGenesInQuery
	 * @param numGenesMatch
	 * @return 
	 */
	protected double getPvalue(int totalGenesInUniverse, int totalGenesInNetwork, int numberGenesInQuery, int numGenesMatch){
		HypergeometricDistribution hd = new HypergeometricDistribution(totalGenesInUniverse, 
				totalGenesInNetwork, numberGenesInQuery);
		double pValue = ((double)1.0 - hd.cumulativeProbability(numGenesMatch));
		if (pValue < 0) {
			return 0.0;
		}
		return pValue;
	}
	
	protected double getSimilarity(
			Collection<NodesElement> networkNodes, Map<Long, Collection<NodeAttributesElement>> nodeAttributes, 
			SortedSet<String> queryGenes,
			Map<String, Set<Long>> nodeMap, Map<String, Double> idfMap
			) {
		Set<String> networkGenes = new TreeSet<>();

		for (NodesElement ne : networkNodes) {
			Collection<NodeAttributesElement> nodeAttribs = nodeAttributes.get(ne.getId());
			if (nodeAttribs == null){
				continue;
			}

			boolean validComplex = false;
			for (NodeAttributesElement nae : nodeAttribs){
				if (nae.getName().toLowerCase().equals("type")){
					if (nae.getValue().toLowerCase().equals("complex") ||
							nae.getValue().toLowerCase().equals("proteinfamily") ||
							nae.getValue().toLowerCase().equals("compartment")) {
						validComplex = true;
					}
				}
			}
			if (validComplex) {
				for (NodeAttributesElement nae : nodeAttribs){
					if (nae.getName().toLowerCase().equals("member")){
						for (String entry : nae.getValues()){
							String validGene = getValidGene(entry);
							if (validGene != null) {
								networkGenes.add(validGene);
							}
						}
					}
				}
			} else {
				String validGene = getValidGene(ne.getNodeName());
				if (validGene != null) {
					networkGenes.add(validGene);
				}
			}
		}

		int size = networkGenes.size() + queryGenes.size();

		GVector networkVector = new GVector(size);
		GVector queryVector = new GVector(size);
		int index = 0;
		for (String gene : networkGenes) {
			if (idfMap.containsKey(gene)) {
				networkVector.setElement(index, idfMap.get(gene));
				if (queryGenes.contains(gene)) {
					queryVector.setElement(index, idfMap.get(gene));
				}
				index++;
			}
		}
		for (String gene : queryGenes) {
			if (!networkGenes.contains(gene) && idfMap.containsKey(gene)) {
				queryVector.setElement(index, idfMap.get(gene));
				index++;
			}
		}
		return getCosineSimilarity(networkVector, queryVector);
	}
	
	protected String getValidGene(final String potentialGene){
		if (potentialGene == null){
			return null;
		}
		String strippedGene = potentialGene;
		// strip off hgnc.symbol: prefix if found
		if (potentialGene.startsWith("hgnc.symbol:") && potentialGene.length()>12){
			strippedGene = potentialGene.substring(potentialGene.indexOf(":") + 1);
		}

		if (strippedGene.length()>30 || !strippedGene.matches("(^[A-Z][A-Z0-9-]*$)|(^C[0-9]+orf[0-9]+$)")) {
			return null;
		}

		return strippedGene;
	}

	protected double getCosineSimilarity(GVector vec1, GVector vec2) {
		return (vec1.dot(vec2)) / (vec1.norm() * vec2.norm());
	}
	
	/**
	 * Sorts in place list of {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResult}
	 * by pvalue and sets the rank starting from 0 for each object based on sorting
	 * with lowest pvalue having highest rank which in this case is 0.
	 * @param eqrList list of {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResult} objects to sort by 
	 *                        {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResult#getpValue()}
	 */
	protected void sortEnrichmentQueryResultByPvalueAndRank(List<EnrichmentQueryResult> eqrList){
		Collections.sort(eqrList, _pvalueComparator);
		int rank = 0;
		for(EnrichmentQueryResult eqr : eqrList){
			eqr.setRank(rank++);
		}
	}
}
