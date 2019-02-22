/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.engine;

import java.util.HashMap;
import java.util.HashSet;
import org.ndexbio.enrichment.rest.model.DatabaseResult;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.enrichment.rest.model.InternalGeneMap;
import org.ndexbio.enrichment.rest.services.Configuration;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author churas
 */
public class BasicEnrichmentEngineFactory {
    
    static Logger _logger = LoggerFactory.getLogger(BasicEnrichmentEngineFactory.class);

    private String _dbDir;
    private NdexRestClientModelAccessLayer _client;
    private InternalDatabaseResults _databaseResults;
    
    /**
     * Temp directory where query results will temporarily be stored.
     * @param tmpDir 
     */
    public BasicEnrichmentEngineFactory(Configuration config){
        
        _dbDir = config.getEnrichmentDatabaseDirectory();
        _databaseResults = config.getNDExDatabases();
        _client = config.getNDExClient();
    }
    
    
    /**
     * Creates EnrichmentEngine
     * @return 
     */
    public EnrichmentEngine getEnrichmentEngine() throws Exception {
        BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(_dbDir,
                                                                           _client);
        enricher.setDatabaseResults(_databaseResults);
        for (DatabaseResult dr : _databaseResults.getResults()){
            _logger.debug("Loading: " + dr.getName());
            addGeneMapToEnricher(enricher, dr);
            _logger.debug("Done with loading");
        }
        return enricher;
    }
    
    protected void addGeneMapToEnricher(BasicEnrichmentEngineImpl enricher,
            DatabaseResult dr) throws Exception{
        String networkOwner = _databaseResults.getDatabaseAccountOwnerMap().get(dr.getUuid());
        if (networkOwner == null){
            _logger.error("Unable to find account for database: " + dr.getName() + " with uuid: " + dr.getUuid());
            return;
        }
        for (InternalGeneMap igm : _databaseResults.getGeneMapList()){
            if (igm.getDatabaseUUID().equals(dr.getUuid())){
                // found matching entry
                for (String gene : igm.getGeneMap().keySet()){
                    enricher.addGeneToDatabase(dr.getUuid(), gene, igm.getGeneMap().get(gene));
                }
            }
        }
    }    
}
