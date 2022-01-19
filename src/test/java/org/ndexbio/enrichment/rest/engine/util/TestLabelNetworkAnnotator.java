package org.ndexbio.enrichment.rest.engine.util;


import static org.junit.Assert.*;
import org.junit.Test;

import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;

import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;

import org.ndexbio.enrichment.rest.TestApp;
import org.ndexbio.model.cx.NiceCXNetwork;
import org.ndexbio.rest.client.NdexRestClientUtilities;

/**
 *
 * @author churas
 */
public class TestLabelNetworkAnnotator {
    
    @Test
    public void testNullNetwork(){
        LabelNetworkAnnotator annotator = new LabelNetworkAnnotator("foo", null);

        try {
            annotator.annotateNetwork(null, null, new EnrichmentQueryResult());
            fail("expected EnrichmentException");
        } catch(EnrichmentException ee){
            assertEquals("network is null", ee.getMessage());
        }
    }

    @Test
    public void testChangingGlypican3NetworkLabel() throws Exception {
        NiceCXNetwork net = NdexRestClientUtilities.getCXNetworkFromStream(TestApp.class.getClassLoader().getResourceAsStream("glypican_3_network.cx"));
         LabelNetworkAnnotator annotator = new LabelNetworkAnnotator("COL=ha,T=string", "null");
         annotator.annotateNetwork(net, null, null);
         
         // @TODO need to test that style was updated on network

    }
        
        
}
