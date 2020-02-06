/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import org.junit.Test;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.cxio.aspects.datamodels.NetworkAttributesElement;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.enrichment.rest.model.InternalGeneMap;
import org.ndexbio.model.cx.NiceCXNetwork;
import org.ndexbio.ndexsearch.rest.model.NetworkInfo;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;
import org.ndexbio.rest.client.NdexRestClientUtilities;

/**
 *
 * @author churas
 */
public class TestApp {
    
	@Rule
    public TemporaryFolder _folder = new TemporaryFolder();
	
    @Test
    public void testGenerateExampleConfiguration() throws Exception{
        String res = App.generateExampleConfiguration();
        assertTrue(res.contains("# Example configuration file for Enrichment service"));
    }
    
    @Test
    public void testExampleModes(){
        String[] args = {"--mode", App.EXAMPLE_CONF_MODE};
        App.main(args);
        String[] oargs = {"--mode", App.EXAMPLE_DBRES_MODE};
        App.main(oargs);
    }
    
    @Test
    public void testGetValidGene(){
        // check null works fine
        assertEquals(null, App.getValidGene(null));
        
        // check standard gene comes back okay
        assertEquals("TP53", App.getValidGene("TP53"));
        
        // check too long gene comes back null
        assertEquals(null,
                     App.getValidGene("AAAAAAAAAAAAAAAAAAAA"
                             + "AAAAAAAAAAAAAAAAAAAAA"));
        
        // invalid gene
        assertEquals(null, App.getValidGene("hi,how"));
        
        // with hgnc.symbol prefix
        assertEquals("HELLO", App.getValidGene("hgnc.symbol:HELLO"));
		assertNull(App.getValidGene("hgnc.symbol:"));

    }
	
	@Test
	public void testIsTypeProtein(){
		// null check
		assertFalse(App.isTypeProtein(null));
		
		// gene
		assertTrue(App.isTypeProtein("gene"));

		// protein
		assertTrue(App.isTypeProtein("protein"));

		// geneproduct
		assertTrue(App.isTypeProtein("geneproduct"));
		
		// GeNe
		assertTrue(App.isTypeProtein("GeNe"));
		
		// Protein
		assertTrue(App.isTypeProtein("Protein"));
		
		// gene product
		assertFalse(App.isTypeProtein("gene product"));
		
		// empty string
		assertFalse(App.isTypeProtein(""));
	
	}
	
	@Test
	public void testIsTypeComplex(){
		// null check
		assertFalse(App.isTypeComplex(null));
		
		// complex
		assertTrue(App.isTypeComplex("complex"));

		// proteinfamily
		assertTrue(App.isTypeComplex("proteinfamily"));

		// geneproduct
		assertFalse(App.isTypeComplex("geneproduct"));
		
		// CompartMent
		assertTrue(App.isTypeComplex("CompartMent"));
		
		//compartment
		assertTrue(App.isTypeComplex("compartment"));
		
		//gene product
		assertFalse(App.isTypeComplex("gene product"));
		
		//empty string
		assertFalse(App.isTypeComplex(""));
	
	}
	
	@Test
	public void testGetSimpleNetwork(){
		NiceCXNetwork network = new NiceCXNetwork();
		network.addNetworkAttribute(new NetworkAttributesElement(null, "name", "netname"));
		NetworkInfo res = App.getSimpleNetwork(network, "netuuid", "neturl", "imageurl");
		assertEquals("netname", res.getName());
		assertEquals("neturl", res.getUrl());
		assertEquals("imageurl", res.getImageUrl());
		assertEquals(null, res.getDescription());
		assertEquals(0, res.getGeneCount());
	}
	
	@Test
	public void testGetNetworkUrl(){
		assertEquals("http://someserver.com/#/network/uuid",
				App.getNetworkUrl("http://someserver.com", "uuid"));
	}
	
	@Test
	public void testGetNetworkSetUrletNetworkUrl(){
		assertEquals("http://someserver.com/#/networkset/uuid",
				App.getNetworkSetUrl("http://someserver.com", "uuid"));
	}
	
	@Test
	public void testGetAppNameAndVersionProperties(){
		Properties props = App.getAppNameAndVersionProperties();
		assertNotNull(props.getProperty("project.name"));
		assertNotNull(props.getProperty("project.version"));
	}
	
	@Test
	public void testGetDescription(){
		String desc = App.getDescription();
		assertNotNull(desc);
		assertTrue(desc.contains("For more information visit:"));
	}
	
	@Test
	public void testSaveNetworkSuccess() throws Exception{
		try {
			File tempDir = _folder.newFolder();
			NdexRestClientModelAccessLayer mockClient = mock(NdexRestClientModelAccessLayer.class);
			UUID myUUID = UUID.randomUUID();
			when(mockClient.getNetworkAsCXStream(myUUID)).thenReturn(TestApp.class.getClassLoader().getResourceAsStream("glypican_3_network.cx"));
			NiceCXNetwork net = App.saveNetwork(mockClient, myUUID, tempDir);
			assertNotNull(net);
			assertEquals("Glypican 3 network", net.getNetworkName());
			assertEquals(8, net.getNodes().size());
			File netFile = new File(tempDir.getAbsolutePath() + File.separator +
					myUUID.toString() + ".cx");
			assertTrue(netFile.isFile());
		} finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testGetPropertiesFromConf() throws Exception{
		try {
			File tempDir = _folder.newFolder();
			File tmpFile = new File(tempDir.getAbsolutePath() + File.separator + "hi.props");
			FileWriter fw = new FileWriter(tmpFile);
			fw.write("hi=bye\nadios=later\n");
			fw.close();
			Properties props = App.getPropertiesFromConf(tmpFile.getAbsolutePath());
			assertNotNull(props);
			assertEquals("bye", props.getProperty("hi"));
			assertEquals("later", props.getProperty("adios"));

		}
		finally {
			_folder.delete();
		}
	}
	
	@Test
	public void testUpdateGeneMap() throws Exception {
		NiceCXNetwork net = NdexRestClientUtilities.getCXNetworkFromStream(TestApp.class.getClassLoader().getResourceAsStream("glypican_3_network.cx"));
		InternalGeneMap geneMap = new InternalGeneMap();
		InternalDatabaseResults idr = new InternalDatabaseResults();
		HashSet<String> uniqueGeneSet = new HashSet<>();
		
		int geneCount = App.updateGeneMap(net, "glypy", geneMap, uniqueGeneSet, idr);
		Map<String, Set<String>> mappy = geneMap.getGeneMap();
		assertEquals(7, geneCount);
		assertEquals(7, uniqueGeneSet.size());
		HashSet<String> glypyGenes = new HashSet<>();
		for (String g : uniqueGeneSet){
			assertEquals(1, mappy.get(g).size());
			assertTrue(mappy.get(g).contains("glypy"));
			glypyGenes.add(g);
		}
		
		NiceCXNetwork netTwo = NdexRestClientUtilities.getCXNetworkFromStream(TestApp.class.getClassLoader().getResourceAsStream("fap_insulin_mediated_apipogenesis.cx"));
		geneCount = App.updateGeneMap(netTwo, "fap_insulin", geneMap, uniqueGeneSet, idr);
		assertEquals(13, geneCount);
		assertEquals(20, uniqueGeneSet.size());
		
		assertEquals(20, mappy.size());
		assertEquals(1, mappy.get("AKT1").size());
		assertTrue(mappy.get("AKT1").contains("fap_insulin"));
		mappy = geneMap.getGeneMap();
		for (String g : uniqueGeneSet){
			try {
				assertEquals(1, mappy.get(g).size());
			} catch(NullPointerException npe){
				npe.printStackTrace();
				fail("Got a nullpointer exception trying to lookup this gene: " +
						g + " : " + mappy.toString());
			}
			if (glypyGenes.contains(g)){
				assertTrue(mappy.get(g).contains("glypy"));
			} else {
				assertTrue(mappy.get(g).contains("fap_insulin"));
			}
		}
		
		// try adding empty network
		NiceCXNetwork netThree = new NiceCXNetwork();
		geneCount = App.updateGeneMap(netThree, "empty", geneMap, uniqueGeneSet, idr);
		assertEquals(0, geneCount);
		assertEquals(20, uniqueGeneSet.size());
		
		// try adding duplicate network with different id
		geneCount = App.updateGeneMap(net, "glypy2", geneMap, uniqueGeneSet, idr);
		assertEquals(7, geneCount);
		assertEquals(20, uniqueGeneSet.size());
		mappy = geneMap.getGeneMap();
		for (String g : uniqueGeneSet){
			if (glypyGenes.contains(g)){
				assertEquals(2, mappy.get(g).size());
				assertTrue(mappy.get(g).contains("glypy"));
				assertTrue(mappy.get(g).contains("glypy2"));
			} else {
				assertEquals(1, mappy.get(g).size());
				assertTrue(mappy.get(g).contains("fap_insulin"));
			}
		}
	}
	
}
