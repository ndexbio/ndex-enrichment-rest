package org.ndexbio.enrichment.rest.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/**
 *
 * @author churas
 */
public class TestInternalGeneMap {
    
    @Test
    public void testGettersAndSetters(){
        InternalGeneMap igm = new InternalGeneMap();
        assertEquals(null, igm.getDatabaseUUID());
        assertEquals(null, igm.getGeneMap());
        igm.setDatabaseUUID("hi");
        
        Map<String, Set<String>> geneMap = new HashMap<>();
        geneMap.put("key", new HashSet<String>());
        igm.setGeneMap(geneMap);
        assertEquals("hi", igm.getDatabaseUUID());
        assertTrue(igm.getGeneMap().containsKey("key"));
        
        

    }
}
