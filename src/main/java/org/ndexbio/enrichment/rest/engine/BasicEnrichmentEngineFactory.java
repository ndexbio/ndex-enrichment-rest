/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.engine;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.ndexbio.enrichment.rest.model.DatabaseResult;
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

    private String _dbDir;
    private String _taskDir;
    private InternalDatabaseResults _databaseResults;
    private int _numWorkers;
    
    /**
     * Temp directory where query results will temporarily be stored.
     * @param tmpDir 
     */
    public BasicEnrichmentEngineFactory(Configuration config){
        
        _dbDir = config.getEnrichmentDatabaseDirectory();
        _taskDir = config.getEnrichmentTaskDirectory();
        _databaseResults = config.getNDExDatabases();
        _numWorkers = config.getNumberWorkers();
    }
    
    
    /**
     * Creates EnrichmentEngine
     * @return 
     */
    public EnrichmentEngine getEnrichmentEngine() throws EnrichmentException {
    	_logger.debug("Creating executor service with: " + Integer.toString(_numWorkers) + " workers");
    	ExecutorService es = Executors.newFixedThreadPool(_numWorkers);
    	
        BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(es, _dbDir, _taskDir);
        enricher.setDatabaseResults(_databaseResults);
        for (DatabaseResult dr : _databaseResults.getResults()){
            _logger.debug("Loading: " + dr.getName());
            addGeneMapToEnricher(enricher, dr);
            _logger.debug("Done with loading");
        }
        return enricher;
    }
    
    protected void addGeneMapToEnricher(BasicEnrichmentEngineImpl enricher,
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
                    enricher.addGeneToDatabase(dr.getUuid(), gene, igm.getGeneMap().get(gene));
                }
            }
        }
    }    
}
