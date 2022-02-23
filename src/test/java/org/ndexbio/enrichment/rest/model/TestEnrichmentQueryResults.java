package org.ndexbio.enrichment.rest.model;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/**
 *
 * @author churas
 */
public class TestEnrichmentQueryResults {
    
    @Test
    public void testConstructorWithStartTime(){
        EnrichmentQueryResults eqr = new EnrichmentQueryResults(1);
        assertEquals(1, eqr.getStartTime());
    }
    
    @Test
    public void testUpdateStartTime(){
        EnrichmentQueryResults eqr = new EnrichmentQueryResults(1);
        assertEquals(1, eqr.getStartTime());
        EnrichmentQueryResults updated = new EnrichmentQueryResults();
        assertEquals(0, updated.getStartTime());
        
        updated.updateStartTime(eqr);
        assertEquals(1, updated.getStartTime());
        
        // try null copy which doesnt change anything
        updated.updateStartTime(null);
        assertEquals(1, updated.getStartTime());
        
    }
    
    @Test
    public void testOverloadedCopyConstructor(){
        EnrichmentQueryResults eqr = new EnrichmentQueryResults();
        eqr.setNumberOfHits(1);
        eqr.setSize(3);
        eqr.setStart(4);
        
        EnrichmentQueryResults updated = new EnrichmentQueryResults(eqr, new ArrayList<EnrichmentQueryResult>());
        assertEquals(1, updated.getNumberOfHits());
        assertEquals(3, updated.getSize());
        assertEquals(4, updated.getStart());
        assertEquals(0, updated.getResults().size());
        
        
        
        
    }
    
    @Test
    public void testGettersAndSetters(){
        EnrichmentQueryResults eqr = new EnrichmentQueryResults();
        assertEquals(null, eqr.getMessage());
        assertEquals(0, eqr.getNumberOfHits());
        assertEquals(0, eqr.getProgress());
        assertEquals(0, eqr.getSize());
        assertEquals(null, eqr.getResults());
        assertEquals(0, eqr.getStart());
        assertEquals(0, eqr.getStartTime());
        assertEquals(null, eqr.getStatus());
        assertEquals(0, eqr.getWallTime());
        
        eqr.setMessage("message");
        eqr.setNumberOfHits(1);
        eqr.setProgress(2);
        eqr.setResults(new ArrayList<EnrichmentQueryResult>());
        eqr.setSize(3);
        eqr.setStart(4);
        eqr.setStartTime(5);
        eqr.setStatus("status");
        eqr.setWallTime(6);
        
        assertEquals(0, eqr.getResults().size());
        assertEquals("message", eqr.getMessage());
        assertEquals(1, eqr.getNumberOfHits());
        assertEquals(2, eqr.getProgress());
        assertEquals(3, eqr.getSize());
        assertEquals(4, eqr.getStart());
        assertEquals(5, eqr.getStartTime());
        assertEquals("status", eqr.getStatus());
        assertEquals(6, eqr.getWallTime());
    }
}
