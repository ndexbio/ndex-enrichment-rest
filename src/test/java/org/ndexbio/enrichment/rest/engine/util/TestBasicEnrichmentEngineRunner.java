package org.ndexbio.enrichment.rest.engine.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.enrichment.rest.model.comparators.EnrichmentQueryResultBySimilarity;
import org.ndexbio.ndexsearch.rest.model.DatabaseResult;
/**
 *
 * @author churas
 */
public class TestBasicEnrichmentEngineRunner {
	
	@Rule
	public TemporaryFolder _folder = new TemporaryFolder();
	
	@Test
	public void testGetPvalue(){
		BasicEnrichmentEngineRunner runner = new BasicEnrichmentEngineRunner(null,null,null,
				null,null,null, new EnrichmentQueryResultBySimilarity(), 25,null,null);
		
		// test where total genes in universe is negative
		assertEquals(Double.MAX_VALUE, runner.getPvalue(-5, 100, 10, 5), 0.001);
		
		// test where total genes in network is negative
		assertEquals(Double.MAX_VALUE, runner.getPvalue(1000, -3, 10, 5), 0.001);
		
		// test where query size greater then universe
		assertEquals(Double.MAX_VALUE, runner.getPvalue(50, 20, 100, 5), 0.001);
		
		// test where valid answer is returned
		assertEquals(0.0, runner.getPvalue(50, 20, 5, 5), 0.1);
	}
	
	@Test
	public void testGetUniqueQueryGeneSetFilteredByUniverseGenes(){
		HashSet<String> uniqueGeneSet = new HashSet<>();
		BasicEnrichmentEngineRunner runner = new BasicEnrichmentEngineRunner(null,null,null,
				null,null,uniqueGeneSet, new EnrichmentQueryResultBySimilarity(), 25, null,null);
		
		// try where universe is empty and input is null
		assertEquals(0, runner.getUniqueQueryGeneSetFilteredByUniverseGenes(null).size());
		
		TreeSet<String> genes = new TreeSet<>();
		genes.add("gene1");
		// try where universe is empty and input is one gene
		assertEquals(0, runner.getUniqueQueryGeneSetFilteredByUniverseGenes(genes).size());
		
		// try where universe has some genes
		uniqueGeneSet.add("gene1");
		uniqueGeneSet.add("Gene2");
		uniqueGeneSet.add("gene3");
		runner = new BasicEnrichmentEngineRunner(null,null,null,
				null,null,uniqueGeneSet,new EnrichmentQueryResultBySimilarity(), 25, null,null);
		
		SortedSet<String> res = runner.getUniqueQueryGeneSetFilteredByUniverseGenes(genes);
		assertEquals(1, res.size());
		assertTrue(res.contains("gene1"));
		
		genes.add("Gene3");
		res = runner.getUniqueQueryGeneSetFilteredByUniverseGenes(genes);
		assertEquals(1, res.size());
		assertTrue(res.contains("gene1"));
		
		genes.add("gene3");
		res = runner.getUniqueQueryGeneSetFilteredByUniverseGenes(genes);
		assertEquals(2, res.size());
		assertTrue(res.contains("gene1"));
		assertTrue(res.contains("gene3"));
		
	}
	
	@Test
	public void testUpdateEnrichmentQueryResults(){
		EnrichmentQueryResults eqr = new EnrichmentQueryResults();
		eqr.setStartTime(100);
		BasicEnrichmentEngineRunner runner = new BasicEnrichmentEngineRunner(null,null,null,
				null,null,null,new EnrichmentQueryResultBySimilarity(), 3, null,eqr);
		
		// test update with null result
		runner.updateEnrichmentQueryResults("done", 55, null);
		assertEquals(55, eqr.getProgress());
		assertEquals("done", eqr.getStatus());
		assertTrue(eqr.getWallTime() > 0);
		
		// test with a couple results
		ArrayList<EnrichmentQueryResult> resList = new ArrayList<>();
		resList.add(new EnrichmentQueryResult());
		resList.add(new EnrichmentQueryResult());
		runner.updateEnrichmentQueryResults("ha", 85, resList);
		assertEquals(85, eqr.getProgress());
		assertEquals("ha", eqr.getStatus());
		assertTrue(eqr.getWallTime() > 0);
		assertEquals(2, eqr.getNumberOfHits());
		assertNotNull(eqr.getResults());
		
		// test with a 4 results (one over limit)
		resList = new ArrayList<>();
		resList.add(new EnrichmentQueryResult());
		resList.add(new EnrichmentQueryResult());
		resList.add(new EnrichmentQueryResult());
		resList.add(new EnrichmentQueryResult());
		runner.updateEnrichmentQueryResults("ha", 85, resList);
		assertEquals(85, eqr.getProgress());
		assertEquals("ha", eqr.getStatus());
		assertTrue(eqr.getWallTime() > 0);
		assertEquals(3, eqr.getNumberOfHits());
		assertNotNull(eqr.getResults());
	}
	
	@Test
	public void testGetDatabaseResultFromDbNoDatabases(){
		EnrichmentQueryResults eqr = new EnrichmentQueryResults();
		eqr.setStartTime(100);
		AtomicReference<InternalDatabaseResults> idr = new AtomicReference<>();
		InternalDatabaseResults dRes = new InternalDatabaseResults();
		dRes.setResults(new ArrayList<DatabaseResult>());
		idr.set(dRes);
		
		BasicEnrichmentEngineRunner runner = new BasicEnrichmentEngineRunner(null,null,null,
				idr,null,null, new EnrichmentQueryResultBySimilarity(), 25, null,eqr);
		assertNull(runner.getDatabaseResultFromDb("foo"));
	}
	
	@Test
	public void testGetDatabaseResultFromDbNoMatch(){
		EnrichmentQueryResults eqr = new EnrichmentQueryResults();
		eqr.setStartTime(100);
		AtomicReference<InternalDatabaseResults> idr = new AtomicReference<>();
		InternalDatabaseResults dRes = new InternalDatabaseResults();
		List<DatabaseResult> dbList = new ArrayList<>();
		DatabaseResult dr = new DatabaseResult();
		dr.setName("notmatchingname");
		dbList.add(dr);
		dRes.setResults(dbList);
		idr.set(dRes);
		
		BasicEnrichmentEngineRunner runner = new BasicEnrichmentEngineRunner(null,null,null,
				idr,null,null, new EnrichmentQueryResultBySimilarity(), 25, null,eqr);
		assertNull(runner.getDatabaseResultFromDb("foo"));
	}
	
	@Test
	public void testGetDatabaseResultFromDbWithMatch(){
		EnrichmentQueryResults eqr = new EnrichmentQueryResults();
		eqr.setStartTime(100);
		AtomicReference<InternalDatabaseResults> idr = new AtomicReference<>();
		InternalDatabaseResults dRes = new InternalDatabaseResults();
		List<DatabaseResult> dbList = new ArrayList<>();
		DatabaseResult dr = new DatabaseResult();
		dr.setName("notmatch");
		dbList.add(dr);
		dr = new DatabaseResult();
		dr.setName("fOO");
		dbList.add(dr);
		dr = new DatabaseResult();
		dr.setName("notmatch2");
		dbList.add(dr);
		dRes.setResults(dbList);
		idr.set(dRes);
		
		BasicEnrichmentEngineRunner runner = new BasicEnrichmentEngineRunner(null,null,null,
				idr,null,null,new EnrichmentQueryResultBySimilarity(), 25, null,eqr);
		DatabaseResult res = runner.getDatabaseResultFromDb("Foo");
		assertEquals("fOO", res.getName());
	}
	
	@Test
	public void testCallWithInvalidDirectory() throws IOException, Exception {
		File tmpDir = _folder.newFolder();
		try {
			UUID taskId = UUID.randomUUID();
			File tmpFile = new File(tmpDir.getAbsolutePath() + File.separator + taskId.toString());
			assertTrue(tmpFile.createNewFile());
			EnrichmentQueryResults eqr = new EnrichmentQueryResults();
			BasicEnrichmentEngineRunner runner = new BasicEnrichmentEngineRunner(taskId.toString(),
			tmpDir.getAbsolutePath(), tmpDir.getAbsolutePath(), null, null, null,
					new EnrichmentQueryResultBySimilarity(), 25, null, eqr);
			runner.call();
			assertEquals(EnrichmentQueryResults.FAILED_STATUS, eqr.getStatus());
			assertEquals(100, eqr.getProgress());
			
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testCallEmptyDatabaseList() throws IOException, Exception {
		File tmpDir = _folder.newFolder();
		try {
			UUID taskId = UUID.randomUUID();
			EnrichmentQueryResults eqr = new EnrichmentQueryResults();
			EnrichmentQuery query = new EnrichmentQuery();
			SortedSet<String> dbSet = new TreeSet<>();
			query.setDatabaseList(dbSet);
			SortedSet<String> geneSet = new TreeSet<>();
			query.setGeneList(geneSet);
			BasicEnrichmentEngineRunner runner = new BasicEnrichmentEngineRunner(taskId.toString(),
			tmpDir.getAbsolutePath(), tmpDir.getAbsolutePath(), null, null, null,
					new EnrichmentQueryResultBySimilarity(), 25, query, eqr);
			runner.call();
			assertEquals(EnrichmentQueryResults.COMPLETE_STATUS, eqr.getStatus());
			assertEquals(100, eqr.getProgress());
			
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testCallNomatchingDatabaseResult() throws IOException, Exception {
		File tmpDir = _folder.newFolder();
		try {
			UUID taskId = UUID.randomUUID();
			EnrichmentQueryResults eqr = new EnrichmentQueryResults();
			EnrichmentQuery query = new EnrichmentQuery();
			SortedSet<String> dbSet = new TreeSet<>();
			dbSet.add("db1");
			query.setDatabaseList(dbSet);
			SortedSet<String> geneSet = new TreeSet<>();
			query.setGeneList(geneSet);
			InternalDatabaseResults idr = new InternalDatabaseResults();
			idr.setResults(new ArrayList<>());
			
			AtomicReference<InternalDatabaseResults> idrRef = new AtomicReference<>();
			idrRef.set(idr);
			BasicEnrichmentEngineRunner runner = new BasicEnrichmentEngineRunner(taskId.toString(),
			tmpDir.getAbsolutePath(), tmpDir.getAbsolutePath(), idrRef, null, null,
					new EnrichmentQueryResultBySimilarity(), 25, query, eqr);
			runner.call();
			assertEquals(EnrichmentQueryResults.COMPLETE_STATUS, eqr.getStatus());
			assertEquals(100, eqr.getProgress());
			
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testCallremapNetworkToGenesReturnsNull() throws IOException, Exception {
		File tmpDir = _folder.newFolder();
		try {
			UUID taskId = UUID.randomUUID();
			EnrichmentQueryResults eqr = new EnrichmentQueryResults();
			EnrichmentQuery query = new EnrichmentQuery();
			SortedSet<String> dbSet = new TreeSet<>();
			dbSet.add("db1");
			query.setDatabaseList(dbSet);
			SortedSet<String> geneSet = new TreeSet<>();
			query.setGeneList(geneSet);
			InternalDatabaseResults idr = new InternalDatabaseResults();
			DatabaseResult dr = new DatabaseResult();
			dr.setName("db1");
			dr.setUuid("uuid");
			idr.setResults(Arrays.asList(dr));
			ConcurrentHashMap<String, ConcurrentHashMap<String, HashSet<String>>> databases = new ConcurrentHashMap<>();
			AtomicReference<InternalDatabaseResults> idrRef = new AtomicReference<>();
			idrRef.set(idr);
			BasicEnrichmentEngineRunner runner = new BasicEnrichmentEngineRunner(taskId.toString(),
			tmpDir.getAbsolutePath(), tmpDir.getAbsolutePath(), idrRef, databases, null,
					new EnrichmentQueryResultBySimilarity(), 25, query, eqr);
			runner.call();
			assertEquals(EnrichmentQueryResults.COMPLETE_STATUS, eqr.getStatus());
			assertEquals(100, eqr.getProgress());
			
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testCallremapNetworkSuccess() throws IOException, Exception {
		File tmpDir = _folder.newFolder();
		try {
			UUID taskId = UUID.randomUUID();

			InternalDatabaseResults idr = new InternalDatabaseResults();
			DatabaseResult dr = new DatabaseResult();
			dr.setName("db1");
			dr.setUuid("uuid");
			idr.setResults(Arrays.asList(dr));
			ConcurrentHashMap<String, ConcurrentHashMap<String, HashSet<String>>> databases = new ConcurrentHashMap<>();
			ConcurrentHashMap<String, HashSet<String>> dbMap = new ConcurrentHashMap<>();
			HashSet<String> networkSetOne = new HashSet<>();
			networkSetOne.add("network1");
			networkSetOne.add("network2");
			dbMap.put("gene1", networkSetOne);
			databases.put(taskId.toString(), dbMap);
			AtomicReference<InternalDatabaseResults> idrRef = new AtomicReference<>();
			idrRef.set(idr);
			BasicEnrichmentEngineRunner runner = new BasicEnrichmentEngineRunner(taskId.toString(),
			tmpDir.getAbsolutePath(), tmpDir.getAbsolutePath(), idrRef, databases, null,
					new EnrichmentQueryResultBySimilarity(), 25, null, null);
			
			SortedSet<String> geneSet = new TreeSet<>();
			geneSet.add("gene1");
			geneSet.add("gene2");
			HashMap<String, HashSet<String>> res = runner.remapNetworksToGenes(taskId.toString(), geneSet);
			assertEquals(2, res.size());
			assertTrue(res.containsKey("network1"));
			assertTrue(res.containsKey("network2"));
			assertTrue(res.get("network1").contains("gene1"));
			assertEquals(1, res.get("network1").size());
			assertTrue(res.get("network2").contains("gene1"));
			assertEquals(1, res.get("network2").size());
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testGetNetworkUrl(){
		BasicEnrichmentEngineRunner runner = new BasicEnrichmentEngineRunner(null,null,null,
				null,null,null,new EnrichmentQueryResultBySimilarity(), 25, null,null);
		String res = runner.getNetworkUrl("http://foo/#/networkset/12345", "myid");
		assertEquals("http://foo/viewer/networks/myid", res);
		
		res = runner.getNetworkUrl("http://foo/#/networks/12345", "myid");
		assertNull(res);
		res = runner.getNetworkUrl("http://foo/viewer/networks/12345", "myid");
		assertNull(res);

		
	}
	
	@Test
	public void testGetSimilarityEmptyNetworkAndQuery(){
		BasicEnrichmentEngineRunner runner = new BasicEnrichmentEngineRunner(null,null,null,
				null,null,null,new EnrichmentQueryResultBySimilarity(), 25, null,null);
		Set<String> networkGenes = new HashSet<>();
		SortedSet<String> queryGenes = new TreeSet<>();
		Map<String, Double> idfMap = new HashMap<>();
		double res = runner.getSimilarity(networkGenes, queryGenes, idfMap);
		assertEquals(Double.NaN, res, 0.0001);
	}
	
	@Test
	public void testGetSimilarityFourNetworkGenesAndTwoQueryGenes(){
		BasicEnrichmentEngineRunner runner = new BasicEnrichmentEngineRunner(null,null,null,
				null,null,null,new EnrichmentQueryResultBySimilarity(), 25, null,null);
		Set<String> networkGenes = new HashSet<>();
		networkGenes.add("g1");
		networkGenes.add("g2");
		networkGenes.add("g3");
		networkGenes.add("g4");
		
		
		SortedSet<String> queryGenes = new TreeSet<>();
		
		queryGenes.add("g2");
		queryGenes.add("g3");
		Map<String, Set<Long>> nodeMap = new HashMap<>();
		Map<String, Double> idfMap = new HashMap<>();
		double res = runner.getSimilarity(networkGenes, queryGenes, idfMap);
		assertEquals(Double.NaN, res, 0.0001);
	}
	
	
	
	@Test
	public void testSortEnrichmentQueryResultAndSetRank(){
		BasicEnrichmentEngineRunner runner = new BasicEnrichmentEngineRunner(null,null,null,
				null,null,null,new EnrichmentQueryResultBySimilarity(), 25, null,null);
		List<EnrichmentQueryResult> eqrList = new ArrayList<>();
		EnrichmentQueryResult eqr1 = new EnrichmentQueryResult();
		eqr1.setSimilarity(1.0);
		eqr1.setDatabaseName("one");
		eqrList.add(eqr1);
		eqr1 = new EnrichmentQueryResult();
		eqr1.setSimilarity(2.0);
		eqr1.setDatabaseName("two");
		eqrList.add(eqr1);
		
		runner.sortEnrichmentQueryResultAndSetRank(eqrList);
		assertEquals(2, eqrList.size());
		assertEquals("two", eqrList.get(0).getDatabaseName());
		assertEquals(0, eqrList.get(0).getRank());
		
		assertEquals("one", eqrList.get(1).getDatabaseName());
		assertEquals(1, eqrList.get(1).getRank());

	}
	
}
