package org.ndexbio.enrichment.rest.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.TreeSet;
import org.apache.commons.io.FileUtils;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.ndexbio.enrichment.rest.model.DatabaseResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryStatus;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.ndexsearch.rest.model.DatabaseResult;

/**
 * Tests {@link org.ndexbio.enrichment.rest.client.EnrichmentRestClientImpl}
 * @author churas
 */
public class EnrichmentRestClientImplTest {
    
    public static final String NDEX_SERVER = "NDEX_PATHWAY_RELEVANCE_SERVER";
    
    @Test
    public void testfoo(){
        assertTrue(1 == 1);
    }
    
    @EnabledIfEnvironmentVariable(named=EnrichmentRestClientImplTest.NDEX_SERVER, matches = ".+")
    @Test
    public void testRealQuery() {
        
        EnrichmentRestClientImpl erc = new EnrichmentRestClientImpl(System.getenv(NDEX_SERVER), null);
        //erc.setTimeouts(10, 10);
        try {
            DatabaseResults dr = erc.getDatabaseResults();
            TreeSet<String> dbList = new TreeSet<>();
            for (DatabaseResult dRes : dr.getResults()){
                dbList.add(dRes.getName());
            }
            
            // try a query
            EnrichmentQuery query = new EnrichmentQuery();
            TreeSet<String> geneList = new TreeSet<>();
            geneList.add("tp53");
            geneList.add("mtor");
            query.setGeneList(geneList);
            query.setDatabaseList(dbList);
            String taskid = erc.query(query);
            assertNotNull(taskid);
            
            try{
                    Thread.sleep(500);
                } catch(InterruptedException ie){
                    // do nothing
                }
            
            EnrichmentQueryStatus eqs = erc.getQueryStatus(taskid);
            while (eqs == null || eqs.getProgress() != 100){
                try{
                    Thread.sleep(500);
                } catch(InterruptedException ie){
                    // do nothing
                }
                
                eqs = erc.getQueryStatus(taskid);
            }
            
            assertEquals("complete", eqs.getStatus());
            
            EnrichmentQueryResults eqr = erc.getQueryResults(taskid, 0, 0);
            assertTrue(eqr.getNumberOfHits() > 0);
            EnrichmentQueryResult result = eqr.getResults().get(0);
            
            InputStream in = erc.getNetworkOverlayAsCX(taskid, result.getDatabaseUUID(), result.getNetworkUUID());
            assertNotNull(in);
            erc.delete(taskid);
            
        } catch(EnrichmentException ee){
            fail("got exception: " + ee.getMessage());
        } 
    }
    
}
