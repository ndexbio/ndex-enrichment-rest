/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.engine;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.ndexbio.cxio.aspects.datamodels.NodeAttributesElement;
import org.ndexbio.cxio.aspects.datamodels.NodesElement;
import org.ndexbio.enrichment.rest.model.DatabaseResult;
import org.ndexbio.enrichment.rest.model.DatabaseResults;
import org.ndexbio.model.cx.NiceCXNetwork;
import org.ndexbio.model.object.NetworkSearchResult;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author churas
 */
public class BasicEnrichmentEngineFactory {
    
    static Logger _logger = LoggerFactory.getLogger(BasicEnrichmentEngineImpl.class);

    private String _tmpDir;
    private NdexRestClientModelAccessLayer _client;
    
    private Map<String, String> _ownerNameMap;
    private DatabaseResults _databaseResults;
    
    /**
     * Temp directory where query results will temporarily be stored.
     * @param tmpDir 
     */
    public BasicEnrichmentEngineFactory(final String tmpDir, NdexRestClientModelAccessLayer client,
            Map<String, String> databaseOwnerNameMap,
            DatabaseResults databaseResults){
        _tmpDir = tmpDir;
        _client = client;
        _ownerNameMap = databaseOwnerNameMap;
        _databaseResults = databaseResults;
        
    }
    
    /**
     * Creates EnrichmentEngine
     * @return 
     */
    public EnrichmentEngine getEnrichmentEngine() throws Exception {
        BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(_tmpDir,
                                                                           _client);
        enricher.setDatabaseResults(_databaseResults);
        for (DatabaseResult dr : _databaseResults.getResults()){
            _logger.debug("Processing: " + dr.getName());
            addGeneMapToEnricher(enricher, dr);
        }
        return enricher;
    }
    
    protected void addGeneMapToEnricher(BasicEnrichmentEngineImpl enricher,
            DatabaseResult dr) throws Exception{
        String networkOwner = _ownerNameMap.get(dr.getUuid());
        if (networkOwner == null){
            _logger.error("Unable to find account for database: " + dr.getName() + " with uuid: " + dr.getUuid());
            return;
        }
        HashMap<String, HashSet<String>> geneMap = buildMap(networkOwner, dr);
        for (String gene : geneMap.keySet()){
            enricher.addGeneToDatabase(dr.getUuid(), gene, geneMap.get(gene));
        }
    }
    
    protected HashMap<String, HashSet<String>> buildMap(final String networkOwner,
            DatabaseResult drToUpdate) throws Exception{
                HashMap<String, HashSet<String>> mappy = new HashMap<>();
        NetworkSearchResult nrs = _client.findNetworks("", networkOwner, 0, 0);
        
        //set number of networks in this database
        drToUpdate.setNumberOfNetworks(Integer.toString(nrs.getNetworks().size()));
        
        for (NetworkSummary ns :  nrs.getNetworks()){
            _logger.debug(ns.getName() + " Nodes => " + Integer.toString(ns.getNodeCount()) + " Edges => " + Integer.toString(ns.getEdgeCount()));
            NiceCXNetwork network = _client.getNetwork(ns.getExternalId());
            Map<Long, Collection<NodeAttributesElement>> attribMap = network.getNodeAttributes();
            for (NodesElement ne : network.getNodes().values()){
                Collection<NodeAttributesElement> nodeAttribs = attribMap.get(ne.getId());
                
                // If there are node attributes and one is named "type" then
                // only include the node name if type is gene or protein
                if (nodeAttribs != null){
                    boolean validgene = false;
                    for (NodeAttributesElement nae : nodeAttribs){
                        if (nae.getName().toLowerCase().equals("type")){
                            if (nae.getValue().toLowerCase().equals("gene") ||
                                  nae.getValue().toLowerCase().equals("protein")){
                                validgene = true;
                                break;
                            }
                        }
                    }
                    if (validgene == false){
                        continue;
                    }
                }
                String name = ne.getNodeName();
             
                if (mappy.containsKey(name) == false){
                    mappy.put(name, new HashSet<String>());
                }
                if (mappy.get(name).contains(ns.getExternalId()) == false){
                    mappy.get(name).add(ns.getExternalId().toString());
                }
            }
        }        
        return mappy;
    }
    
}
