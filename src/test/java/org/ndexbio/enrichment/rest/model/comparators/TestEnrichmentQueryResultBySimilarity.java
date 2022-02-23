package org.ndexbio.enrichment.rest.model.comparators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
/**
 * Tests EnrichmentQueryResultByValue
 * @author churas
 */
public class TestEnrichmentQueryResultBySimilarity {
    
    @Test
    public void testSortingWithNullParameters(){
        EnrichmentQueryResultBySimilarity sorter = new EnrichmentQueryResultBySimilarity();
        assertEquals(0, sorter.compare(null, null));
        assertEquals(-1, sorter.compare(new EnrichmentQueryResult(), null));
        assertEquals(1, sorter.compare(null, new EnrichmentQueryResult()));
    }
    
    @Test
    public void testVariousSimilarityValues(){
        EnrichmentQueryResultBySimilarity sorter = new EnrichmentQueryResultBySimilarity();
        EnrichmentQueryResult o1 = new EnrichmentQueryResult();
        o1.setSimilarity(0);
		o1.setpValue(0);
        
        EnrichmentQueryResult o2 = new EnrichmentQueryResult();
        o2.setSimilarity(0);
		o2.setpValue(0);
        
        // equals
        assertEquals(0, sorter.compare(o1, o2));
        
        o1.setSimilarity(0.001);
        o2.setSimilarity(0.0001);
        //o1 less then o2
        assertEquals(-1, sorter.compare(o1, o2));
        //flip o1 is still less then o2, but put as 2nd argument
        assertEquals(1, sorter.compare(o2, o1));
	
		//compare where similarity is the same, but pvalues differ
		o1.setSimilarity(0);
		o1.setpValue(0.1);
		o2.setSimilarity(0);
		o2.setpValue(0.001);
		assertEquals(1, sorter.compare(o1, o2));
		assertEquals(-1, sorter.compare(o2, o1));
        
    }

}
