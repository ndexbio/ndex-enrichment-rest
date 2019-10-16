/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.engine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.*;
import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.cxio.aspects.datamodels.NetworkAttributesElement;
import org.ndexbio.cxio.aspects.datamodels.NodesElement;
import org.ndexbio.enrichment.rest.model.DatabaseResult;

import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.enrichment.rest.services.Configuration;
import org.ndexbio.enrichment.rest.services.Enrichment;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryStatus;
import org.ndexbio.enrichment.rest.model.ErrorResponse;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.model.cx.NiceCXNetwork;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author churas
 */
public class TestBasicEnrichmentEngineImpl {
    
    public Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
    
    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();
    
    @Test
    public void testAddGeneToDatabaseViaremapNetworksToGenes(){
        BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null);
        enricher.addGeneToDatabase("db1", "gene", Arrays.asList("network1", "network2"));
        HashMap<String, HashSet<String>> netMap = enricher.remapNetworksToGenes("db1", new TreeSet<>(Arrays.asList("GENE")));
        assertTrue(netMap.containsKey("network1"));
        assertTrue(netMap.containsKey("network2"));
    }
    
    @Test
    public void testremapNetworksToGenesDatabaseNotFound(){
        BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null);
        enricher.addGeneToDatabase("db1", "gene", Arrays.asList("network1", "network2"));
        assertNull(enricher.remapNetworksToGenes("dbnotfound", new TreeSet<>(Arrays.asList("GENE"))));
    }
    
    @Test
    public void testgetEnrichmentQueryResultObjectsFromNetworkMap(){
        BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null);
        List<EnrichmentQueryResult> eqr = null;
        
        // null check
        eqr = enricher.getEnrichmentQueryResultObjectsFromNetworkMap(null, null, null, null);
        assertEquals(null, eqr);
        
        // another null check
        DatabaseResult dbres = new DatabaseResult();
        eqr = enricher.getEnrichmentQueryResultObjectsFromNetworkMap(null, dbres, null, null);
        assertEquals(null, eqr);
        /**
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
        */
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
        SortedSet<String> genelist = new TreeSet<String>();
        genelist.add("YO");
        genelist.add("TP53");
        genelist.add("ALPHA");
        res = enricher.remapNetworksToGenes("ncipid", genelist);
        assertEquals(4, res.size());
        assertEquals(2, res.get("uuid1").size());
        assertTrue(res.get("uuid1").contains("TP53"));
        assertTrue(res.get("uuid1").contains("ALPHA"));
        
        assertEquals(1, res.get("uuid2").size());
        assertTrue(res.get("uuid2").contains("TP53"));
        
        assertEquals(1, res.get("uuid3").size());
        assertTrue(res.get("uuid3").contains("TP53"));
        
        assertEquals(1, res.get("uuid4").size());
        assertTrue(res.get("uuid4").contains("ALPHA"));
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
  	
  	  InternalDatabaseResults dres = new InternalDatabaseResults();
  	  DatabaseResult dr = new DatabaseResult();
  	  dr.setDescription("ncipid networks haha");
  	  dr.setName("ncipid");
  	  //dr.setNumberOfNetworks("4");
  	  dr.setUuid("ncipid1234");
  	
  	  LinkedList<DatabaseResult> drlist = new LinkedList<>();
  	  drlist.add(dr);
  	
  	  dr = new DatabaseResult();
  	  dr.setDescription("signor networks haha");
  	  dr.setName("signor");
  	  //dr.setNumberOfNetworks("1");
  	  dr.setUuid("signor1234");
  	  drlist.add(dr);
  	  dres.setResults(drlist);
  	  enricher.setDatabaseResults(dres);
  	
  	  EnrichmentQuery query = new EnrichmentQuery();
  	  query.setDatabaseList(new TreeSet<>(Arrays.asList("ncipid", "signor")));
  	  query.setGeneList(new TreeSet<>(Arrays.asList("tp53", "beta", "alpha")));
  	
  	  enricher.processQuery("12345", query);
        EnrichmentQueryResults eqr = enricher.getQueryResults("12345", 0, 0);
        /*
        assertEquals(6, eqr.getNumberOfHits());
        assertEquals(EnrichmentQueryResults.COMPLETE_STATUS, eqr.getStatus());
        assertEquals(100, eqr.getProgress());
        List<EnrichmentQueryResult> eqlist = eqr.getResults();
        assertEquals(6, eqlist);
        assertEquals(6, eqlist.size());
        
        HashSet<String> netSet = new HashSet<>();
        for (EnrichmentQueryResult eRes : eqlist){
            if (eRes.getNetworkUUID().equals("uuid1")){
                assertEquals("ncipid", eRes.getDatabaseName());
                assertEquals("ncipid1234", eRes.getDatabaseUUID());
                assertEquals(2, eRes.getHitGenes().size());
                assertTrue(eRes.getHitGenes().contains("TP53"));
                assertTrue(eRes.getHitGenes().contains("ALPHA"));
            }
            netSet.add(eRes.getNetworkUUID());
        }
        assertEquals(6, netSet.size());
        assertTrue(netSet.containsAll(Arrays.asList("uuid1", "uuid2", "uuid3",
                "uuid5", "uuid6", "uuid7")));  */
    }
    
    @Test
    public void testQueryNoDatabases() throws ExecutionException{
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
    public void testQuerySuccess() throws EnrichmentException, ExecutionException {
        BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null);
        EnrichmentQuery eq = new EnrichmentQuery();
        eq.setDatabaseList(new TreeSet<>(Arrays.asList("ncipid")));
        eq.setGeneList(new TreeSet<>(Arrays.asList("brca1")));
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
    
    @Test
    public void testGetPvalue(){
        BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null);
        assertEquals(0.076, enricher.getPvalue(100, 10, 5, 1), 0.01);
        
    }
    
    @Test
    public void testUpdateStatsAboutNetwork(){
        NiceCXNetwork net = new NiceCXNetwork();
        NetworkAttributesElement nameAttrib = new NetworkAttributesElement(0L, "name", "mynetwork");
        net.addNetworkAttribute(nameAttrib);
        NodesElement ne = new NodesElement();
        ne.setId(0);
        ne.setNodeName("node1");
        net.addNode(ne);
        ne = new NodesElement();
        ne.setId(1);
        ne.setNodeName("node2");
        net.addNode(ne);
        
        BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null);
        InternalDatabaseResults idr = new InternalDatabaseResults();
        idr.setUniverseUniqueGeneCount(100);
        enricher.setDatabaseResults(idr);
        EnrichmentQueryResult eqr = new EnrichmentQueryResult();
        HashSet<String> geneSet = new HashSet<>();
        geneSet.add("gene1");
        geneSet.add("gene2");
        eqr.setHitGenes(geneSet);
        /*enricher.updateStatsAboutNetwork(net, eqr, 4);
        
        assertEquals(50, eqr.getPercentOverlap());
        assertEquals(2, eqr.getNodes());
        assertEquals(0, eqr.getEdges());
        assertEquals("mynetwork", eqr.getDescription());
        assertEquals(0.076, enricher.getPvalue(100, 10, 5, 1), 0.01);
    */}
    
  @Test
  public void testUpdateEnrichmentQueryResultsInDb() {
	  BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null);
	  EnrichmentQuery eq = new EnrichmentQuery();
	  eq.setDatabaseList(new TreeSet<>(Arrays.asList("ncipid")));
	  eq.setGeneList(new TreeSet<>(Arrays.asList("aldoa")));
	  try {
		//Set wrong values
		String id = enricher.query(eq);
		EnrichmentQueryResults eqrs = enricher.getEnrichmentQueryResultsFromDb(id);
		eqrs.setProgress(0);
		eqrs.setStatus("wrong status");
		eqrs.setNumberOfHits(0);
		List<EnrichmentQueryResult> eqrList1 = new LinkedList<>();
		EnrichmentQueryResult eqr1 = new EnrichmentQueryResult();
		eqr1.setNetworkUUID("wrong network");
		eqrList1.add(eqr1);
		eqrs.setResults(eqrList1);
		eqrs.setWallTime(0);
		
		//Set correct values
		List<EnrichmentQueryResult> eqrList2 = new LinkedList<>();
		EnrichmentQueryResult eqr2 = new EnrichmentQueryResult();
		eqr2.setNetworkUUID("right network 1");
		eqrList2.add(eqr2);
		EnrichmentQueryResult eqr3 = new EnrichmentQueryResult();
		eqr3.setNetworkUUID("right network 2");
		eqrList2.add(eqr3);
		
		//Update results
		enricher.updateEnrichmentQueryResultsInDb(id, "right status", 100, eqrList2);
		
		//Check results
		EnrichmentQueryResults newEqrs = enricher.getEnrichmentQueryResultsFromDb(id);
		assertEquals(100, newEqrs.getProgress());
		assertEquals("right status", newEqrs.getStatus());
		assertEquals(2, newEqrs.getNumberOfHits());
		assertEquals("right network 1", newEqrs.getResults().get(0).getNetworkUUID());
		assertEquals("right network 2", newEqrs.getResults().get(1).getNetworkUUID());

	} catch (EnrichmentException e) {
		fail();
		e.printStackTrace();
	} catch (ExecutionException e) {
		fail();
		e.printStackTrace();
	}
  }
  
  @Test
  public void testgetQueryResultsNoResult() {
	  BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null);
	  try {
		assertEquals(null, enricher.getQueryResults("12345", 0, 0));
	} catch (EnrichmentException e) {
		fail();
		e.printStackTrace();
	}
  }
  
  @Test
  public void testDeleteFromDb() throws Exception{
	  BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null);
	  EnrichmentQuery eq = new EnrichmentQuery();
	  eq.setDatabaseList(new TreeSet<>(Arrays.asList("ncipid")));
	  eq.setGeneList(new TreeSet<>(Arrays.asList("aldoa")));
	  String id = enricher.query(eq);
	  
	  EnrichmentQueryResults oldEqrs = enricher.getEnrichmentQueryResultsFromDb(id);
	  oldEqrs.setStatus("wrong results");
	  EnrichmentQueryResults oldEqrs2 = enricher.getEnrichmentQueryResultsFromDb(id);
	  assertTrue(oldEqrs2.getStatus().equals("wrong results") );
	  
	  enricher.delete(id);
	  EnrichmentQueryResults newEqrs = enricher.getEnrichmentQueryResultsFromDb(id);
	  assertEquals(newEqrs.getStatus(), null);
  }
  
  @Test
  public void testcacheDelete() throws Exception {
	  try {
		File tempDbDir = _folder.newFolder();
		File tempTaskDir = _folder.newFolder();
		BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(tempDbDir.getAbsolutePath(), tempTaskDir.getAbsolutePath());
		
		EnrichmentQuery eq0 = new EnrichmentQuery();
		eq0.setGeneList(new TreeSet<>(Arrays.asList(Integer.toString(0))));
		eq0.setDatabaseList(new TreeSet<>(Arrays.asList("ncipid")));
		String id0 = enricher.query(eq0);
		EnrichmentQueryResults eqr0 = enricher.getEnrichmentQueryResultsFromDb(id0);
		eqr0.setStatus("not deleted");
		
		for (int i = 1; i < 600; i++) {
			EnrichmentQuery eq = new EnrichmentQuery();
			eq.setGeneList(new TreeSet<>(Arrays.asList(Integer.toString(i))));
			eq.setDatabaseList(new TreeSet<>(Arrays.asList("ncipid")));
			enricher.query(eq);
		}
		
		EnrichmentQuery eq601 = new EnrichmentQuery();
		eq601.setGeneList(new TreeSet<>(Arrays.asList(Integer.toString(601))));
		eq601.setDatabaseList(new TreeSet<>(Arrays.asList("ncipid")));
		String id601 = enricher.query(eq601);
		EnrichmentQueryResults eqr601 = enricher.getEnrichmentQueryResultsFromDb(id601);
		eqr601.setStatus("not deleted");
		
		EnrichmentQueryResults eqr0New = enricher.getEnrichmentQueryResultsFromDb(id0);
		assertFalse(eqr0New.getStatus() == "not deleted");
		
		EnrichmentQueryResults eqr601New = enricher.getEnrichmentQueryResultsFromDb(id601);
		assertTrue(eqr601New.getStatus() == "not deleted");
		
	} finally {
		_folder.delete();
	}
	  
  }
}
