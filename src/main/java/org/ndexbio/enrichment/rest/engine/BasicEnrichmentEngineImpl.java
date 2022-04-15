package org.ndexbio.enrichment.rest.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.ndexbio.cxio.core.NdexCXNetworkWriter;
import org.ndexbio.cxio.core.writers.NiceCXNetworkWriter;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.enrichment.rest.engine.util.BasicEnrichmentEngineRunner;
import org.ndexbio.enrichment.rest.model.DatabaseResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryStatus;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.enrichment.rest.model.ServerStatus;
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
import java.util.Comparator;
import java.util.Map;
import org.ndexbio.enrichment.rest.engine.util.NetworkAnnotator;

/**
 * Runs enrichment 
 * @author churas
 */
public class BasicEnrichmentEngineImpl implements EnrichmentEngine {
	public static final String CX_SUFFIX = ".cx";
	public static final String EQR_JSON_FILE = "enrichmentqueryresults.json";
    public static final String QUERY_JSON_FILE = "query.json";
	public static final String SUMMARY_RESULTS_FILE = "summary_results.csv";

	static Logger _logger = LoggerFactory.getLogger(BasicEnrichmentEngineImpl.class);

	private String _dbDir;
	private String _taskDir;
	private boolean _shutdown;
	private ExecutorService _executorService;
	private ConcurrentHashMap<String, Future> _futureTaskMap;

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
        
        private List<NetworkAnnotator> _networkAnnotators;
	
	private HashSet<String> _uniqueGeneSet;
	
	private Comparator<EnrichmentQueryResult> _comparator;

	private long _threadSleep = 10;

	//Cache
	private final LoadingCache<EnrichmentQuery, String> geneSetSearchCache;
	private static int resultCacheSize = 600;
	
	private int _numResultsToReturn;

	public BasicEnrichmentEngineImpl(ExecutorService es, 
			final String dbDir,
			final String taskDir, int numResultsToReturn, Comparator<EnrichmentQueryResult> resComparator){
		_executorService = es;
		_futureTaskMap = new ConcurrentHashMap<>();
		_shutdown = false;
		_dbDir = dbDir;
		_taskDir = taskDir;
		_queryResults = new ConcurrentHashMap<>();
		_databaseResults = new AtomicReference<>();
		_queryTasks = new ConcurrentLinkedQueue<>();
		_uniqueGeneSet = new HashSet<>();
		_comparator = resComparator;
		_numResultsToReturn = numResultsToReturn;

		RemovalListener<EnrichmentQuery, String> removalListener = new RemovalListener<EnrichmentQuery, String>() {
			@Override
			public void onRemoval(RemovalNotification<EnrichmentQuery, String> removal) {
				String id = removal.getValue();
				try {
					delete(id);
				} catch (EnrichmentException e) {
					_logger.error("Cache delete on task {} raised exception", id, e);
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
								long startTime = System.currentTimeMillis();
								String id = UUID.nameUUIDFromBytes(getUniqueString(eq).getBytes()).toString();
								_logger.info("Creating id {} took {} ms", id, System.currentTimeMillis() - startTime);
								EnrichmentQueryResults eqr = new EnrichmentQueryResults(System.currentTimeMillis());
								eqr.setStatus(EnrichmentQueryResults.SUBMITTED_STATUS);
								BasicEnrichmentEngineRunner task = new BasicEnrichmentEngineRunner(id, _taskDir, _dbDir, _databaseResults, _databases, _uniqueGeneSet, _comparator, _numResultsToReturn, eq, eqr);
								_futureTaskMap.put(id, _executorService.submit(task));
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
			Future f;
			String taskId;
			Iterator<String> idItr = _futureTaskMap.keySet().iterator();
			while(idItr.hasNext()) {
				taskId = idItr.next();
				
				f = _futureTaskMap.get(taskId);
				if (f == null) {
					continue;
				}
				if (f.isCancelled()) {
					_futureTaskMap.remove(taskId);
				} else if (f.isDone()) {
					_logger.debug("Found a completed or failed task");
					try {
						EnrichmentQueryResults eqr = (EnrichmentQueryResults) f.get();
						_queryResults.merge(taskId, eqr, (oldval, newval) -> newval.updateStartTime(oldval));
						saveEnrichmentQueryResultsToFilesystem(taskId);
					} catch (InterruptedException ex) {
						_logger.error("Got interrupted exception", ex);
                    } catch (ExecutionException ex) {
                        _logger.error("Got execution exception", ex);
                    } catch (CancellationException ex){
                        _logger.error("Got cancellation exception", ex);
					}
					_futureTaskMap.remove(taskId);
				}
			}
			threadSleep();
		}
		_logger.debug("Shutdown was invoked");
	}

	@Override
	public void shutdown() {
		_shutdown = true;
	}

        public void setNetworkAnnotators(List<NetworkAnnotator> annotators){
            _networkAnnotators = annotators;
        }

	public void setDatabaseResults(InternalDatabaseResults dr){
		_databaseResults.set(dr);
	}
	
	public void setDatabaseMap(ConcurrentHashMap<String, ConcurrentHashMap<String, HashSet<String>>> databases){
		_databases = databases;
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

	protected String getEnrichmentQueryResultsFilePath(final String id){
		return this._taskDir + File.separator + id + File.separator + BasicEnrichmentEngineImpl.EQR_JSON_FILE;
	}
        
        protected String getEnrichmentQueryFilePath(final String id){
		return this._taskDir + File.separator + id + File.separator + BasicEnrichmentEngineImpl.QUERY_JSON_FILE;
	}
        
        protected EnrichmentQuery getEnrichmentQueryFromFilesystem(final String id){
            ObjectMapper mappy = new ObjectMapper();
		File eqrFile = new File(getEnrichmentQueryFilePath(id));
		if (eqrFile.isFile() == false){
			_logger.error(eqrFile.getAbsolutePath() + " is not a file");
			return null;
		}
		try {
			return mappy.readValue(eqrFile, EnrichmentQuery.class);
		} catch(IOException io){
			_logger.error("Caught exception trying to load " + eqrFile.getAbsolutePath(), io);
		}
		return null;
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
         * Annotates and saves network with {@link NetworkAnnotator} objects set via constructor
         * If there is an issue, error level logging messages are emitted and a destination
         * file may not be written
         * 
         * @param destFile Where to save network
         * @param cxNetwork Network to annotate and save
         * @param eqr Query result to annotate network with
         */
	protected void annotateAndSaveNetwork(File destFile, NiceCXNetwork cxNetwork, EnrichmentQuery query, EnrichmentQueryResult eqr){
		if (destFile == null){
			_logger.error("destFile is null");
			return;
		}
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
			
                        if (this._networkAnnotators != null){
                            for (NetworkAnnotator annotator : _networkAnnotators){
                                try {
                                    annotator.annotateNetwork(cxNetwork, query, eqr);
                                } catch(EnrichmentException ee){
                                    _logger.error("Caught exception trying to annotate network with : " + 
                                            annotator.getClass().getCanonicalName() + " class : " + ee.getMessage(), ee);
                                }
                            }
                        }
                        
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
	
	protected NiceCXNetwork getNetwork(final String databaseUUID, final String networkUUID){
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(new File(this._dbDir + File.separator + databaseUUID + File.separator + networkUUID + CX_SUFFIX));
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
        
        private void removeQueryFromCache(final String id){
            Map<EnrichmentQuery, String> cacheMap = this.geneSetSearchCache.asMap();
            EnrichmentQuery queryToInvalidate = null;
            for (EnrichmentQuery query : cacheMap.keySet()){
                String queryId = cacheMap.get(query);
                if (queryId == null){
                    continue;
                }
                if (queryId.equals(id)){
                    queryToInvalidate = query;
                    break;
                }
            }
            if (queryToInvalidate != null){
                this.geneSetSearchCache.invalidate(queryToInvalidate);
            }
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
			throw new EnrichmentException("start parameter must be a value of 0 or greater");
		}
		if (size < 0){
			throw new EnrichmentException("size parameter must be a value of 0 or greater");
		}

		if (eqr == null || (start == 0 && size == 0) || eqr.getResults() == null){
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
                
                removeQueryFromCache(id);
                
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
				File.separator + networkUUID + CX_SUFFIX);

		try {
			if (!destFile.exists() || destFile.length() == 0){
				File tmpFile = new File(this._taskDir + File.separator + id +
						File.separator + UUID.randomUUID().toString() + CX_SUFFIX);
				long startTime = System.currentTimeMillis();
                                EnrichmentQuery query = this.getEnrichmentQueryFromFilesystem(id);
				annotateAndSaveNetwork(tmpFile, cxNetwork, query, eqr);
				_logger.info("Annotating network {} for task {} took {} ms",
						new Object[]{networkUUID, id, System.currentTimeMillis() - startTime});
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

		String version = "unknown";
		ServerStatus sObj = new ServerStatus();
		sObj.setStatus(ServerStatus.OK_STATUS);
		sObj.setRestVersion(EnrichmentHttpServletDispatcher.getVersion());
                if (this.geneSetSearchCache != null){
                    sObj.setCacheSize(this.geneSetSearchCache.size());
                }
		OperatingSystemMXBean omb = ManagementFactory.getOperatingSystemMXBean();
		float unknown = (float)-1;
		float load = (float)omb.getSystemLoadAverage();
		sObj.setLoad(Arrays.asList(load, unknown, unknown));
		if (this._taskDir == null){
			throw new EnrichmentException("Task directory is null");
		}
		File taskDir = new File(this._taskDir);
		sObj.setPcDiskFull(100-(int)Math.round(((double)taskDir.getFreeSpace()/(double)taskDir.getTotalSpace())*100));
		return sObj;
	}
}