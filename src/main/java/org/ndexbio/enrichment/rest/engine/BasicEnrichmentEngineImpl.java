package org.ndexbio.enrichment.rest.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Arrays;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.vecmath.GVector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.ndexbio.cxio.aspects.datamodels.ATTRIBUTE_DATA_TYPE;
import org.ndexbio.cxio.aspects.datamodels.NodeAttributesElement;
import org.ndexbio.cxio.aspects.datamodels.NodesElement;
import org.ndexbio.cxio.core.NdexCXNetworkWriter;
import org.ndexbio.cxio.core.writers.NiceCXNetworkWriter;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.enrichment.rest.model.DatabaseResult;
import org.ndexbio.enrichment.rest.model.DatabaseResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryStatus;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.enrichment.rest.model.ServerStatus;
import org.ndexbio.enrichment.rest.model.comparators.EnrichmentQueryResultByPvalue;
import org.ndexbio.enrichment.rest.services.Configuration;
import org.ndexbio.enrichment.rest.services.EnrichmentHttpServletDispatcher;
import org.ndexbio.model.cx.NiceCXNetwork;
import org.ndexbio.model.exceptions.NdexException;

import org.ndexbio.rest.client.NdexRestClientUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 * Runs enrichment 
 * @author churas
 */
public class BasicEnrichmentEngineImpl implements EnrichmentEngine {

  public static final String EQR_JSON_FILE = "enrichmentqueryresults.json";
    
  static Logger _logger = LoggerFactory.getLogger(BasicEnrichmentEngineImpl.class);

  private String _dbDir;
  private String _taskDir;
  private boolean _shutdown;
  private EnrichmentQueryResultByPvalue _pvalueComparator;

  /**
   * This should be a map of <query String> => EnrichmentQuery object
   */  
  private ConcurrentLinkedQueue<EnrichmentQuery> _queryTasks;
  
  /**
   * This should be a map of <query String> => EnrichmentQueryResults object
   */
  private ConcurrentHashMap<String, EnrichmentQueryResults> _queryResults;
      
  /**
   * This should be a map of <database UUID> => Map<Gene => Set of network UUIDs>
   */
  private ConcurrentHashMap<String, ConcurrentHashMap<String, HashSet<String>>> _databases;
  
  private AtomicReference<InternalDatabaseResults> _databaseResults;
  
  private long _threadSleep = 10;
  
  //Cache
  private final LoadingCache<EnrichmentQuery, String> geneSetSearchCache;
  private static int resultCacheSize = 600;
  
  public BasicEnrichmentEngineImpl(final String dbDir,
    final String taskDir){
    _shutdown = false;
    _dbDir = dbDir;
    _taskDir = taskDir;
    _queryResults = new ConcurrentHashMap<>();
    _databaseResults = new AtomicReference<>();
    _queryTasks = new ConcurrentLinkedQueue<>();
    _pvalueComparator = new EnrichmentQueryResultByPvalue();

    RemovalListener<EnrichmentQuery, String> removalListener = new RemovalListener<EnrichmentQuery, String>() {
      @Override
      public void onRemoval(RemovalNotification<EnrichmentQuery, String> removal) {
        String id = removal.getValue();
        try {
			delete(id);
		} catch (EnrichmentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
      }
    };
        
    geneSetSearchCache = CacheBuilder
      .newBuilder()
      .initialCapacity(resultCacheSize)
      .maximumSize(resultCacheSize)
      .removalListener(removalListener)
      .build(
        new CacheLoader<EnrichmentQuery, String>() {
        	public String load(EnrichmentQuery eq) {
            String id = UUID.nameUUIDFromBytes(getUniqueString(eq).getBytes()).toString();
            _queryTasks.add(eq);
            
            EnrichmentQueryResults eqr = new EnrichmentQueryResults(System.currentTimeMillis());
            eqr.setStatus(EnrichmentQueryResults.SUBMITTED_STATUS);
            _queryResults.merge(id, eqr, (oldval, newval) -> newval.updateStartTime(oldval));        
            return id;
          }
        }      		
      );
  }

  /**
   * Sets milliseconds thread should sleep if no work needs to be done.
   * @param sleepTime 
   */
  public void updateThreadSleepTime(long sleepTime){
    _threadSleep = sleepTime;
  }

  protected void threadSleep(){
    try {
      Thread.sleep(_threadSleep);
    }
    catch(InterruptedException ie){

    }
  }

  /**
   * Processes any query tasks, looping until {@link #shutdown()} is invoked
   */
  @Override
  public void run() {
    while(_shutdown == false) {
      EnrichmentQuery eq = _queryTasks.poll();
      if (eq == null) {
        threadSleep();
        continue;
      }
      try {
		processQuery(geneSetSearchCache.get(eq), eq);
	} catch (ExecutionException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    }
    _logger.debug("Shutdown was invoked");
  }

  @Override
  public void shutdown() {
    _shutdown = true;
  }

  /**
   * Adds {@code gene} gene symbol (as upper case) to HashMap specified by 
   * {@code databaseId} to allow quick retrieval of all networks for a given
   * database that contain a specific gene.
   * 
   * Structure of data being stored.
   * 
   * {@code databaseId} -> {@code gene upper case} -> {@code networkIds}
   * 
   * @param databaseId Unique identifier for the database
   * @param gene Gene name
   * @param networkIds {@link java.util.Collection} of network ids
   */
  public void addGeneToDatabase(final String databaseId, final String gene,
    Collection<String> networkIds){
    if (_databases == null){
      _databases = new ConcurrentHashMap<>();
    }
    String geneUpperCase = gene.toUpperCase();
    ConcurrentHashMap<String, HashSet<String>> dbHash = _databases.get(databaseId);
    if (dbHash == null){
      dbHash = new ConcurrentHashMap<String, HashSet<String>>();
      _databases.put(databaseId, dbHash);
    }
    HashSet<String> geneSet = dbHash.get(geneUpperCase);
    if (geneSet == null){
      geneSet = new HashSet<String>();
      dbHash.put(geneUpperCase, geneSet);
    }
    geneSet.clear();
    geneSet.addAll(networkIds);
  }

  public void setDatabaseResults(InternalDatabaseResults dr){
    _databaseResults.set(dr);
  }

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
  }

/**
 * First tries to get EnrichmentQueryResults from _queryResults list
 * and if that fails method creates a new EnrichmentQueryResults setting
 * current time in constructor.
 * @param id
 * @return 
 */
  protected EnrichmentQueryResults getEnrichmentQueryResultsFromDb(final String id){
    EnrichmentQueryResults eqr = _queryResults.get(id);
    if (eqr == null){
      eqr = new EnrichmentQueryResults(System.currentTimeMillis());
    }
    return eqr;
  }

  protected EnrichmentQueryResults getEnrichmentQueryResultsFromDbOrFilesystem(final String id){
    EnrichmentQueryResults eqr = _queryResults.get(id);
    if (eqr != null){
      return eqr;
    }
    ObjectMapper mappy = new ObjectMapper();
    File eqrFile = new File(getEnrichmentQueryResultsFilePath(id));
    if (eqrFile.isFile() == false){
      _logger.error(eqrFile.getAbsolutePath() + " is not a file");
      return null;
    }
    try {
      return mappy.readValue(eqrFile, EnrichmentQueryResults.class);
    } catch(IOException io){
      _logger.error("Caught exception trying to load " + eqrFile.getAbsolutePath(), io);
    }
    return null;
  }

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
 * Runs enrichment on query storing results in _queryResults and _queryStatus
 * @param id 
 */
  protected void processQuery(final String id, EnrichmentQuery query){
    File taskDir = new File(this._taskDir + File.separator + id.toString());
    _logger.debug("Creating new task directory:" + taskDir.getAbsolutePath());
    
    if (taskDir.mkdirs() == false){
      _logger.error("Unable to create task directory: " + taskDir.getAbsolutePath());
      updateEnrichmentQueryResultsInDb(id, EnrichmentQueryResults.FAILED_STATUS, 100, null);
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
    updateEnrichmentQueryResultsInDb(id, EnrichmentQueryResults.COMPLETE_STATUS, 100, enrichmentResult);
    saveEnrichmentQueryResultsToFilesystem(id);
  }

  protected String getEnrichmentQueryResultsFilePath(final String id){
    return this._taskDir + File.separator + id.toString() + File.separator + BasicEnrichmentEngineImpl.EQR_JSON_FILE;
  }

  protected void saveEnrichmentQueryResultsToFilesystem(final String id){
    EnrichmentQueryResults eqr = getEnrichmentQueryResultsFromDb(id);
    if (eqr == null){
        return;
    }
    File destFile = new File(getEnrichmentQueryResultsFilePath(id));
    ObjectMapper mappy = new ObjectMapper();
    try (FileOutputStream out = new FileOutputStream(destFile)){
        mappy.writeValue(out, eqr);
    } catch(IOException io){
        _logger.error("Caught exception writing " + destFile.getAbsolutePath(), io);
    }
    _queryResults.remove(id);
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
      eqr.setHitGenes(new TreeSet<>(networkMap.get(network)));
      eqr.setNetworkUUID(network);
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

  public void annotateAndSaveNetwork(File destFile, NiceCXNetwork cxNetwork, EnrichmentQueryResult eqr){
    try (FileOutputStream fos = new FileOutputStream(destFile)) {
      if (cxNetwork == null){
        _logger.error("Network passed in is null, cant write out: " + destFile.getAbsolutePath());
        return;
      }
      _logger.info("Writing updated network to file: " + destFile.getName());
      if (cxNetwork.getMetadata() == null){
        _logger.error("No Meta data object for network" + destFile.getName());
        return;
      }
      long nodeAttrCntr = -1;
      try {
        nodeAttrCntr = cxNetwork.getMetadata().getIdCounter(NodeAttributesElement.ASPECT_NAME);
      } catch(NullPointerException npe){
        _logger.error("No id counter for network weird: " + destFile.getName());
      }
      InternalDatabaseResults idr = (InternalDatabaseResults)this._databaseResults.get();
      Map<String, Set<Long>> geneToNodeMap = idr.getNetworkToGeneToNodeMap().get(eqr.getNetworkUUID());
      if (geneToNodeMap != null){
        for (String hitGene : eqr.getHitGenes()){
          Set<Long> nodeIdSet = geneToNodeMap.get(hitGene);
          if (nodeIdSet != null){
            for (Long nodeId : nodeIdSet){
              NodeAttributesElement nae = new NodeAttributesElement(nodeId, "querynode", "true",
              ATTRIBUTE_DATA_TYPE.BOOLEAN);
              cxNetwork.addNodeAttribute(nae);
              nodeAttrCntr++;
            }
          }
        }
      }
      _logger.debug("Updating node attributes counter to " + Long.toString(nodeAttrCntr));
      cxNetwork.getMetadata().setElementCount(NodeAttributesElement.ASPECT_NAME, nodeAttrCntr);
      NdexCXNetworkWriter ndexwriter = new NdexCXNetworkWriter(fos, true);
      NiceCXNetworkWriter writer = new NiceCXNetworkWriter(ndexwriter);
      writer.writeNiceCXNetwork(cxNetwork);  
    }
    catch(IOException ex){
      _logger.error("problems writing cx", ex);
    }
    catch(NdexException nex){
      _logger.error("Problems writing network as cx", nex);
    }
  }
  
  public static void streamNetwork(OutputStream out, NiceCXNetwork cxNetwork, EnrichmentQueryResult eqr,
    InternalDatabaseResults idr){
    try  { 
      if (cxNetwork.getMetadata() == null){
        _logger.error("No Meta data object for network");
        return;
      }
      long nodeAttrCntr = -1;
      try {
        nodeAttrCntr = cxNetwork.getMetadata().getIdCounter(NodeAttributesElement.ASPECT_NAME);
      } catch(NullPointerException npe){
        _logger.error("No id counter for network weird: " + cxNetwork.getNetworkName());
      }
      Map<String, Set<Long>> geneToNodeMap = idr.getNetworkToGeneToNodeMap().get(eqr.getNetworkUUID());
      if (geneToNodeMap != null){
        for (String hitGene : eqr.getHitGenes()){
          Set<Long> nodeIdSet = geneToNodeMap.get(hitGene);
          if (nodeIdSet != null){
            for (Long nodeId : nodeIdSet){
              NodeAttributesElement nae = new NodeAttributesElement(nodeId, "querynode", "true",
              ATTRIBUTE_DATA_TYPE.BOOLEAN);
              cxNetwork.addNodeAttribute(nae);
              nodeAttrCntr++;
            }
          }
        }
      }
      
      _logger.debug("Updating node attributes counter to " + Long.toString(nodeAttrCntr));
      cxNetwork.getMetadata().setElementCount(NodeAttributesElement.ASPECT_NAME, nodeAttrCntr);
      NdexCXNetworkWriter ndexwriter = new NdexCXNetworkWriter(out, true);
      NiceCXNetworkWriter writer = new NiceCXNetworkWriter(ndexwriter);
      writer.writeNiceCXNetwork(cxNetwork);
      return;
    }
    catch(IOException ex){
      _logger.error("problems writing cx", ex);
    }
    catch(NdexException nex){
      _logger.error("Problems writing network as cx", nex);
    }
    return;
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
  
  protected double getSimilarity(SortedSet<String> networkGenes, SortedSet<String> queryGenes, int overlap, Map<String, Double> idfMap) {
    int size = networkGenes.size() + queryGenes.size() - overlap;
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
  
  protected double getCosineSimilarity(GVector vec1, GVector vec2) {
	  return (vec1.dot(vec2)) / (vec1.norm() * vec2.norm());
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
    
    SortedSet<String> networkGenes = new TreeSet<>();
    for (NodesElement node : cxNetwork.getNodes().values()) {
    	networkGenes.add(node.getNodeName());
    }
    eqr.setSimilarity(getSimilarity(networkGenes, queryGenes, numHitGenes, idr.getIdfMap()));
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

  @Override
  public String query(EnrichmentQuery thequery) throws EnrichmentException, ExecutionException {
    if (thequery.getDatabaseList() == null || thequery.getDatabaseList().isEmpty()){
      throw new EnrichmentException("No databases selected");
    }
    return geneSetSearchCache.get(thequery);
  }  

  String getUniqueString(EnrichmentQuery query) {
    String intermediary = query.getDatabaseList()
      .stream()
      .map( e -> e.trim())
      .filter(e -> e.length()>0)
      .collect(Collectors.joining(",")) 
      + ":" +
      query.getGeneList()
      .stream()
      .map( e -> e.trim())
      .filter(e -> e.length()>0)
      .collect(Collectors.joining(","));
    //String id = UUID.nameUUIDFromBytes(intermediary.getBytes()).toString();
    return intermediary;
  }

  @Override
  public DatabaseResults getDatabaseResults() throws EnrichmentException {
    return new DatabaseResults(this._databaseResults.get());
  }

  /**
   * Returns
   * @param id Id of the query. 
   * @param start starting index to return from. Starting index is 0.
   * @param size Number of results to return. If 0 means all from starting index so
   *             to get all set both {@code start} and {@code size} to 0.
   * @return {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResults} object
   *         or null if no result could be found. 
   * @throws EnrichmentException If there was an error getting the results
   */
  @Override
  public EnrichmentQueryResults getQueryResults(String id, int start, int size) throws EnrichmentException {
    EnrichmentQueryResults eqr = getEnrichmentQueryResultsFromDbOrFilesystem(id);
    if (start < 0){
      throw new EnrichmentException("start parameter must be value of 0 or greater");
    }
    if (size < 0){
      throw new EnrichmentException("size parameter must be value of 0 or greater");
    }

    if (start == 0 && size == 0){
      return eqr;
    }
    List<EnrichmentQueryResult> eqrSubList = new LinkedList<>();
    int numElementsAdded = 0;
    for (EnrichmentQueryResult element : eqr.getResults()){
      if (element.getRank() < start){
        continue;
      }
      eqrSubList.add(element);
      numElementsAdded++;
      if (numElementsAdded >= size){
        break;
      }
    }
    return new EnrichmentQueryResults(eqr, eqrSubList);
  }

  @Override
  public EnrichmentQueryStatus getQueryStatus(String id) throws EnrichmentException {
    EnrichmentQueryResults eqr = getEnrichmentQueryResultsFromDbOrFilesystem(id);
    if (eqr == null){
      return null;
    }
    return new EnrichmentQueryStatus(eqr);
  }

  @Override
  public void delete(String id) throws EnrichmentException {
    _logger.debug("Deleting task " + id);
    if (_queryResults.containsKey(id) == true){
      _queryResults.remove(id);
    }
    File thisTaskDir = new File(this._taskDir + File.separator + id);
    if (thisTaskDir.exists() == false){
      return;
    }
    _logger.debug("Attempting to delete task from filesystem: " + thisTaskDir.getAbsolutePath());
    if (FileUtils.deleteQuietly(thisTaskDir) == false){
      _logger.error("There was a problem deleting the directory: " + thisTaskDir.getAbsolutePath());
    }
  }

  protected EnrichmentQueryResult getEnrichmentQueryResult(final String id, 
    final String networkUUID) throws EnrichmentException {
      EnrichmentQueryResults eqResults = getEnrichmentQueryResultsFromDbOrFilesystem(id);
      if (eqResults == null){
          return null;
      }
      if (eqResults.getResults() == null){
          return null;
      }
      for (EnrichmentQueryResult eqr: eqResults.getResults()){
          if (eqr.getDatabaseUUID() == null){
              continue;
          }
          if (eqr.getNetworkUUID() == null){
              continue;
          }
          if (!eqr.getNetworkUUID().equals(networkUUID)){
              continue;
          }
          return eqr;
      }
      return null;
  }

  @Override
  public InputStream getNetworkOverlayAsCX(String id, String databaseUUID, String networkUUID) throws EnrichmentException { 
    EnrichmentQueryResult eqr = getEnrichmentQueryResult(id, networkUUID);
    if (eqr == null){
      _logger.error("No network found");
      return null;
    }
    NiceCXNetwork cxNetwork = getNetwork(eqr.getDatabaseUUID(), networkUUID);
    if (cxNetwork == null){
      _logger.error("Unable to get network: " + networkUUID + " skipping...");
      return null;
    }
    
    File destFile = new File(this._taskDir + File.separator + id +
      File.separator + networkUUID + ".cx");
    
    try {
      if (!destFile.exists() || destFile.length() == 0){
        File tmpFile = new File(this._taskDir + File.separator + id +
          File.separator + UUID.randomUUID().toString() + ".cx");
        annotateAndSaveNetwork(tmpFile, cxNetwork, eqr);
        tmpFile.renameTo(destFile);
      }
        
      return new FileInputStream(destFile);
    } catch(FileNotFoundException fe){
      _logger.error("File not found", fe);
    }
    return null;
  }

  /**
   * Gets ServerStatus
   * @return 
   * @throws EnrichmentException If there was a problem
   */
  @Override
  public ServerStatus getServerStatus() throws EnrichmentException {
    try {
      String version = "unknown";
      ServerStatus sObj = new ServerStatus();
      sObj.setStatus(ServerStatus.OK_STATUS);
      sObj.setRestVersion(EnrichmentHttpServletDispatcher.getVersion());
      OperatingSystemMXBean omb = ManagementFactory.getOperatingSystemMXBean();
      float unknown = (float)-1;
      float load = (float)omb.getSystemLoadAverage();
      sObj.setLoad(Arrays.asList(load, unknown, unknown));
      File taskDir = new File(this._taskDir);
      sObj.setPcDiskFull(100-(int)Math.round(((double)taskDir.getFreeSpace()/(double)taskDir.getTotalSpace())*100));
      return sObj;
    } catch(Exception ex){
      _logger.error("ServerStatus error", ex);
      throw new EnrichmentException("Exception raised when getting ServerStatus: " + ex.getMessage());
    }
  }
}