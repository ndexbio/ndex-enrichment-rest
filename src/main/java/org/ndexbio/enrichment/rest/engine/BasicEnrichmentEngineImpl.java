/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.ndexbio.cxio.core.NdexCXNetworkWriter;
import org.ndexbio.enrichment.rest.exceptions.EnrichmentException;
import org.ndexbio.enrichment.rest.model.DatabaseResult;
import org.ndexbio.enrichment.rest.model.DatabaseResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryStatus;
import org.ndexbio.model.cx.NiceCXNetwork;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;
import org.ndexbio.rest.client.NdexRestClientUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs enrichment 
 * @author churas
 */
public class BasicEnrichmentEngineImpl implements EnrichmentEngine {

    static Logger _logger = LoggerFactory.getLogger(BasicEnrichmentEngineImpl.class);

    private String _dbDir;
    private String _taskDir;
    private boolean _shutdown;
    
    /**
     * This should be a map of <query UUID> => EnrichmentQuery object
     */
    private ConcurrentHashMap<String, EnrichmentQuery> _queryTasks;
    
    private ConcurrentLinkedQueue<String> _queryTaskIds;
    
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
    
    private long _threadSleep = 10;
    
    public BasicEnrichmentEngineImpl(final String dbDir,
            final String taskDir,
            NdexRestClientModelAccessLayer client){
        _shutdown = false;
        _dbDir = dbDir;
        _taskDir = taskDir;
        _client = client;
        _queryTasks = new ConcurrentHashMap<>();
        _queryResults = new ConcurrentHashMap<>();
        _databaseResults = new AtomicReference<>();
        _queryTaskIds = new ConcurrentLinkedQueue<>();
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
        while(_shutdown == false){
            String id = _queryTaskIds.poll();
            if (id == null){
                threadSleep();
                continue;
            }
            processQuery(id,_queryTasks.remove(id));            
        }
        _logger.debug("Shutdown was invoked");
    }

    @Override
    public void shutdown() {
        _shutdown = true;
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
    
    public void setDatabaseResults(DatabaseResults dr){
        _databaseResults.set(dr);
    }

    protected List<String> getUniqueGeneList(List<String> geneList){
        if (geneList == null){
            return null;
        }
        HashSet<String> uniqueGenes = new HashSet<>();
        for (String gene : geneList){
            uniqueGenes.add(gene.toUpperCase());
        }
        return new LinkedList<String>(uniqueGenes);
    }
    /**
     * Runs enrichment on query storing results in _queryResults and _queryStatus
     * @param id 
     */
    protected void processQuery(final String id, EnrichmentQuery query){
        
        File taskDir = new File(this._taskDir + File.separator + id);
        taskDir.mkdirs();
        
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
            enrichmentResult.addAll(getEnrichmentQueryResultObjectsFromNetworkMap(taskDir, dbres, networkMap, uniqueGeneList));
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
    protected List<EnrichmentQueryResult> getEnrichmentQueryResultObjectsFromNetworkMap(File taskDir, DatabaseResult dbres,
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
        int numGenesInQuery = uniqueGeneList.size();
        List<EnrichmentQueryResult> eqrList = new LinkedList<EnrichmentQueryResult>();
        for (String network : networkMap.keySet()){
            EnrichmentQueryResult eqr = new EnrichmentQueryResult();
            eqr.setDatabaseName(dbres.getName());
            eqr.setDatabaseUUID(dbres.getUuid());
            eqr.setHitGenes(new LinkedList<String>(networkMap.get(network)));
            eqr.setNetworkUUID(network);
            NiceCXNetwork cxNetwork = getNetwork(dbres.getUuid(), network);
            if (cxNetwork == null){
                _logger.error("Unable to get network: " + network + " skipping...");
                continue;
            }
            updateStatsAboutNetwork(cxNetwork, eqr, numGenesInQuery);
            eqrList.add(eqr);
        }
        return eqrList;
    }
    
    public void annotateAndSaveNetwork(File destFile, NiceCXNetwork cxNetwork, EnrichmentQueryResult eqr){
        try {
           
            NdexCXNetworkWriter writer = new NdexCXNetworkWriter(new FileOutputStream(destFile), false);
            
        }
        catch(IOException ex){
            _logger.error("problems writing cx", ex);
        }
    }
    
    /**
     * TODO FIX THIS, NEED TO FIGURE OUT WHAT the arguments are supposed to be...
     * @param totalGenesInNetwork
     * @param numberGenesInQuery
     * @param numGenesMatch
     * @return 
     */
    protected double getPvalue(int totalGenesInNetwork, int numberGenesInQuery, int numGenesMatch){
        HypergeometricDistribution hd = new HypergeometricDistribution(2500, 
                                                                       totalGenesInNetwork, numberGenesInQuery);
        return ((double)1.0 - hd.cumulativeProbability(numGenesMatch));
    }
    protected void updateStatsAboutNetwork(NiceCXNetwork cxNetwork, EnrichmentQueryResult eqr,
            int numGenesInQuery){
        eqr.setNodes(cxNetwork.getNodes().size());
        eqr.setEdges(cxNetwork.getEdges().size());
        int numHitGenes = eqr.getHitGenes().size();
        eqr.setPercentOverlap(Math.round(((float)numHitGenes/(float)numGenesInQuery)*(float)100));
        eqr.setpValue(getPvalue(eqr.getNodes(), numGenesInQuery, numHitGenes));
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
        _queryTasks.put(id, thequery);
        _queryTaskIds.add(id);
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
