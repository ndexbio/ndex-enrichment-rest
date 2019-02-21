/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.engine;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.ndexbio.enrichment.rest.exceptions.EnrichmentException;
import org.ndexbio.enrichment.rest.model.DatabaseResult;
import org.ndexbio.enrichment.rest.model.DatabaseResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryStatus;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs enrichment 
 * @author churas
 */
public class BasicEnrichmentEngineImpl implements EnrichmentEngine {

    static Logger _logger = LoggerFactory.getLogger(BasicEnrichmentEngineImpl.class);

    private String _tmpDir;
    
    /**
     * This should be a map of <query UUID> => EnrichmentQuery object
     */
    private ConcurrentHashMap<String, EnrichmentQuery> _queryTasks;
    
    /**
     * This should be a map of <query UUID> => EnrichmentQueryResults object
     */
    private ConcurrentHashMap<String, EnrichmentQueryResults> _queryResults;
        
    /**
     * This should be a map of <database UUID> => Map<Gene => Set of network UUIDs>
     */
    private ConcurrentHashMap<String, ConcurrentHashMap<String, HashSet<String>>> _databases;
    
    private AtomicReference<DatabaseResults> _databaseResults;
    private NdexRestClientModelAccessLayer _client;
    
    public BasicEnrichmentEngineImpl(final String tmpDir,
            NdexRestClientModelAccessLayer client){
        _tmpDir = tmpDir;
        _client = client;
        _queryTasks = new ConcurrentHashMap<String, EnrichmentQuery>();
        _queryResults = new ConcurrentHashMap<String, EnrichmentQueryResults>();
        _databaseResults = new AtomicReference<>();
    }
    
    /**
     * Adds a gene map to database
     * @param databaseId
     * @param gene
     * @param networkIds 
     */
    public void addGeneToDatabase(final String databaseId, final String gene,
            Collection<String> networkIds){
        if (_databases == null){
            _databases = new ConcurrentHashMap<String, ConcurrentHashMap<String, HashSet<String>>>();
        }
        ConcurrentHashMap<String, HashSet<String>> dbHash = _databases.get(databaseId);
        if (dbHash == null){
            dbHash = new ConcurrentHashMap<String, HashSet<String>>();
            _databases.put(databaseId, dbHash);
        }
        HashSet<String> geneSet = dbHash.get(gene);
        if (geneSet == null){
            geneSet = new HashSet<String>();
            dbHash.put(gene, geneSet);
        }
        geneSet.clear();
        geneSet.addAll(networkIds);
    }
    // @TODO need worker that runs synchronized on processing query list
    
    public void setDatabaseResults(DatabaseResults dr){
        _databaseResults.set(dr);
    }

    protected List<String> getUniqueGeneList(List<String> geneList){
        if (geneList == null){
            return null;
        }
        HashSet<String> uniqueGenes = new HashSet<String>(geneList);
        return new LinkedList<String>(uniqueGenes);
    }
    /**
     * Runs enrichment on query storing results in _queryResults and _queryStatus
     * @param id 
     */
    protected void processQuery(final String id, EnrichmentQuery query){
        //check gene list
        List<EnrichmentQueryResult> enrichmentResult = new LinkedList<EnrichmentQueryResult>();
        for (String databaseName : query.getDatabaseList()){
            DatabaseResult dbres = null;
            for (DatabaseResult res : _databaseResults.get().getResults()){
                if (res.getName().toLowerCase().equals(databaseName.toLowerCase())){
                    dbres = res;
                    break;
                }
            }
            if (dbres == null){
                _logger.error("No database matching: " + databaseName + " found. Skipping");
                continue;
            }
            List<String> uniqueGeneList = getUniqueGeneList(query.getGeneList());
            HashMap<String, HashSet<String>> networkMap = remapNetworksToGenes(dbres.getUuid(), uniqueGeneList);
            if (networkMap == null){
                continue;
            }
            // generate EnrichmentQueryResult from networkMap
            enrichmentResult.addAll(getEnrichmentQueryResultObjectsFromNetworkMap(dbres, networkMap, uniqueGeneList));
        }

        // combine all EnrichmentQueryResults generated above and create
        // EnrichmentQueryResults object and store in _queryResults and _queryStatus
        // replacing any existing entry
        EnrichmentQueryResults eqr = _queryResults.get(id);
        if (eqr == null){
            eqr = new EnrichmentQueryResults(System.currentTimeMillis());
        }
        
        eqr.setProgress(100);
        eqr.setStatus(EnrichmentQueryResults.COMPLETE_STATUS);
        eqr.setNumberOfHits(enrichmentResult.size());
        eqr.setResults(enrichmentResult);
        eqr.setWallTime(System.currentTimeMillis() - eqr.getStartTime());
        _queryResults.merge(id, eqr, (oldval, newval) -> newval.updateStartTime(oldval));        
    }
    
    /**
     * Generates EnrichmentQueryResult objects from data passed in
     * @param dbres Database associated with this result
     * @param networkMap Map where key is networkUUID and value is list of genes
     *                   that were found to match the query on that network
     * @param uniqueGeneList Unique list of genes
     * @return List of EnrichmentQueryResult objects 
     */
    protected List<EnrichmentQueryResult> getEnrichmentQueryResultObjectsFromNetworkMap(DatabaseResult dbres,
            HashMap<String, HashSet<String>> networkMap, List<String> uniqueGeneList){
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
        List<EnrichmentQueryResult> eqrList = new LinkedList<EnrichmentQueryResult>();
        for (String network : networkMap.keySet()){
            EnrichmentQueryResult eqr = new EnrichmentQueryResult();
            eqr.setDatabaseName(dbres.getName());
            eqr.setDatabaseUUID(dbres.getUuid());
            eqr.setHitGenes(new LinkedList<String>(networkMap.get(network)));
            eqr.setNetworkUUID(network);
            eqr.setPercentOverlap(0); //need to pass in # of genes to figure this out
            eqr.setRank(0); //needs to be set later once we get all the pvalues
            eqr.setpValue(0); //need a method to get this
            eqrList.add(eqr);
        }
        return eqrList;
    }

    protected HashMap<String, HashSet<String>> remapNetworksToGenes(final String databaseId, final List<String> geneList){
        HashMap<String, HashSet<String>> networkMap = new HashMap<String, HashSet<String>>();

        ConcurrentHashMap<String, HashSet<String>> dbMap = _databases.get(databaseId);
        if (dbMap == null){
            _logger.debug("No database with id: " + databaseId + " found. Skipping");
            return null;
        }
        for (String gene : geneList){
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
    public String query(EnrichmentQuery thequery) throws EnrichmentException {
        
        if (thequery.getDatabaseList() == null || thequery.getDatabaseList().isEmpty()){
            throw new EnrichmentException("No databases selected");
        }
        String id = UUID.randomUUID().toString();
        this._queryTasks.put(id, thequery);
        EnrichmentQueryResults eqr = new EnrichmentQueryResults(System.currentTimeMillis());
        eqr.setStatus(EnrichmentQueryResults.SUBMITTED_STATUS);
        _queryResults.merge(id, eqr, (oldval, newval) -> newval.updateStartTime(oldval));        
        return id;
    }

    @Override
    public DatabaseResults getDatabaseResults() throws EnrichmentException {
        return _databaseResults.get();
    }
    
    @Override
    public EnrichmentQueryResults getQueryResults(String id, int start, int size) throws EnrichmentException {
        if (_queryResults.containsKey(id) == false){
            return null;
        }
        return _queryResults.get(id);
    }

    @Override
    public EnrichmentQueryStatus getQueryStatus(String id) throws EnrichmentException {
        if (_queryResults.containsKey(id) == false){
            return null;
        }
        return (EnrichmentQueryStatus)_queryResults.get(id);
    }

    @Override
    public void delete(String id) throws EnrichmentException {
        if (_queryResults.containsKey(id) == true){
            _queryResults.remove(id);
        }
        // @TODO remove any files from the filesystem
    }

    @Override
    public String getNetworkOverlayAsCX(String id, String databaseUUID, String networkUUID) throws EnrichmentException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
