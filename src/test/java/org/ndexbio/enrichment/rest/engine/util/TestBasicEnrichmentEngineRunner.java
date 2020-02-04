package org.ndexbio.enrichment.rest.engine.util;

import org.junit.Test;
import static org.junit.Assert.*;
/**
 *
 * @author churas
 */
public class TestBasicEnrichmentEngineRunner {
	
	@Test
	public void testFoo(){
		assertEquals(1, 1);
	}
	
	/**
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
                "uuid5", "uuid6", "uuid7")));  *//*
    }
    */
	
	/**
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
        
        BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null, null);
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
    }*/
	
		/**
  @Test
  public void testUpdateEnrichmentQueryResultsInDb() {
	  BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null, null);
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
  */
}
