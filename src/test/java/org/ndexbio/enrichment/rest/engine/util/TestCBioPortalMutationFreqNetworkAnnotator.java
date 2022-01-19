package org.ndexbio.enrichment.rest.engine.util;


import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.model.cx.NiceCXNetwork;

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
        
}
