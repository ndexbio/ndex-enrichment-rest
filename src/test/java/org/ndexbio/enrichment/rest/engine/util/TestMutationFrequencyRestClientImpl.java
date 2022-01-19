package org.ndexbio.enrichment.rest.engine.util;


import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import kong.unirest.HttpMethod;
import kong.unirest.HttpStatus;
import kong.unirest.MockClient;
import kong.unirest.MockResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Test;
import org.ndexbio.enrichment.rest.model.ErrorResponse;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.ndexsearch.rest.model.GeneList;
import org.ndexbio.ndexsearch.rest.model.MutationFrequencies;

/**
 *
 * @author churas
 */
public class TestMutationFrequencyRestClientImpl {
	
    
    /**
     * Tests real end point so keep this ignored unless one wants to hit real service
     * @throws EnrichmentException 
     */
    @Ignore
    public void testRealService() throws EnrichmentException {
        MutationFrequencyRestClientImpl client = null;
        try {
            final String theURL = "https://iquery-cbio-dev.ucsd.edu/integratedsearch/v1/mutationfrequency";
            client = new MutationFrequencyRestClientImpl();
            GeneList geneList = new GeneList();
            geneList.setGenes(Arrays.asList("mtor", "tp53"));
            MutationFrequencies mutFreqs = client.getMutationFrequencies(theURL, geneList);
            assertNotNull(mutFreqs);
            Map<String, Double> freqs = mutFreqs.getMutationFrequencies();
            assertNotNull(freqs);
            assertEquals(2, freqs.size());
            assertTrue(freqs.containsKey("mtor"));
            assertTrue(freqs.containsKey("tp53"));
            assertTrue(freqs.get("mtor") >= 0.0);
            assertTrue(freqs.get("tp53") >= 0.0);
        } finally {
            if (client != null){
                client.shutdown();
            }
        }
    }
    
    @Test
    public void testNotFoundStatusCode() throws Exception {
        MockClient mock = null;
        MutationFrequencyRestClientImpl client = null;
        try {
            final String theURL = "http://doesnotexist";
            client = new MutationFrequencyRestClientImpl();
            mock = MockClient.register(client.getUnirestInstance());
            GeneList geneList = new GeneList();
            geneList.setGenes(Arrays.asList("mtor"));
            ObjectMapper omappy = new ObjectMapper();
            mock.expect(HttpMethod.POST, theURL)
                    .header(HttpHeaders.ACCEPT, MutationFrequencyRestClientImpl.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_TYPE, MutationFrequencyRestClientImpl.APPLICATION_JSON)
                    .body(omappy.writeValueAsString(geneList))
                    .thenReturn(MockResponse.of(HttpStatus.NOT_FOUND,
                            "not found", "body"));
            
            client.getMutationFrequencies(theURL, geneList);
            
            fail("Expected EnrichmentException");
            
        } catch (EnrichmentException ee){
            assertEquals("Received status code: 404 : not found", ee.getMessage());
            if (mock != null){
                mock.verifyAll();
            }
        }
        finally {
            MockClient.clear();
            if (client != null){
                client.shutdown();
            }
        }
    }
    
    @Test
    public void testErrorReturned() throws Exception {
        MockClient mock = null;
        MutationFrequencyRestClientImpl client = null;
         ObjectMapper omappy = new ObjectMapper();
        try {
            final String theURL = "http://doesnotexist";
            client = new MutationFrequencyRestClientImpl("hi");
            mock = MockClient.register(client.getUnirestInstance());
            GeneList geneList = new GeneList();
            geneList.setGenes(Arrays.asList("mtor"));
           
            
            ErrorResponse er = new ErrorResponse("some error", new Exception("problem"));
            
            mock.expect(HttpMethod.POST, theURL)
                    .header(HttpHeaders.ACCEPT, MutationFrequencyRestClientImpl.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_TYPE, MutationFrequencyRestClientImpl.APPLICATION_JSON)
                    .body(omappy.writeValueAsString(geneList))
                    .thenReturn(MockResponse.of(HttpStatus.INTERNAL_SERVER_ERROR,
                            "status text", omappy.writeValueAsString(er)));
            
            client.getMutationFrequencies(theURL, geneList);
            fail("Expected EnrichmentException");
            
        } catch (EnrichmentException ee){
            assertTrue(ee.getMessage().contains("\"description\": \"problem\""));
            assertTrue(ee.getMessage().contains("\"message\": \"some error\""));
            
            if (mock != null){
                mock.verifyAll();
            }
        }
        finally {
            MockClient.clear();
            if (client != null){
                client.shutdown();
            }
        }
    }
    
    @Test
    public void testSuccess() throws Exception {
        MockClient mock = null;
        MutationFrequencyRestClientImpl client = null;
        try {
            final String theURL = "http://doesnotexist";
            client = new MutationFrequencyRestClientImpl("agent");
            client.setTimeouts(0, 0);
            mock = MockClient.register(client.getUnirestInstance());
            GeneList geneList = new GeneList();
            geneList.setGenes(Arrays.asList("mtor"));
            
            MutationFrequencies mutFreqs = new MutationFrequencies();
            Map<String, Double> freqs = new HashMap<>();
            freqs.put("mtor", 1.0);
            mutFreqs.setMutationFrequencies(freqs);
            ObjectMapper omappy = new ObjectMapper();
            mock.expect(HttpMethod.POST, theURL)
                    .header(HttpHeaders.ACCEPT, MutationFrequencyRestClientImpl.APPLICATION_JSON)
                    .header(HttpHeaders.CONTENT_TYPE, MutationFrequencyRestClientImpl.APPLICATION_JSON)
                    .body(omappy.writeValueAsString(geneList))
                    .thenReturn(MockResponse.of(HttpStatus.OK,
                            "", omappy.writeValueAsString(mutFreqs)));
            
            MutationFrequencies res = client.getMutationFrequencies(theURL, geneList);
            assertNotNull(res);
            assertEquals(1, res.getMutationFrequencies().size());
            assertTrue(res.getMutationFrequencies().containsKey("mtor"));

            mock.verifyAll();
            
        } catch (EnrichmentException ee){
            fail("unexpected exception: " + ee.getMessage());
        }
        finally {
            MockClient.clear();
            if (client != null){
                client.shutdown();
            }
        }
    }
}
