package org.ndexbio.enrichment.rest.engine;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryStatus;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;

/**
 *
 * @author churas
 */
public class TestBasicEnrichmentEngineImpl {

    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();
    
    
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
}
