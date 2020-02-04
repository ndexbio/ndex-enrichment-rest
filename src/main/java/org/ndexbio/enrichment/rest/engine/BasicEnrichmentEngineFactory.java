package org.ndexbio.enrichment.rest.engine;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.ndexbio.ndexsearch.rest.model.DatabaseResult;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.enrichment.rest.model.InternalGeneMap;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.enrichment.rest.services.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author churas
 */
public class BasicEnrichmentEngineFactory {
    
    static Logger _logger = LoggerFactory.getLogger(BasicEnrichmentEngineFactory.class);

    private final String _dbDir;
    private final String _taskDir;
    private final InternalDatabaseResults _databaseResults;
	private final int _numWorkers;
	private ExecutorServiceFactory _executorServiceFac;
    
    /**
     * Temp directory where query results will temporarily be stored.
     * @param tmpDir 
     */
    public BasicEnrichmentEngineFactory(Configuration config) throws NullPointerException {
        
        _dbDir = config.getEnrichmentDatabaseDirectory();
        _taskDir = config.getEnrichmentTaskDirectory();
        _databaseResults = config.getNDExDatabases();
		if (_databaseResults == null){
			throw new NullPointerException("Unable to retrieve database");
		}
		_numWorkers = config.getNumberWorkers();
		_executorServiceFac = new ExecutorServiceFactoryImpl();
    }
    
    protected void setAlternateExecutorServiceFactory(ExecutorServiceFactory esf){
		_executorServiceFac = esf;
	}

    /**
     * Creates EnrichmentEngine
     * @return 
     */
    public EnrichmentEngine getEnrichmentEngine() throws EnrichmentException {
		_logger.debug("Creating executor service with: " + Integer.toString(_numWorkers) + " workers");
    	ExecutorService es = _executorServiceFac.getExecutorService(_numWorkers);
    	ConcurrentHashMap<String, ConcurrentHashMap<String, HashSet<String>>> databases = new ConcurrentHashMap<>();
        
		if (_databaseResults.getResults() == null){
			throw new NullPointerException("No data found in database");
		}
		for (DatabaseResult dr : _databaseResults.getResults()){
			_logger.debug("Loading: " + dr.getName());
			addGeneMapToEnricher(databases, dr);
			_logger.debug("Done with loading");
		}

		BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(es, _dbDir, _taskDir);
		enricher.setDatabaseResults(_databaseResults);
		enricher.setDatabaseMap(databases);
        return enricher;
    }
    
	
    private void addGeneMapToEnricher(ConcurrentHashMap<String, ConcurrentHashMap<String, HashSet<String>>> databases,
            DatabaseResult dr) throws EnrichmentException{
        for (InternalGeneMap igm : _databaseResults.getGeneMapList()){
            if (igm.getDatabaseUUID().equals(dr.getUuid())){
                // found matching entry
                Map<String,Set<String>> geneMap = igm.getGeneMap();
                if (geneMap == null){
                    _logger.error("Gene map is null for database with id: " +
                                  dr.getUuid() + " skipping");
                    continue;
                } 
                for (String gene : igm.getGeneMap().keySet()){
                    addGeneToDatabase(databases, dr.getUuid(), gene, igm.getGeneMap().get(gene));
                }
            }
        }
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
	private void addGeneToDatabase(ConcurrentHashMap<String, ConcurrentHashMap<String, HashSet<String>>> databases, final String databaseId, final String gene,
			Collection<String> networkIds){
		
		String geneUpperCase = gene.toUpperCase();
		ConcurrentHashMap<String, HashSet<String>> dbHash = databases.get(databaseId);
		if (dbHash == null){
			dbHash = new ConcurrentHashMap<>();
			databases.put(databaseId, dbHash);
		}
		HashSet<String> geneSet = dbHash.get(geneUpperCase);
		if (geneSet == null){
			geneSet = new HashSet<>();
			dbHash.put(geneUpperCase, geneSet);
		}
		geneSet.clear();
		geneSet.addAll(networkIds);
	}
}
