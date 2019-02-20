/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.engine;

import org.ndexbio.enrichment.rest.exceptions.EnrichmentException;
import org.ndexbio.enrichment.rest.model.DatabaseResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryStatus;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

/**
 * Runs enrichment 
 * @author churas
 */
public class BasicEnrichmentEngineImpl implements EnrichmentEngine {

    private String _tmpDir;
    private NdexRestClientModelAccessLayer _client;
    public BasicEnrichmentEngineImpl(final String tmpDir,
            NdexRestClientModelAccessLayer client){
        _tmpDir = tmpDir;
        _client = client;
    }
    
    // need worker that runs synchronized on processing query list
    
    @Override
    public String query(EnrichmentQuery query) throws EnrichmentException {
        throw new UnsupportedOperationException("Not supported yet."); 
        /**
         * if query.getDatabaseList() == null or is empty
         *    raise exception
         * 
         * id = new uuid();
         * this._queryList(new Query(id, query));
         * 
         * return id;
         * 
         */
    }

    @Override
    public DatabaseResults getDatabaseResults() throws EnrichmentException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }
    @Override
    public EnrichmentQueryResults getQueryResults(String id, int start, int size) throws EnrichmentException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public EnrichmentQueryStatus getQueryStatus(String id) throws EnrichmentException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public void delete(String id) throws EnrichmentException {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public String getNetworkOverlayAsCX(String id, String databaseUUID, String networkUUID) throws EnrichmentException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
