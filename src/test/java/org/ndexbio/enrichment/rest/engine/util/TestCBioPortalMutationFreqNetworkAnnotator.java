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
public class TestCBioPortalMutationFreqNetworkAnnotator {
    
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
            CBioPortalMutationFreqNetworkAnnotator annotator = new CBioPortalMutationFreqNetworkAnnotator(null);
            fail("expected EnrichmentException");

        } catch(EnrichmentException ee){
            assertEquals("InternalDatabaseResults is null", ee.getMessage());
        }
    }
    
    @Test
    public void testConstructorWithNullNodeMap(){
        try {
            InternalDatabaseResults idr = new InternalDatabaseResults();
            CBioPortalMutationFreqNetworkAnnotator annotator = new CBioPortalMutationFreqNetworkAnnotator(idr);
            fail("expected EnrichmentException");

        } catch(EnrichmentException ee){
            assertEquals("InternalDatabaseResults NetworkToGeneToNodeMap is null", ee.getMessage());
        }
    }
    
    @Test
    public void testNullQuery() {
        try {
            CBioPortalMutationFreqNetworkAnnotator annotator = new CBioPortalMutationFreqNetworkAnnotator(_defaultIdr);
            annotator.annotateNetwork(new NiceCXNetwork(), null, _defaultEqr);
            // nothing really happens
        } catch(EnrichmentException ee){
            fail("unexpected EnrichmentException : " + ee.getMessage());
        }
    }
    
    @Test
    public void testNullAnnotationServices(){

        try {
            CBioPortalMutationFreqNetworkAnnotator annotator = new CBioPortalMutationFreqNetworkAnnotator(_defaultIdr);
            EnrichmentQuery query = new EnrichmentQuery();
            annotator.annotateNetwork(new NiceCXNetwork(), query, _defaultEqr);
            // nothing really happens
        } catch(EnrichmentException ee){
            fail("unexpected EnrichmentException : " + ee.getMessage());
        }
    }
    
    @Test
    public void testNullMutationURL(){

        try {
            CBioPortalMutationFreqNetworkAnnotator annotator = new CBioPortalMutationFreqNetworkAnnotator(_defaultIdr);
            EnrichmentQuery query = new EnrichmentQuery();

            Map<String, String> annotationServices = new HashMap<>();
            query.setGeneAnnotationServices(annotationServices);
            annotationServices.put(CBioPortalMutationFreqNetworkAnnotator.MUTATION_FREQUENCY,
                    null);
            annotator.annotateNetwork(new NiceCXNetwork(), query, _defaultEqr);
            // nothing really happens
        } catch(EnrichmentException ee){
            fail("unexpected EnrichmentException : " + ee.getMessage());
        }
    }
    
    @Test
    public void testNetworkIsNull(){

        try {
            CBioPortalMutationFreqNetworkAnnotator annotator = new CBioPortalMutationFreqNetworkAnnotator(_defaultIdr);
            
            EnrichmentQuery query = new EnrichmentQuery();
            Map<String, String> annotationServices = new HashMap<>();
            annotationServices.put(CBioPortalMutationFreqNetworkAnnotator.MUTATION_FREQUENCY,
                    "https://doesnotexist");
            query.setGeneAnnotationServices(annotationServices);
            annotator.annotateNetwork(null, query, _defaultEqr);
            fail("expected EnrichmentException");
        } catch(EnrichmentException ee){
            assertEquals("network is null", ee.getMessage());
        }
    }
    
    @Test
    public void testQueryResultIsNull(){

        try {
            CBioPortalMutationFreqNetworkAnnotator annotator = new CBioPortalMutationFreqNetworkAnnotator(_defaultIdr);
            
            EnrichmentQuery query = new EnrichmentQuery();
            Map<String, String> annotationServices = new HashMap<>();
            annotationServices.put(CBioPortalMutationFreqNetworkAnnotator.MUTATION_FREQUENCY,
                    "https://doesnotexist");
            query.setGeneAnnotationServices(annotationServices);
            annotator.annotateNetwork(new NiceCXNetwork(), query, null);
            fail("expected EnrichmentException");
        } catch(EnrichmentException ee){
            assertEquals("EnrichmentQueryResult is null", ee.getMessage());
        }
    }
    
    @Test
    public void testQueryResultNetworkIdIsNull(){

        try {
            CBioPortalMutationFreqNetworkAnnotator annotator = new CBioPortalMutationFreqNetworkAnnotator(_defaultIdr);
            EnrichmentQuery query = new EnrichmentQuery();
            Map<String, String> annotationServices = new HashMap<>();
            annotationServices.put(CBioPortalMutationFreqNetworkAnnotator.MUTATION_FREQUENCY,
                    "https://doesnotexist");
            query.setGeneAnnotationServices(annotationServices);
            EnrichmentQueryResult eqr = new EnrichmentQueryResult();
            annotator.annotateNetwork(new NiceCXNetwork(), query, eqr);
            fail("expected EnrichmentException");
        } catch(EnrichmentException ee){
            assertEquals("network UUID is null", ee.getMessage());
        }
    }
    
    @Test
    public void testRESTClientIsNull(){

        try {
            CBioPortalMutationFreqNetworkAnnotator annotator = new CBioPortalMutationFreqNetworkAnnotator(_defaultIdr);
            EnrichmentQuery query = new EnrichmentQuery();
            Map<String, String> annotationServices = new HashMap<>();
            annotationServices.put(CBioPortalMutationFreqNetworkAnnotator.MUTATION_FREQUENCY,
                    "https://doesnotexist");
            query.setGeneAnnotationServices(annotationServices);

            annotator.setAlternateMutationFrequencyRestClient(null);
            
            annotator.annotateNetwork(new NiceCXNetwork(), query, _defaultEqr);
            fail("expected EnrichmentException");
        } catch(EnrichmentException ee){
            assertEquals("REST client is null", ee.getMessage());
        }
    }
    
    @Test
    public void testNetworkLabelAnnotatorIsNull(){

        try {
            CBioPortalMutationFreqNetworkAnnotator annotator = new CBioPortalMutationFreqNetworkAnnotator(_defaultIdr);
            
            EnrichmentQuery query = new EnrichmentQuery();
            Map<String, String> annotationServices = new HashMap<>();
            annotationServices.put(CBioPortalMutationFreqNetworkAnnotator.MUTATION_FREQUENCY,
                    "https://doesnotexist");
            query.setGeneAnnotationServices(annotationServices);

            annotator.setAlternateNetworkLabelAnnotator(null);
            
            annotator.annotateNetwork(new NiceCXNetwork(), query, _defaultEqr);
            fail("expected EnrichmentException");
        } catch(EnrichmentException ee){
            assertEquals("NetworkLabelAnnotator is null", ee.getMessage());
        }
    }
    
    @Test
    public void testNoGenesForNetwork(){
        
        try {
            CBioPortalMutationFreqNetworkAnnotator annotator = new CBioPortalMutationFreqNetworkAnnotator(_defaultIdr);
            
            EnrichmentQuery query = new EnrichmentQuery();
            Map<String, String> annotationServices = new HashMap<>();
            annotationServices.put(CBioPortalMutationFreqNetworkAnnotator.MUTATION_FREQUENCY,
                    "https://doesnotexist");
            query.setGeneAnnotationServices(annotationServices);

            
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
        MutationFrequencies mutFreqs = new MutationFrequencies();
        Map<String, Double> freqs = new HashMap<>();
        freqs.put("PTCH1", 2.0);
        freqs.put("SHH", 3.0);
        freqs.put("MAPK8", 7.0);
        freqs.put("MAPK9", Double.NaN);
        freqs.put("GPC3", 1.0);
        freqs.put("DKK1", 4.0);
        // FURIN is not set its id is 5
        mutFreqs.setMutationFrequencies(freqs);
        
        MutationFrequencyRestClient mockClient = mock(MutationFrequencyRestClient.class);
        when(mockClient.getMutationFrequencies(eq(restEndPoint), any())).thenReturn(mutFreqs);
        
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
        CBioPortalMutationFreqNetworkAnnotator annotator = new CBioPortalMutationFreqNetworkAnnotator(idr);
        annotator.setAlternateMutationFrequencyRestClient(mockClient);
        EnrichmentQuery query = new EnrichmentQuery();
        Map<String, String> geneAnnotationServices = new HashMap<>();
        geneAnnotationServices.put(CBioPortalMutationFreqNetworkAnnotator.MUTATION_FREQUENCY,
                restEndPoint);
        query.setGeneAnnotationServices(geneAnnotationServices);
        query.setGeneList(new TreeSet<>(Arrays.asList("SSH")));
        annotator.annotateNetwork(net, query, _defaultEqr);
        
        // network originally has 15 node attributes, check that we now have 30
        assertEquals(30L, (long)net.getMetadata().getElementCount(NodeAttributesElement.ASPECT_NAME));
        
        Map<Long, Collection<NodeAttributesElement>> n_attrs = net.getNodeAttributes();
        for(Long nodeId : n_attrs.keySet()){
            for (NodeAttributesElement nae : n_attrs.get(nodeId)){
                System.out.println(nae.getName() + " => " + nae.getValueAsJsonString());
                if (nodeId == 4L){
                    if (nae.getName().equals(CBioPortalMutationFreqNetworkAnnotator.IQUERY_LABEL)){
                        //assertTrue("MAPK8 7.0%,DKK1 4.0%".equals(nae.getValue()) ||
                        //        "DKK1 4.0%,MAPK8 7.0%".equals(nae.getValue()));
                    } else if (nae.getName().equals(CBioPortalMutationFreqNetworkAnnotator.IQUERY_MUTFREQ_LIST)){
                        //assertTrue("MAPK8::7.0".equals(nae.getValues().get(0)) ||
                        //        "MAPK8::7.0".equals(nae.getValues().get(1)));
                       //assertTrue("DKK1::4.0".equals(nae.getValues().get(0)) ||
                        //        "DKK1::4.0".equals(nae.getValues().get(1)));
                        System.out.println("uh");

                    }
                } else if (nodeId == 2L){
                    if (nae.getName().equals(CBioPortalMutationFreqNetworkAnnotator.IQUERY_LABEL)){
                        assertEquals("PTCH1 (2.0%)", nae.getValue());
                    } else if (nae.getName().equals(CBioPortalMutationFreqNetworkAnnotator.IQUERY_MUTFREQ_LIST)){
                        assertEquals("PTCH1::2.0", nae.getValues().get(0));
                    }
                    
                } else if (nodeId == 5L){
                    if (nae.getName().equals(CBioPortalMutationFreqNetworkAnnotator.IQUERY_LABEL)){
                        assertEquals("FURIN (?)", nae.getValue());
                    } else if (nae.getName().equals(CBioPortalMutationFreqNetworkAnnotator.IQUERY_MUTFREQ_LIST)){
                        assertEquals("FURIN::?", nae.getValues().get(0));
                    }
                }
                else if (nodeId == 8L){
                    if (nae.getName().equals(CBioPortalMutationFreqNetworkAnnotator.IQUERY_LABEL)){
                        assertTrue("MAPK9 (?),DKK1 (4.0%)".equals(nae.getValue()) ||
                                "DKK1 (4.0%),MAPK9 (?)".equals(nae.getValue()));
                    } else if (nae.getName().equals(CBioPortalMutationFreqNetworkAnnotator.IQUERY_MUTFREQ_LIST)){
                        assertTrue(("MAPK9::?".equals(nae.getValues().get(0)) &&
                                "DKK1::4.0".equals(nae.getValues().get(1))) ||
                                ("MAPK9::?".equals(nae.getValues().get(1)) &&
                                "DKK1::4.0".equals(nae.getValues().get(0))));
                        
                    }
                }
            }
        }
    }
        
}
