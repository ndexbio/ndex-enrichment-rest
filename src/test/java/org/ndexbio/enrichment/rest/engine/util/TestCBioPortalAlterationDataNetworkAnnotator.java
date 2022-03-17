package org.ndexbio.enrichment.rest.engine.util;


import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.ndexbio.cxio.aspects.datamodels.NodeAttributesElement;
import org.ndexbio.enrichment.rest.TestApp;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.model.cx.NiceCXNetwork;
import org.ndexbio.ndexsearch.rest.model.AlterationData;
import org.ndexbio.ndexsearch.rest.model.GeneList;
import org.ndexbio.rest.client.NdexRestClientUtilities;

/**
 *
 * @author churas
 */
public class TestCBioPortalAlterationDataNetworkAnnotator {
    
    private InternalDatabaseResults _defaultIdr;
    private EnrichmentQueryResult _defaultEqr;
    
    @Before
    public void setUp(){
        _defaultIdr = new InternalDatabaseResults();
        _defaultIdr.setNetworkToGeneToNodeMap(new HashMap<>());
        _defaultEqr = new EnrichmentQueryResult();
        _defaultEqr.setNetworkUUID("UUID");
        
    }
    
    @After
    public void tearDown(){
        _defaultIdr = null;
        _defaultEqr = null;
    }
    
    @Test
    public void testConstructorWithNullInternalDatabaseResults(){
        try {
            CBioPortalAlterationDataNetworkAnnotator annotator = new CBioPortalAlterationDataNetworkAnnotator(null);
            fail("expected EnrichmentException");

        } catch(EnrichmentException ee){
            assertEquals("InternalDatabaseResults is null", ee.getMessage());
        }
    }
    
    @Test
    public void testConstructorWithNullNodeMap(){
        try {
            InternalDatabaseResults idr = new InternalDatabaseResults();
            CBioPortalAlterationDataNetworkAnnotator annotator = new CBioPortalAlterationDataNetworkAnnotator(idr);
            fail("expected EnrichmentException");

        } catch(EnrichmentException ee){
            assertEquals("InternalDatabaseResults NetworkToGeneToNodeMap is null", ee.getMessage());
        }
    }
    
    @Test
    public void testNullQuery() {
        try {
            CBioPortalAlterationDataNetworkAnnotator annotator = new CBioPortalAlterationDataNetworkAnnotator(_defaultIdr);
            annotator.annotateNetwork(new NiceCXNetwork(), null, _defaultEqr);
            // nothing really happens
        } catch(EnrichmentException ee){
            fail("unexpected EnrichmentException : " + ee.getMessage());
        }
    }
    

    @Test
    public void testNetworkIsNull(){

        try {
            CBioPortalAlterationDataNetworkAnnotator annotator = new CBioPortalAlterationDataNetworkAnnotator(_defaultIdr);
            
            EnrichmentQuery query = new EnrichmentQuery();
            List<AlterationData> altData = new LinkedList();
            query.setAlterationData(altData);
            annotator.annotateNetwork(null, query, _defaultEqr);
            fail("expected EnrichmentException");
        } catch(EnrichmentException ee){
            assertEquals("network is null", ee.getMessage());
        }
    }
    
    @Test
    public void testQueryResultIsNull(){

        try {
            CBioPortalAlterationDataNetworkAnnotator annotator = new CBioPortalAlterationDataNetworkAnnotator(_defaultIdr);
            
            EnrichmentQuery query = new EnrichmentQuery();
            List<AlterationData> altData = new LinkedList();
            query.setAlterationData(altData);
            annotator.annotateNetwork(new NiceCXNetwork(), query, null);
            fail("expected EnrichmentException");
        } catch(EnrichmentException ee){
            assertEquals("EnrichmentQueryResult is null", ee.getMessage());
        }
    }
    
    @Test
    public void testQueryResultNetworkIdIsNull(){

        try {
            CBioPortalAlterationDataNetworkAnnotator annotator = new CBioPortalAlterationDataNetworkAnnotator(_defaultIdr);
            EnrichmentQuery query = new EnrichmentQuery();
            List<AlterationData> altData = new LinkedList();
            query.setAlterationData(altData);
            EnrichmentQueryResult eqr = new EnrichmentQueryResult();
            annotator.annotateNetwork(new NiceCXNetwork(), query, eqr);
            fail("expected EnrichmentException");
        } catch(EnrichmentException ee){
            assertEquals("network UUID is null", ee.getMessage());
        }
    }
	
	@Test
	public void testAlterationDataIsNull(){
		try {
            CBioPortalAlterationDataNetworkAnnotator annotator = new CBioPortalAlterationDataNetworkAnnotator(_defaultIdr);
            EnrichmentQuery query = new EnrichmentQuery();
            annotator.annotateNetwork(new NiceCXNetwork(), query, _defaultEqr);
        } catch(EnrichmentException ee){
			 fail("Unexpected EnrichmentException " + ee.getMessage());
        }
		
	}
    
    @Test
    public void testNoGenesForNetwork(){
        
        try {
            CBioPortalAlterationDataNetworkAnnotator annotator = new CBioPortalAlterationDataNetworkAnnotator(_defaultIdr);
            
            EnrichmentQuery query = new EnrichmentQuery();
            List<AlterationData> altData = new LinkedList();
            query.setAlterationData(altData);
            
            annotator.annotateNetwork(new NiceCXNetwork(), query, _defaultEqr);
            // nothing happens, code just returns
        } catch(EnrichmentException ee){
            fail("unexpected EnrichmentException : " + ee.getMessage());
        }
    }

    @Test
    public void testGlypican3Network() throws IOException, EnrichmentException {
        NiceCXNetwork net = NdexRestClientUtilities.getCXNetworkFromStream(TestApp.class.getClassLoader().getResourceAsStream("glypican_3_network.cx"));
        String restEndPoint = "http://foo.com";
        GeneList geneList = new GeneList();
        geneList.setGenes(Arrays.asList("SSH"));
        
        InternalDatabaseResults idr = new InternalDatabaseResults();
        Map<String, Map<String, Set<Long>>> nodeMap = new HashMap<>();
        Map<String, Set<Long>> geneMap = new HashMap<>();
        geneMap.put("PTCH1", new HashSet<>(Arrays.asList(2L)));
        geneMap.put("SHH", new HashSet<>(Arrays.asList(3L)));
        geneMap.put("MAPK8", new HashSet<>(Arrays.asList(7L)));
        geneMap.put("MAPK9", new HashSet<>(Arrays.asList(8L)));
        geneMap.put("GPC3", new HashSet<>(Arrays.asList(1L)));
        geneMap.put("DKK1", new HashSet<>(Arrays.asList(4L, 8L)));
        geneMap.put("FURIN", new HashSet<>(Arrays.asList(5L)));
        nodeMap.put("UUID", geneMap);
        idr.setNetworkToGeneToNodeMap(nodeMap);
        
        _defaultEqr.setNetworkUUID("UUID");
        CBioPortalAlterationDataNetworkAnnotator annotator = new CBioPortalAlterationDataNetworkAnnotator(idr);
        EnrichmentQuery query = new EnrichmentQuery();
        
        query.setGeneList(new TreeSet<>(Arrays.asList("SHH")));
		AlterationData shh = new AlterationData();
		shh.setGene("SHH");
		shh.setPercentAltered("42%");
		List<AlterationData> altData = Arrays.asList(shh);
        query.setAlterationData(altData);
        annotator.annotateNetwork(net, query, _defaultEqr);
        
        // network originally has 15 node attributes, check that we now have 30
        assertEquals(30L, (long)net.getMetadata().getElementCount(NodeAttributesElement.ASPECT_NAME));
        
        Map<Long, Collection<NodeAttributesElement>> n_attrs = net.getNodeAttributes();
        for(Long nodeId : n_attrs.keySet()){
            for (NodeAttributesElement nae : n_attrs.get(nodeId)){
                System.out.println(nae.getName() + " => " + nae.getValueAsJsonString());
                if (nodeId == 3L){
                    if (nae.getName().equals(CBioPortalAlterationDataNetworkAnnotator.IQUERY_LABEL)){
                        assertTrue("SHH (42%)".equals(nae.getValue()));
                    } else if (nae.getName().equals(CBioPortalAlterationDataNetworkAnnotator.IQUERY_PERCENTALTERED_LIST)){
                        assertTrue("SHH::42%".equals(nae.getValues().get(0)));
                    }
                } 
            }
        }
    }  
}
