package org.ndexbio.enrichment.rest.engine.util;


import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import static org.junit.Assert.*;
import org.junit.Test;
import org.ndexbio.cxio.aspects.datamodels.ATTRIBUTE_DATA_TYPE;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.cxio.aspects.datamodels.NodeAttributesElement;
import org.ndexbio.cxio.aspects.datamodels.NodesElement;
import org.ndexbio.cxio.core.NdexCXNetworkWriter;
import org.ndexbio.cxio.core.writers.NiceCXNetworkWriter;
import org.ndexbio.enrichment.rest.TestApp;
import static org.ndexbio.enrichment.rest.engine.util.LabelNetworkAnnotator._logger;
import org.ndexbio.model.cx.NiceCXNetwork;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.rest.client.NdexRestClientUtilities;

/**
 *
 * @author churas
 */
public class TestLabelNetworkAnnotator {
	
	
    public void saveNetwork(NiceCXNetwork cxNetwork){
        String destFile = "/Users/churas/well_hello.cx";
        try (FileOutputStream fos = new FileOutputStream(destFile)) {
			
                        
                NdexCXNetworkWriter ndexwriter = new NdexCXNetworkWriter(fos, true);
                NiceCXNetworkWriter writer = new NiceCXNetworkWriter(ndexwriter);
                writer.writeNiceCXNetwork(cxNetwork);  
        }
        catch(IOException ex){
                _logger.error("problems writing cx", ex);
        }
        catch(NdexException nex){
                _logger.error("Problems writing network as cx", nex);
        }
    }
    
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

    }
        
        
}
