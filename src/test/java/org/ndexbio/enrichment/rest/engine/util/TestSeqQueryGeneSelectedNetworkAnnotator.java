package org.ndexbio.enrichment.rest.engine.util;


import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.After;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.junit.Test;
import org.junit.Before;
import org.ndexbio.cxio.aspects.datamodels.ATTRIBUTE_DATA_TYPE;
import org.ndexbio.cxio.aspects.datamodels.NodeAttributesElement;
import org.ndexbio.enrichment.rest.TestApp;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.model.cx.NiceCXNetwork;
import org.ndexbio.ndexsearch.rest.model.GeneList;
import org.ndexbio.ndexsearch.rest.model.MutationFrequencies;
import org.ndexbio.rest.client.NdexRestClientUtilities;

/**
 *
 * @author churas
 */
public class TestSeqQueryGeneSelectedNetworkAnnotator {
    
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
            SetQueryGeneSelectedNetworkAnnotator annotator = new SetQueryGeneSelectedNetworkAnnotator(null);
            fail("expected EnrichmentException");

        } catch(EnrichmentException ee){
            assertEquals("InternalDatabaseResults is null", ee.getMessage());
        }
    }
    
    @Test
    public void testConstructorWithNullNodeMap(){
        try {
            InternalDatabaseResults idr = new InternalDatabaseResults();
            SetQueryGeneSelectedNetworkAnnotator annotator = new SetQueryGeneSelectedNetworkAnnotator(idr);
            fail("expected EnrichmentException");

        } catch(EnrichmentException ee){
            assertEquals("InternalDatabaseResults NetworkToGeneToNodeMap is null", ee.getMessage());
        }
    }
    
    
    @Test
    public void testNetworkIsNull(){

        try {
            SetQueryGeneSelectedNetworkAnnotator annotator = new SetQueryGeneSelectedNetworkAnnotator(_defaultIdr);

            annotator.annotateNetwork(null, new EnrichmentQuery(), _defaultEqr);
            fail("expected EnrichmentException");
        } catch(EnrichmentException ee){
            assertEquals("network is null", ee.getMessage());
        }
    }
    
    @Test
    public void testQueryResultIsNull(){

        try {
            SetQueryGeneSelectedNetworkAnnotator annotator = new SetQueryGeneSelectedNetworkAnnotator(_defaultIdr);

            annotator.annotateNetwork(new NiceCXNetwork(), new EnrichmentQuery(), null);
            fail("expected EnrichmentException");
        } catch(EnrichmentException ee){
            assertEquals("EnrichmentQueryResult is null", ee.getMessage());
        }
    }
    
    @Test
    public void testQueryResultNetworkIdIsNull(){

        try {
            SetQueryGeneSelectedNetworkAnnotator annotator = new SetQueryGeneSelectedNetworkAnnotator(_defaultIdr);
            EnrichmentQuery query = new EnrichmentQuery();
           
            EnrichmentQueryResult eqr = new EnrichmentQueryResult();
            annotator.annotateNetwork(new NiceCXNetwork(), query, eqr);
            fail("expected EnrichmentException");
        } catch(EnrichmentException ee){
            assertEquals("network UUID is null", ee.getMessage());
        }
    }
    
  
    
 
    @Test
    public void testNoGenesForNetwork(){
        
        try {
            SetQueryGeneSelectedNetworkAnnotator annotator = new SetQueryGeneSelectedNetworkAnnotator(_defaultIdr);
            
            EnrichmentQuery query = new EnrichmentQuery();
            
            annotator.annotateNetwork(new NiceCXNetwork(), query, _defaultEqr);
            // nothing happens, code just returns
        } catch(EnrichmentException ee){
            fail("unexpected EnrichmentException : " + ee.getMessage());
        }
    }
    
    @Test
    public void testGlypican3Network() throws IOException, EnrichmentException {
        NiceCXNetwork net = NdexRestClientUtilities.getCXNetworkFromStream(TestApp.class.getClassLoader().getResourceAsStream("glypican_3_network.cx"));

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
        SetQueryGeneSelectedNetworkAnnotator annotator = new SetQueryGeneSelectedNetworkAnnotator(idr);
       
        _defaultEqr.setHitGenes(new TreeSet<>(Arrays.asList("SHH", "MAPK8")));
        EnrichmentQuery query = new EnrichmentQuery();
        
        // set MAPK8 (node id 7) selected value to false
        NodeAttributesElement selectedAttr = new NodeAttributesElement(7L,
                            SetQueryGeneSelectedNetworkAnnotator.SELECTED,
                            "true",
                                    ATTRIBUTE_DATA_TYPE.BOOLEAN);
        net.addNodeAttribute(selectedAttr);
        net.getMetadata().setElementCount(NodeAttributesElement.ASPECT_NAME, 
                net.getMetadata().getElementCount(NodeAttributesElement.ASPECT_NAME) + 1);
        
        annotator.annotateNetwork(net, query, _defaultEqr);
        
        // network check with went from 16 to 17 attributes
        assertEquals(17L, (long)net.getMetadata().getElementCount(NodeAttributesElement.ASPECT_NAME));
        boolean foundSHH = false;
        boolean foundMAPK8 = false;
        Map<Long, Collection<NodeAttributesElement>> n_attrs = net.getNodeAttributes();
        for(Long nodeId : n_attrs.keySet()){
            for (NodeAttributesElement nae : n_attrs.get(nodeId)){
                System.out.println(nae.getName() + " => " + nae.getValueAsJsonString());
                if (nodeId == 3L){
                    if (nae.getName().equals(SetQueryGeneSelectedNetworkAnnotator.SELECTED)){
                        assertEquals("true", nae.getValue());
                        foundSHH = true;
                    }
                } else if (nodeId == 7L){
                    if (nae.getName().equals(SetQueryGeneSelectedNetworkAnnotator.SELECTED)){
                            assertEquals("true", nae.getValue());
                            foundMAPK8 = true;
                    }
                }
            }
                
        }
        assertTrue(foundSHH);
        assertTrue(foundMAPK8);
    }
        
}
