package org.ndexbio.enrichment.rest.client;

import java.io.IOException;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import java.io.InputStream;
import java.util.TreeSet;
import kong.unirest.GetRequest;
import kong.unirest.HttpResponse;
import kong.unirest.HttpStatus;
import kong.unirest.UnirestException;
import kong.unirest.UnirestInstance;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
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
    public void testGetNetworkOverlayAsCXIdNull(){
		
		UnirestInstance mockInstance = createMock(UnirestInstance.class);
		
        EnrichmentRestClientImpl erc = new EnrichmentRestClientImpl(mockInstance, "http://foo", null);
		try {
			erc.getNetworkOverlayAsCX(null, "databaseUUID", "networkUUID");
			fail("Expected IllegalArgumentExeption");
		} catch(EnrichmentException ee){
			fail("did not expect this exception: " + ee.getMessage());
		} catch(IllegalArgumentException iae){
			assertEquals("id cannot be null", iae.getMessage());
		}
    }
	
	@Test
    public void testGetNetworkOverlayAsCXUnirestException(){
		UnirestInstance mockInstance = createMock(UnirestInstance.class);
		expect(mockInstance.get("http://foo/12345/overlaynetwork")).andThrow(new UnirestException("error"));

		replay(mockInstance);
		
        EnrichmentRestClientImpl erc = new EnrichmentRestClientImpl(mockInstance, "http://foo", null);
		try {
			erc.getNetworkOverlayAsCX("12345", "db", "net");
			fail("Expected EnrichmentException");
		} catch(EnrichmentException ee){
			assertEquals("Caught an exception: error", ee.getMessage());
		}

		verify(mockInstance);
    }
	
	@Test
    public void testGetNetworkOverlayAsCXErrorHttpStatus(){
		
		GetRequest mockDbQueryGR = createMock(GetRequest.class);
		GetRequest mockNQueryGR = createMock(GetRequest.class);
		GetRequest mockHeaderGR = createMock(GetRequest.class);
		GetRequest mockBytesGR = createMock(GetRequest.class);
		HttpResponse<byte[]> mockResponse = createMock(HttpResponse.class);
		expect(mockResponse.getStatus()).andReturn(HttpStatus.NOT_FOUND);
		expect(mockResponse.getStatus()).andReturn(HttpStatus.NOT_FOUND);
		
		expect(mockBytesGR.asBytes()).andReturn(mockResponse);
		expect(mockHeaderGR.header(EnrichmentRestClientImpl.ACCEPT, EnrichmentRestClientImpl.APPLICATION_JSON)).andReturn(mockDbQueryGR);
		expect(mockDbQueryGR.queryString("databaseUUID", "db")).andReturn(mockNQueryGR);
		expect(mockNQueryGR.queryString("networkUUID", "net")).andReturn(mockBytesGR);
		UnirestInstance mockInstance = createMock(UnirestInstance.class);
		expect(mockInstance.get("http://foo/12345/overlaynetwork")).andReturn(mockHeaderGR);
		
		replay(mockDbQueryGR);
		replay(mockNQueryGR);
		replay(mockHeaderGR);
		replay(mockBytesGR);
		replay(mockResponse);
		replay(mockInstance);
		
        EnrichmentRestClientImpl erc = new EnrichmentRestClientImpl(mockInstance, "http://foo", null);
		try {
			erc.getNetworkOverlayAsCX("12345", "db", "net");
			fail("Expected IllegalArgumentExeption");
		} catch(EnrichmentException ee){
			assertEquals("HTTP Error: 404", ee.getMessage());
		}
		verify(mockDbQueryGR);
		verify(mockNQueryGR);
		verify(mockHeaderGR);
		verify(mockBytesGR);
		verify(mockResponse);
		verify(mockInstance);
    }
	
	@Test
    public void testGetNetworkOverlayAsCXSuccess(){
		
		GetRequest mockDbQueryGR = createMock(GetRequest.class);
		GetRequest mockNQueryGR = createMock(GetRequest.class);
		GetRequest mockHeaderGR = createMock(GetRequest.class);
		GetRequest mockBytesGR = createMock(GetRequest.class);
		HttpResponse<byte[]> mockResponse = createMock(HttpResponse.class);
		expect(mockResponse.getStatus()).andReturn(HttpStatus.OK);
		byte[] barr = new byte[1];
		expect(mockResponse.getBody()).andReturn(barr);
		expect(mockBytesGR.asBytes()).andReturn(mockResponse);
		expect(mockHeaderGR.header(EnrichmentRestClientImpl.ACCEPT, EnrichmentRestClientImpl.APPLICATION_JSON)).andReturn(mockDbQueryGR);
		expect(mockDbQueryGR.queryString("databaseUUID", "db")).andReturn(mockNQueryGR);
		expect(mockNQueryGR.queryString("networkUUID", "net")).andReturn(mockBytesGR);
		UnirestInstance mockInstance = createMock(UnirestInstance.class);
		expect(mockInstance.get("http://foo/12345/overlaynetwork")).andReturn(mockHeaderGR);
		
		replay(mockDbQueryGR);
		replay(mockNQueryGR);
		replay(mockHeaderGR);
		replay(mockBytesGR);
		replay(mockResponse);
		replay(mockInstance);
		
        EnrichmentRestClientImpl erc = new EnrichmentRestClientImpl(mockInstance, "http://foo", null);
		try {
			InputStream in = erc.getNetworkOverlayAsCX("12345", "db", "net");
			byte[] resBarr = in.readAllBytes();
			
			assertEquals(barr.length, resBarr.length);
			assertEquals(barr[0], resBarr[0]);
		} catch(EnrichmentException ee){
			fail("Unexpected exception " + ee.getMessage());
		} catch(IOException io){
			fail("unexpected ioexception " + io.getMessage());
		}
		verify(mockDbQueryGR);
		verify(mockNQueryGR);
		verify(mockHeaderGR);
		verify(mockBytesGR);
		verify(mockResponse);
		verify(mockInstance);
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
