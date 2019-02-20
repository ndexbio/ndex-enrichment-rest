/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.engine;

import java.util.Map;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

/**
 *
 * @author churas
 */
public class BasicEnrichmentEngineFactory {
    
    private String _tmpDir;
    private NdexRestClientModelAccessLayer _client;
    private Map<String, String> _ownerNameMap;
    
    /**
     * Temp directory where query results will temporarily be stored.
     * @param tmpDir 
     */
    public BasicEnrichmentEngineFactory(final String tmpDir, NdexRestClientModelAccessLayer client,
            Map<String, String> databaseOwnerNameMap){
        _tmpDir = tmpDir;
        _client = client;
        _ownerNameMap = databaseOwnerNameMap;
    }
    
    /**
     * Creates EnrichmentEngine
     * @return 
     */
    public EnrichmentEngine getEnrichmentEngine(){
        
        return null;
    }
    
}
