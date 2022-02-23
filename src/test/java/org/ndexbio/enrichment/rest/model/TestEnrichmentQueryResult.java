package org.ndexbio.enrichment.rest.model;

import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author churas
 */
public class TestEnrichmentQueryResult {
	
	@Test
	public void testGettersAndSetters(){
		EnrichmentQueryResult eqr = new EnrichmentQueryResult();
		assertNull(eqr.getDatabaseName());
		assertNull(eqr.getDatabaseUUID());
		assertNull(eqr.getDescription());
		assertEquals(0, eqr.getEdges());
		assertNull(eqr.getHitGenes());
		assertNull(eqr.getImageURL());
		assertNull(eqr.getNetworkUUID());
		assertEquals(0, eqr.getNodes());
		assertEquals(0, eqr.getPercentOverlap());
		assertEquals(0, eqr.getRank());
		assertEquals(0.0, eqr.getSimilarity());
		assertEquals(0, eqr.getTotalNetworkCount());
		assertNull(eqr.getUrl());
		assertEquals(0, eqr.getpValue());
		
		eqr.setDatabaseName("database");
		eqr.setDatabaseUUID("dbuuid");
		eqr.setDescription("description");
		eqr.setEdges(1);
		Set<String> hitGenes = new HashSet<>();
		hitGenes.add("gene1");
		eqr.setHitGenes(hitGenes);
		eqr.setImageURL("imageurl");
		eqr.setNetworkUUID("networkuuid");
		eqr.setNodes(2);
		eqr.setPercentOverlap(3);
		eqr.setRank(4);
		eqr.setSimilarity(5.0);
		eqr.setTotalNetworkCount(6);
		eqr.setUrl("url");
		eqr.setpValue(7.0);
		
		assertEquals("database", eqr.getDatabaseName());
		assertEquals("dbuuid", eqr.getDatabaseUUID());
		assertEquals("description", eqr.getDescription());
		assertEquals(1, eqr.getEdges());
		assertEquals(1, eqr.getHitGenes().size());
		assertTrue(eqr.getHitGenes().contains("gene1"));
		assertEquals("imageurl", eqr.getImageURL());
		assertEquals("networkuuid", eqr.getNetworkUUID());
		assertEquals(2, eqr.getNodes());
		assertEquals(3, eqr.getPercentOverlap());
		assertEquals(4, eqr.getRank());
		assertEquals(5.0, eqr.getSimilarity());
		assertEquals(6, eqr.getTotalNetworkCount());
		assertEquals("url", eqr.getUrl());
		assertEquals(7.0, eqr.getpValue());
		
		
				
		
	}
}
