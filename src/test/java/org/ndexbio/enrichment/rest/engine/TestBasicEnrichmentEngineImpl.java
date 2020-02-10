package org.ndexbio.enrichment.rest.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.UUID;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.ServerStatus;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.model.cx.NiceCXNetwork;

/**
 *
 * @author churas
 */
public class TestBasicEnrichmentEngineImpl {

    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();
    
    @Test
	public void testThreadSleep(){
		BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null, null);
		enricher.updateThreadSleepTime(1);
		enricher.threadSleep();
	}
	
	@Test
	public void testRunWithShutdownAlreadyInvoked(){
		BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null, null);
		enricher.shutdown();
		enricher.run();
	}
	
	@Test
	public void testGetEnrichmentQueryResultsFromDbNotFound(){
		BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null, null);
		EnrichmentQueryResults eqr = enricher.getEnrichmentQueryResultsFromDb(UUID.randomUUID().toString());
		assertNotNull(eqr);
		
	}
	
	@Test
	public void testGetEnrichmentQueryResultsFromDbOrFilesystemNotFound() throws IOException {
		File tempDir = _folder.newFolder();
		try {
			BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null,
					tempDir.getAbsolutePath());
			EnrichmentQueryResults eqr = enricher.getEnrichmentQueryResultsFromDbOrFilesystem(UUID.randomUUID().toString());
			assertNull(eqr);
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testGetEnrichmentQueryResultsFromDbOrFilesystemParseError() throws IOException {
		File tempDir = _folder.newFolder();
		try {
			BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null,
					tempDir.getAbsolutePath());
			String myuuid = UUID.randomUUID().toString();
			File subdir = new File(enricher.getEnrichmentQueryResultsFilePath(myuuid));
			assertTrue(subdir.getParentFile().mkdirs());
			File foo = new File(enricher.getEnrichmentQueryResultsFilePath(myuuid));
			assertTrue(foo.createNewFile());
			EnrichmentQueryResults eqr = enricher.getEnrichmentQueryResultsFromDbOrFilesystem(myuuid);
			assertNull(eqr);
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testGetEnrichmentQueryResultsFromDbOrFilesystemFoundInFilesystem() throws IOException {
		File tempDir = _folder.newFolder();
		try {
			BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null,
					tempDir.getAbsolutePath());
			String myuuid = UUID.randomUUID().toString();
			File subdir = new File(enricher.getEnrichmentQueryResultsFilePath(myuuid));
			assertTrue(subdir.getParentFile().mkdirs());
			File foo = new File(enricher.getEnrichmentQueryResultsFilePath(myuuid));
			EnrichmentQueryResults myEqr = new EnrichmentQueryResults();
			myEqr.setMessage("hi");
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(foo, myEqr);
			EnrichmentQueryResults eqr = enricher.getEnrichmentQueryResultsFromDbOrFilesystem(myuuid);
			assertNotNull(eqr);
			assertEquals(myEqr.getMessage(), eqr.getMessage());
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testSaveEnrichmentQueryResultsToFilesystemNotFound(){
		BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null, null);
		String myuuid = UUID.randomUUID().toString();
		enricher.saveEnrichmentQueryResultsToFilesystem(myuuid);
	}
	
	@Test
	public void testAnnotateAndSaveNetworkWithNullFilePassedIn() {
		BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null, null);
		EnrichmentQueryResult eqr = new EnrichmentQueryResult();
		enricher.annotateAndSaveNetwork(null, new NiceCXNetwork(), eqr);
	}
	
	@Test
	public void testAnnotateAndSaveNetworkWithNullNetworkPassedIn() throws IOException {
		File tempFile = _folder.newFile();
		try {
			BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null, null);
			EnrichmentQueryResult eqr = new EnrichmentQueryResult();
			enricher.annotateAndSaveNetwork(tempFile, null, eqr);
			
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testGetUniqueString(){
		EnrichmentQuery query = new EnrichmentQuery();
		query.setDatabaseList(new TreeSet<>(Arrays.asList("dbone", "dbtwo")));
		query.setGeneList(new TreeSet<>(Arrays.asList("gene1", "gene2")));
		BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null, null);
		String res = enricher.getUniqueString(query);
		assertEquals("dbone,dbtwo:GENE1,GENE2", res);
	}
	/**
    @Test
    public void testQuerySuccess() throws EnrichmentException, ExecutionException {
        BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null, null);
        EnrichmentQuery eq = new EnrichmentQuery();
        eq.setDatabaseList(new TreeSet<>(Arrays.asList("ncipid")));
        eq.setGeneList(new TreeSet<>(Arrays.asList("brca1")));
        String res = enricher.query(eq);
        assertTrue(res != null);
        
        EnrichmentQueryResults eqr = enricher.getQueryResults(res, 0, 0);
        assertTrue(eqr != null);
        assertEquals(EnrichmentQueryResults.SUBMITTED_STATUS, eqr.getStatus());
        assertTrue(eqr.getStartTime() > 0);
        
        EnrichmentQueryStatus eqs = enricher.getQueryStatus(res);
        assertEquals(EnrichmentQueryResults.SUBMITTED_STATUS, eqs.getStatus());
        assertEquals(eqr.getStartTime(), eqs.getStartTime());
        
    }*/
  
  @Test
  public void testgetQueryResultsNoResult() {
	  BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null, null);
	  try {
		assertEquals(null, enricher.getQueryResults("12345", 0, 0));
	} catch (EnrichmentException e) {
		fail();
		e.printStackTrace();
	}
  }
  
  /**
  @Test
  public void testDeleteFromDb() throws Exception{
	  BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null, null);
	  EnrichmentQuery eq = new EnrichmentQuery();
	  eq.setDatabaseList(new TreeSet<>(Arrays.asList("ncipid")));
	  eq.setGeneList(new TreeSet<>(Arrays.asList("aldoa")));
	  String id = enricher.query(eq);
	  
	  EnrichmentQueryResults oldEqrs = enricher.getEnrichmentQueryResultsFromDb(id);
	  oldEqrs.setStatus("wrong results");
	  EnrichmentQueryResults oldEqrs2 = enricher.getEnrichmentQueryResultsFromDb(id);
	  assertTrue(oldEqrs2.getStatus().equals("wrong results") );
	  
	  enricher.delete(id);
	  EnrichmentQueryResults newEqrs = enricher.getEnrichmentQueryResultsFromDb(id);
	  assertEquals(newEqrs.getStatus(), null);
  }*/
  
  /**
  @Test
  public void testcacheDelete() throws Exception {
	  try {
		File tempDbDir = _folder.newFolder();
		File tempTaskDir = _folder.newFolder();
		BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, tempDbDir.getAbsolutePath(), tempTaskDir.getAbsolutePath());
		
		EnrichmentQuery eq0 = new EnrichmentQuery();
		eq0.setGeneList(new TreeSet<>(Arrays.asList(Integer.toString(0))));
		eq0.setDatabaseList(new TreeSet<>(Arrays.asList("ncipid")));
		String id0 = enricher.query(eq0);
		EnrichmentQueryResults eqr0 = enricher.getEnrichmentQueryResultsFromDb(id0);
		eqr0.setStatus("not deleted");
		
		for (int i = 1; i < 600; i++) {
			EnrichmentQuery eq = new EnrichmentQuery();
			eq.setGeneList(new TreeSet<>(Arrays.asList(Integer.toString(i))));
			eq.setDatabaseList(new TreeSet<>(Arrays.asList("ncipid")));
			enricher.query(eq);
		}
		
		EnrichmentQuery eq601 = new EnrichmentQuery();
		eq601.setGeneList(new TreeSet<>(Arrays.asList(Integer.toString(601))));
		eq601.setDatabaseList(new TreeSet<>(Arrays.asList("ncipid")));
		String id601 = enricher.query(eq601);
		EnrichmentQueryResults eqr601 = enricher.getEnrichmentQueryResultsFromDb(id601);
		eqr601.setStatus("not deleted");
		
		EnrichmentQueryResults eqr0New = enricher.getEnrichmentQueryResultsFromDb(id0);
		assertFalse(eqr0New.getStatus() == "not deleted");
		
		EnrichmentQueryResults eqr601New = enricher.getEnrichmentQueryResultsFromDb(id601);
		assertTrue(eqr601New.getStatus() == "not deleted");
		
	} finally {
		_folder.delete();
	}
	  
  }*/
  
	@Test
	public void testDeleteNonExistant() throws IOException, EnrichmentException {
		File tempDir = _folder.newFolder();
		try {
			BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null,
					tempDir.getAbsolutePath());
			enricher.delete(UUID.randomUUID().toString());
			
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testDeleteOnResultInFilesystem() throws IOException, EnrichmentException {
		File tempDir = _folder.newFolder();
		try {
			BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null,
					tempDir.getAbsolutePath());
			String myuuid = UUID.randomUUID().toString();
			File taskFile = new File(enricher.getEnrichmentQueryResultsFilePath(myuuid));
			assertTrue(taskFile.getParentFile().mkdirs());
			assertTrue(taskFile.createNewFile());
			assertTrue(taskFile.isFile());
			enricher.delete(myuuid);
			assertFalse(taskFile.isFile());
			assertFalse(taskFile.getParentFile().isDirectory());
		} finally {
			_folder.delete();
		}
	}
  
	@Test
	public void testGetServerStatusErrorCauseNullTaskDir() throws EnrichmentException {
		BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null, null);
		try {
			enricher.getServerStatus();
			fail("Expected Enrichment Exception");
		} catch(EnrichmentException ee){
			assertEquals("Task directory is null", ee.getMessage());
		}
	}
	
	@Test
	public void testGetServerStatusSuccess() throws IOException {
		File tempDir = _folder.newFolder();
		try {
			BasicEnrichmentEngineImpl enricher = new BasicEnrichmentEngineImpl(null, null,
					tempDir.getAbsolutePath());
			ServerStatus ss = enricher.getServerStatus();
			assertEquals(ServerStatus.OK_STATUS, ss.getStatus());
			assertNotNull(ss.getRestVersion());
			
		} catch(EnrichmentException ee){
			assertEquals("Task directory is null", ee.getMessage());
		} finally {
			_folder.delete();
		}
	}
}
