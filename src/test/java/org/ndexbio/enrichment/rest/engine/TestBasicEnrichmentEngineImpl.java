/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.engine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.ndexbio.enrichment.rest.exceptions.EnrichmentException;
import org.ndexbio.enrichment.rest.model.DatabaseResult;
import org.ndexbio.enrichment.rest.model.DatabaseResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryStatus;

/**
 *
 * @author churas
 */
public class TestBasicEnrichmentEngineImpl {
    
    public Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
    
    
    public TestBasicEnrichmentEngineImpl() {
    }
   
    @Test
    public void testGetUniqueGeneList() throws Exception {
        BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null);
        assertEquals(null, enricher.getUniqueGeneList(null));
        List<String> mylist = new LinkedList<String>();
        assertTrue(enricher.getUniqueGeneList(mylist).isEmpty());
        mylist.add("1");
        mylist.add("1");
        mylist.add("2");
        assertEquals(2, enricher.getUniqueGeneList(mylist).size());
        assertTrue(enricher.getUniqueGeneList(mylist).contains("1"));
        assertTrue(enricher.getUniqueGeneList(mylist).contains("2"));
    }
    
    @Test
    public void testgetEnrichmentQueryResultObjectsFromNetworkMap(){
        BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null);
        List<EnrichmentQueryResult> eqr = null;
        
        // null check
        eqr = enricher.getEnrichmentQueryResultObjectsFromNetworkMap(null, null, null);
        assertEquals(null, eqr);
        
        // another null check
        DatabaseResult dbres = new DatabaseResult();
        eqr = enricher.getEnrichmentQueryResultObjectsFromNetworkMap(dbres, null, null);
        assertEquals(null, eqr);
        
        // another null check
        HashMap<String, HashSet<String>> networkMap = new HashMap<String, HashSet<String>>();
        eqr = enricher.getEnrichmentQueryResultObjectsFromNetworkMap(dbres, networkMap, null);
        assertEquals(null, eqr);
        
        // test with 1 network and 1 gene
        List<String> geneList = new LinkedList<String>();
        geneList.add("tp53");
        dbres.setDescription("description");
        dbres.setName("ncipid");
        dbres.setUuid("uuid");
        HashSet<String> genes = new HashSet<String>(geneList);
        networkMap.put("network1", genes);
        eqr = enricher.getEnrichmentQueryResultObjectsFromNetworkMap(dbres, networkMap, geneList);
        assertEquals(1, eqr.size());
        EnrichmentQueryResult first = eqr.get(0);
        assertEquals("ncipid", first.getDatabaseName());
        assertEquals("uuid", first.getDatabaseUUID());
        assertEquals(0, first.getPercentOverlap());
        assertEquals(0, first.getRank());
        assertEquals(0, first.getpValue());
        assertEquals("network1", first.getNetworkUUID());
        assertEquals("tp53", first.getHitGenes().get(0));   
    }
    
    @Test
    public void testremapNetworksToGenes(){
        BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null);
        
        LinkedList<String> networktp = new LinkedList<String>();
        networktp.add("uuid1");
        networktp.add("uuid2");
        networktp.add("uuid3");
        enricher.addGeneToDatabase("ncipid", "tp53", networktp);
        
        networktp = new LinkedList<String>();
        networktp.add("uuid1");
        networktp.add("uuid4");
        enricher.addGeneToDatabase("ncipid", "alpha", networktp);
        
        networktp = new LinkedList<String>();
        networktp.add("uuid5");
        enricher.addGeneToDatabase("ncipid", "beta", networktp);
        
        HashMap<String, HashSet<String>> res = null;
        List<String> genelist = new LinkedList<String>();
        genelist.add("yo");
        genelist.add("tp53");
        genelist.add("alpha");
        res = enricher.remapNetworksToGenes("ncipid", genelist);
        assertEquals(4, res.size());
        assertEquals(2, res.get("uuid1").size());
        assertTrue(res.get("uuid1").contains("tp53"));
        assertTrue(res.get("uuid1").contains("alpha"));
        
        assertEquals(1, res.get("uuid2").size());
        assertTrue(res.get("uuid2").contains("tp53"));
        
        assertEquals(1, res.get("uuid3").size());
        assertTrue(res.get("uuid3").contains("tp53"));
        
        assertEquals(1, res.get("uuid4").size());
        assertTrue(res.get("uuid4").contains("alpha"));
    }
    
    @Test
    public void testprocessQuery() throws Exception{
        BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null);
        
        List<String> networktp = new LinkedList<String>();
        networktp.add("uuid1");
        networktp.add("uuid2");
        networktp.add("uuid3");
        enricher.addGeneToDatabase("ncipid1234", "tp53", networktp);
        
        networktp = new LinkedList<String>();
        networktp.add("uuid1");
        networktp.add("uuid3");
        enricher.addGeneToDatabase("ncipid1234", "alpha", networktp);
        
        networktp = new LinkedList<String>();
        networktp.add("uuid5");
        networktp.add("uuid6");
        networktp.add("uuid7");
        enricher.addGeneToDatabase("signor1234", "beta", networktp);
        
        DatabaseResults dres = new DatabaseResults();
        DatabaseResult dr = new DatabaseResult();
        dr.setDescription("ncipid networks haha");
        dr.setName("ncipid");
        dr.setNumberOfNetworks("4");
        dr.setUuid("ncipid1234");
        
        LinkedList<DatabaseResult> drlist = new LinkedList<>();
        drlist.add(dr);
        
        dr = new DatabaseResult();
        dr.setDescription("signor networks haha");
        dr.setName("signor");
        dr.setNumberOfNetworks("1");
        dr.setUuid("signor1234");
        drlist.add(dr);
        dres.setResults(drlist);
        enricher.setDatabaseResults(dres);
        
        EnrichmentQuery query = new EnrichmentQuery();
        query.setDatabaseList(Arrays.asList("ncipid", "signor"));
        query.setGeneList(Arrays.asList("tp53", "beta", "alpha"));
        
        enricher.processQuery("12345", query);
        EnrichmentQueryResults eqr = enricher.getQueryResults("12345", 0, 0);
        
        assertEquals(6,eqr.getNumberOfHits());
        assertEquals(EnrichmentQueryResults.COMPLETE_STATUS, eqr.getStatus());
        assertEquals(100, eqr.getProgress());
        List<EnrichmentQueryResult> eqlist = eqr.getResults();
        assertEquals(6, eqlist.size());
        
        HashSet<String> netSet = new HashSet<>();
        for (EnrichmentQueryResult eRes : eqlist){
            if (eRes.getNetworkUUID().equals("uuid1")){
                assertEquals("ncipid", eRes.getDatabaseName());
                assertEquals("ncipid1234", eRes.getDatabaseUUID());
                assertEquals(2, eRes.getHitGenes().size());
                assertTrue(eRes.getHitGenes().contains("tp53"));
                assertTrue(eRes.getHitGenes().contains("alpha"));
            }
            netSet.add(eRes.getNetworkUUID());
        }
        assertEquals(6, netSet.size());
        assertTrue(netSet.containsAll(Arrays.asList("uuid1", "uuid2", "uuid3",
                "uuid5", "uuid6", "uuid7")));        
    }
    
    @Test
    public void testQueryNoDatabases(){
        try{
            BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null);
            EnrichmentQuery eq = new EnrichmentQuery();
            enricher.query(eq);
            fail("Expected exception");
        }
        catch(EnrichmentException ee){
            assertTrue(ee.getMessage().equals("No databases selected"));
        }
    }
    
    @Test
    public void testQuerySuccess() throws EnrichmentException {
        BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null);
        EnrichmentQuery eq = new EnrichmentQuery();
        eq.setDatabaseList(Arrays.asList("ncipid"));
        eq.setGeneList(Arrays.asList("brca1"));
        String res = enricher.query(eq);
        assertTrue(res != null);
        
        EnrichmentQueryResults eqr = enricher.getQueryResults(res, 0, 0);
        assertTrue(eqr != null);
        assertEquals(EnrichmentQueryResults.SUBMITTED_STATUS, eqr.getStatus());
        assertTrue(eqr.getStartTime() > 0);
        
        EnrichmentQueryStatus eqs = enricher.getQueryStatus(res);
        assertEquals(EnrichmentQueryResults.SUBMITTED_STATUS, eqs.getStatus());
        assertEquals(eqr.getStartTime(), eqs.getStartTime());
        
    }
}
