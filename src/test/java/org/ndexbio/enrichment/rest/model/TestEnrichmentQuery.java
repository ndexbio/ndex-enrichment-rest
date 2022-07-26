package org.ndexbio.enrichment.rest.model;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.ndexbio.ndexsearch.rest.model.AlterationData;
/**
 *
 * @author churas
 */
public class TestEnrichmentQuery {
    
    @Test
    public void testGettersAndSetters(){
        EnrichmentQuery eq = new EnrichmentQuery();
        assertEquals(null, eq.getDatabaseList());
        assertEquals(null, eq.getGeneList());
        TreeSet<String> dblist = new TreeSet<>();
        dblist.add("db");
		dblist.add("");
        eq.setDatabaseList(dblist);
        
        TreeSet<String> genelist = new TreeSet<>();
        genelist.add("gene");
		genelist.add("");
        eq.setGeneList(genelist);
        
		assertEquals(1, eq.getDatabaseList().size());
        assertEquals("db", eq.getDatabaseList().first());
		assertEquals(1, eq.getGeneList().size());
        assertEquals("GENE", eq.getGeneList().first());

    }
	
	@Test
	public void testhashCode(){
		EnrichmentQuery eq = new EnrichmentQuery();
		// try on empty object
		assertEquals(-431459049, eq.hashCode());
		
		// try with just empty database list set
		SortedSet<String> dblist = new TreeSet<>();
		eq.setDatabaseList(dblist);
		assertEquals(-431459049, eq.hashCode());
		
		// try with just empty database and gene list set
		SortedSet<String> genelist = new TreeSet<>();
		eq.setGeneList(genelist);
		assertEquals(-431459049, eq.hashCode());
		
		// try with database list with 1 value
		dblist.add("db1");
		eq.setDatabaseList(dblist);
		assertEquals(1508617782, eq.hashCode());
		
		// try with database list with 2 values
		dblist.add("db2");
		eq.setDatabaseList(dblist);
		assertEquals(-1113712068, eq.hashCode());
		
		// try now with also gene list with 1 value
		genelist.add("gene1");
		eq.setGeneList(genelist);
		assertEquals(-1451926778, eq.hashCode());
		
		// try now with also gene list with 2 values
		genelist.add("gene2");
		eq.setGeneList(genelist);
		assertEquals(588716314, eq.hashCode());
		
		AlterationData aOne = new AlterationData();
		aOne.setGene("gene1");
		aOne.setPercentAltered("1%");
		eq.setAlterationData(Arrays.asList(aOne));
		assertEquals(1472474081, eq.hashCode());
		
		aOne = new AlterationData();
		aOne.setGene("gene1");
		aOne.setPercentAltered("2%");
		eq.setAlterationData(Arrays.asList(aOne));
		assertEquals(338607488, eq.hashCode());
		
		AlterationData aTwo = new AlterationData();
		aTwo.setGene("gene2");
		aTwo.setPercentAltered("1%");
		eq.setAlterationData(Arrays.asList(aOne, aTwo));
		assertEquals(-358253819, eq.hashCode());
	}
	
	@Test
	public void testEquals(){
		EnrichmentQuery eq = new EnrichmentQuery();
		
		// compare with non EnrichmentQuery object
		assertFalse(eq.equals("hi"));
		// compare with itself
		assertTrue(eq.equals(eq));
		
		SortedSet<String> dblist = new TreeSet<>();
		// try with dblist matching
		dblist.add("db1");
		eq.setDatabaseList(dblist);
		assertTrue(eq.equals(eq));
		
		// try with dblist differing
		EnrichmentQuery othereq = new EnrichmentQuery();
		assertFalse(eq.equals(othereq));
		assertFalse(othereq.equals(eq));
		
		// try with dblist matching
		othereq.setDatabaseList(dblist);
		assertTrue(eq.equals(othereq));
		assertTrue(othereq.equals(eq));
		
		SortedSet<String> genelist = new TreeSet<>();
		
		eq = new EnrichmentQuery();
		eq.setDatabaseList(new TreeSet<>(Arrays.asList("dbone", "dbtwo")));
		eq.setGeneList(new TreeSet<>(Arrays.asList("gene1", "gene2")));
		assertTrue(eq.equals(eq));
		othereq = new EnrichmentQuery();
		othereq.setDatabaseList(eq.getDatabaseList());
		othereq.setGeneList(othereq.getGeneList());
		AlterationData aOne = new AlterationData();
		aOne.setGene("gene1");
		aOne.setPercentAltered("1%");
		othereq.setAlterationData(Arrays.asList(aOne));
		assertFalse(eq.equals(othereq));
		assertFalse(othereq.equals(eq));
		
	}
}
