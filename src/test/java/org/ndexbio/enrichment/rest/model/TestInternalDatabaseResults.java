package org.ndexbio.enrichment.rest.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.ndexbio.ndexsearch.rest.model.DatabaseResult;

import static org.junit.jupiter.api.Assertions.*;
/**
 *
 * @author churas
 */
public class TestInternalDatabaseResults {
    
    @Test
    public void testGettersAndSetters(){
        InternalDatabaseResults idr = new InternalDatabaseResults();
        assertEquals(null, idr.getDatabaseConnectionMap());
        assertEquals(null, idr.getDatabaseUniqueGeneCount());
        assertEquals(null, idr.getGeneMapList());
        assertEquals(null, idr.getNetworksToExclude());
        assertEquals(null, idr.getResults());
        assertEquals(0, idr.getUniverseUniqueGeneCount());
		assertNull(idr.getIdfMap());
		assertNull(idr.getNetworkGeneList());
		assertNull(idr.getNetworkToGeneToNodeMap());
		assertEquals(0, idr.getTotalNetworkCount());
        
        Map<String, InternalNdexConnectionParams> conMap = new HashMap<String, InternalNdexConnectionParams>();
        conMap.put("hi", new InternalNdexConnectionParams());
        idr.setDatabaseConnectionMap(conMap);
        Map<String, Integer> geneCount = new HashMap<String, Integer>();
        geneCount.put("key", 1);
        idr.setDatabaseUniqueGeneCount(geneCount);
       
        
        idr.setGeneMapList(new ArrayList<InternalGeneMap>());
        
        Set<String> exclude = new HashSet<>();
        exclude.add("exclude");
        idr.setNetworksToExclude(exclude);
        
        idr.setUniverseUniqueGeneCount(5);
		idr.setTotalNetworkCount(6);
		
		Map<String, Map<String, Set<Long>>> netToGeneMap = new HashMap<>();
		idr.setNetworkToGeneToNodeMap(netToGeneMap);
		Map<String, Double> idfMap = new HashMap<>();
        idr.setIdfMap(idfMap);
		Map<String, Set<String>> networkGeneList = new HashMap<>();
		idr.setNetworkGeneList(networkGeneList);
        assertTrue(idr.getDatabaseConnectionMap().containsKey("hi"));
        assertTrue(idr.getDatabaseUniqueGeneCount().containsKey("key"));
        assertEquals(0, idr.getGeneMapList().size());
        assertTrue(idr.getNetworksToExclude().contains("exclude"));
        idr.setResults(new ArrayList<>());
        assertEquals(0, idr.getResults().size());
        assertEquals(5, idr.getUniverseUniqueGeneCount());
		assertEquals(6, idr.getTotalNetworkCount());
		assertEquals(netToGeneMap, idr.getNetworkToGeneToNodeMap());
		assertEquals(idfMap, idr.getIdfMap());
		assertEquals(networkGeneList, idr.getNetworkGeneList());
    }
}
