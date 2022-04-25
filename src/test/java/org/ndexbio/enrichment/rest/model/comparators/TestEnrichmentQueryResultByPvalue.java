package org.ndexbio.enrichment.rest.model.comparators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
/**
 * Tests EnrichmentQueryResultByValue
 * @author churas
 */
public class TestEnrichmentQueryResultByPvalue {
    
    @Test
    public void testSortingWithNullParameters(){
        EnrichmentQueryResultByPvalue sorter = new EnrichmentQueryResultByPvalue();
        assertEquals(0, sorter.compare(null, null));
        assertEquals(-1, sorter.compare(new EnrichmentQueryResult(), null));
        assertEquals(1, sorter.compare(null, new EnrichmentQueryResult()));
    }
    
    @Test
    public void testVariousPvalues(){
        EnrichmentQueryResultByPvalue sorter = new EnrichmentQueryResultByPvalue();
        EnrichmentQueryResult o1 = new EnrichmentQueryResult();
        o1.setpValue(0);
        o1.setPercentOverlap(0);
        EnrichmentQueryResult o2 = new EnrichmentQueryResult();
        o2.setpValue(0);
		o2.setPercentOverlap(0);

		o1.setDescription("a");
		o2.setDescription("a");
		// equals
        assertEquals(0, sorter.compare(o1, o2));
      
		// same pvalue, same overlap diff description
		o1.setDescription("a");
		o2.setDescription("b");
		assertEquals(-1, sorter.compare(o1, o2));
		assertEquals(1, sorter.compare(o2, o1));

		
        o1.setpValue(0.0001);
        o2.setpValue(0.001);
        //o1 less then o2
        assertEquals(-1, sorter.compare(o1, o2));
        //flip o1 is still less then o2, but put as 2nd argument
        assertEquals(1, sorter.compare(o2, o1));
		
		// same pvalue but different overlap
		o1.setpValue(0.0);
		o2.setpValue(0.0);
		o1.setPercentOverlap(2);
		o1.setPercentOverlap(1);
		
		assertEquals(-1, sorter.compare(o1, o2));
		assertEquals(1, sorter.compare(o2, o1));
		
		// same pvalue, same overlap diff description
		
		
	}    
    

}
