package org.ndexbio.enrichment.rest.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.ndexbio.enrichment.rest.model.DatabaseResults;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.enrichment.rest.model.InternalGeneMap;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.enrichment.rest.services.Configuration;
import org.ndexbio.ndexsearch.rest.model.DatabaseResult;

/**
 *
 * @author churas
 */
public class TestBasicEnrichmentEngineFactory {
	
	@Test
    public void testGetConstructorNullNDExDatabases() throws EnrichmentException {
		Configuration mockConfig = mock(Configuration.class);
		when (mockConfig.getEnrichmentDatabaseDirectory()).thenReturn("dbdir");
		when(mockConfig.getEnrichmentTaskDirectory()).thenReturn("taskdir");
		when(mockConfig.getNDExDatabases()).thenReturn(null);
		when(mockConfig.getNumberWorkers()).thenReturn(5);
		
		try {
			BasicEnrichmentEngineFactory fac = new BasicEnrichmentEngineFactory(mockConfig);
			fail("Expected NullPointerException");
		} catch(NullPointerException npe){
			assertEquals("Unable to retrieve database", npe.getMessage());
		}
    }
	
	@Test
	public void testConstructorWithpvalueSortAlgorithm(){
		Configuration mockConfig = mock(Configuration.class);
		when (mockConfig.getEnrichmentDatabaseDirectory()).thenReturn("dbdir");
		when(mockConfig.getEnrichmentTaskDirectory()).thenReturn("taskdir");
		InternalDatabaseResults idr = new InternalDatabaseResults();
		when(mockConfig.getNDExDatabases()).thenReturn(idr);
		when(mockConfig.getNumberWorkers()).thenReturn(5);
		when(mockConfig.getNumberOfResultsToReturn()).thenReturn(25);
		when(mockConfig.getSortAlgorithm()).thenReturn("pvalue");
		BasicEnrichmentEngineFactory fac = new BasicEnrichmentEngineFactory(mockConfig);
	}
	
	@Test
	public void testConstructorWithInvalidSortAlgorithm(){
		Configuration mockConfig = mock(Configuration.class);
		when (mockConfig.getEnrichmentDatabaseDirectory()).thenReturn("dbdir");
		when(mockConfig.getEnrichmentTaskDirectory()).thenReturn("taskdir");
		InternalDatabaseResults idr = new InternalDatabaseResults();
		when(mockConfig.getNDExDatabases()).thenReturn(idr);
		when(mockConfig.getNumberWorkers()).thenReturn(5);
		when(mockConfig.getNumberOfResultsToReturn()).thenReturn(25);
		when(mockConfig.getSortAlgorithm()).thenReturn("invalidalgo");
		BasicEnrichmentEngineFactory fac = new BasicEnrichmentEngineFactory(mockConfig);
	}
	
	@Test
	public void testGetEnrichmentEngineEmptyDatabase() throws EnrichmentException {
		Configuration mockConfig = mock(Configuration.class);
		when (mockConfig.getEnrichmentDatabaseDirectory()).thenReturn("dbdir");
		when(mockConfig.getEnrichmentTaskDirectory()).thenReturn("taskdir");
		InternalDatabaseResults idr = new InternalDatabaseResults();
		when(mockConfig.getNDExDatabases()).thenReturn(idr);
		when(mockConfig.getNumberWorkers()).thenReturn(5);
		when(mockConfig.getNumberOfResultsToReturn()).thenReturn(25);
		when(mockConfig.getSortAlgorithm()).thenReturn("pvalue");
		BasicEnrichmentEngineFactory fac = new BasicEnrichmentEngineFactory(mockConfig);
		try {
			fac.getEnrichmentEngine();
		} catch(NullPointerException npe){
			assertEquals("No data found in database", npe.getMessage());
		}
	}
	
	@Test
	public void testGetEnrichmentEngineSuccess() throws EnrichmentException {
		Configuration mockConfig = mock(Configuration.class);
		ExecutorServiceFactory mockExecFac = mock(ExecutorServiceFactory.class);
		ExecutorService mockExec = mock(ExecutorService.class);
		when(mockExecFac.getExecutorService(5)).thenReturn(mockExec);
		when (mockConfig.getEnrichmentDatabaseDirectory()).thenReturn("dbdir");
		when(mockConfig.getEnrichmentTaskDirectory()).thenReturn("taskdir");
		when(mockConfig.getNumberOfResultsToReturn()).thenReturn(25);
		when(mockConfig.getSortAlgorithm()).thenReturn("similarity");
		
		
		InternalDatabaseResults idr = new InternalDatabaseResults();
		ArrayList<DatabaseResult> dbres = new ArrayList<>();
		DatabaseResult dbOne = new DatabaseResult();
		dbOne.setName("one");
		dbOne.setUuid("oneuuid");
		dbres.add(dbOne);
		
		DatabaseResult dbTwo = new DatabaseResult();
		dbTwo.setName("two");
		dbTwo.setUuid("twouuid");
		dbres.add(dbTwo);
		
		ArrayList<InternalGeneMap> geneMapList = new ArrayList<>();
		InternalGeneMap igmOne = new InternalGeneMap();
		igmOne.setDatabaseUUID("oneuuid");
		geneMapList.add(igmOne);
		
		Map<String, Set<String>> geneMapTwo = new HashMap<>();
		Set<String> netSet = new HashSet<>();
		netSet.add("net1");
		netSet.add("net2");
		geneMapTwo.put("gene1", netSet);
		
		netSet = new HashSet<>();
		netSet.add("net3");
		netSet.add("net4");
		geneMapTwo.put("gene2", netSet);
		
		geneMapTwo.put("gene3", new HashSet<>());
		
		InternalGeneMap igmTwo = new InternalGeneMap();
		igmTwo.setDatabaseUUID("twouuid");
		igmTwo.setGeneMap(geneMapTwo);
		
		geneMapList.add(igmTwo);
		idr.setGeneMapList(geneMapList);
		
		idr.setResults(dbres);
                idr.setNetworkToGeneToNodeMap(new HashMap<>());
		
		when(mockConfig.getNDExDatabases()).thenReturn(idr);
		when(mockConfig.getNumberWorkers()).thenReturn(5);
		
		BasicEnrichmentEngineFactory fac = new BasicEnrichmentEngineFactory(mockConfig);
		fac.setAlternateExecutorServiceFactory(mockExecFac);
		EnrichmentEngine ee = fac.getEnrichmentEngine();
		assertNotNull(ee);
		DatabaseResults dr = ee.getDatabaseResults();
		List<DatabaseResult> dbList = dr.getResults();
		assertEquals(2, dbList.size());
		DatabaseResult drOne = dbList.get(0);
		assertEquals("one", drOne.getName());			
		
	}
}
