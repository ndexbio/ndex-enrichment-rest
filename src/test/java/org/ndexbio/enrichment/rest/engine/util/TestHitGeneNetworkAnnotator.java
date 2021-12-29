package org.ndexbio.enrichment.rest.engine.util;


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
import org.ndexbio.model.cx.NiceCXNetwork;

/**
 *
 * @author churas
 */
public class TestHitGeneNetworkAnnotator {
	
	@Test
        public void testNullNetwork(){
            HitGeneNetworkAnnotator annotator = new HitGeneNetworkAnnotator(new InternalDatabaseResults(), null);

            try {
                annotator.annotateNetwork(null, new EnrichmentQueryResult());
                fail("expected EnrichmentException");
            } catch(EnrichmentException ee){
                assertEquals("network is null", ee.getMessage());
            }
        }
        
        @Test
        public void testNullEnrichmentQueryResult(){
            HitGeneNetworkAnnotator annotator = new HitGeneNetworkAnnotator(new InternalDatabaseResults(), null);

            try {
                annotator.annotateNetwork(new NiceCXNetwork(), null);
                fail("expected EnrichmentException");
            } catch(EnrichmentException ee){
                assertEquals("EnrichmentQueryResult is null", ee.getMessage());
            }
        }

        @Test
        public void testNullNetworkUUID(){
            HitGeneNetworkAnnotator annotator = new HitGeneNetworkAnnotator(new InternalDatabaseResults(), null);

            try {
                
                annotator.annotateNetwork(new NiceCXNetwork(),
                        new EnrichmentQueryResult());
                fail("expected EnrichmentException");
            } catch(EnrichmentException ee){
                assertEquals("network UUID is null", ee.getMessage());
            }
        }
        
        @Test
        public void testAnnotateOnValidNetworkWhereGeneToNodeMapIsNull() throws EnrichmentException {
            EnrichmentQueryResult eqr = new EnrichmentQueryResult();
            eqr.setNetworkUUID(UUID.randomUUID().toString());
            InternalDatabaseResults idr = new InternalDatabaseResults();
            
            Map<String, Map<String, Set<Long>>> geneMap = new HashMap<>();
            idr.setNetworkToGeneToNodeMap(geneMap);
            
            HitGeneNetworkAnnotator annotator = new HitGeneNetworkAnnotator(idr, null);
            
            NiceCXNetwork net = new NiceCXNetwork();
            
            annotator.annotateNetwork(net, eqr);
            assertNotNull(net.getMetadata());
            assertEquals(0L, (long)net.getMetadata().getElementCount(NodeAttributesElement.ASPECT_NAME));
        }
        
        @Test
        public void testAnnotateOnValidNetwork() throws EnrichmentException {
            EnrichmentQueryResult eqr = new EnrichmentQueryResult();
            eqr.setNetworkUUID(UUID.randomUUID().toString());
            Set<String> hitGenes = new HashSet<>();
            hitGenes.add("node1");
            hitGenes.add("node3");
            eqr.setHitGenes(hitGenes);
            InternalDatabaseResults idr = new InternalDatabaseResults();

            NiceCXNetwork net = new NiceCXNetwork();
            
            net.addNode(new NodesElement(1, "node1", "node1_rep"));
            net.addNode(new NodesElement(2, "node2", "node2_rep"));
            net.addNode(new NodesElement(3, "node3", "node3_rep"));
            // we are pretending node4 is a special node that matches
            // node 3
            net.addNode(new NodesElement(4, "node4", "node4_rep"));
            
            Map<String, Map<String, Set<Long>>> geneMap = new HashMap<>();
            Map<String, Set<Long>> netGeneMap = new HashMap<>();
            netGeneMap.put("node1", new HashSet<>(Arrays.asList(1L)));
            netGeneMap.put("node3", new HashSet<>(Arrays.asList(3L, 4L)));
            
            geneMap.put(eqr.getNetworkUUID(), netGeneMap);
            
            idr.setNetworkToGeneToNodeMap(geneMap);
            
            HitGeneNetworkAnnotator annotator = new HitGeneNetworkAnnotator(idr, null);
         
            annotator.annotateNetwork(net, eqr);
            assertNotNull(net.getMetadata());
            assertEquals(3L, (long)net.getMetadata().getElementCount(NodeAttributesElement.ASPECT_NAME));

            Map<Long, Collection<NodeAttributesElement>> nodeAttrsMap = net.getNodeAttributes();
            
            assertEquals(3, nodeAttrsMap.size());
            
            for (Long nodeId : Arrays.asList(1L, 3L, 4L)){
                Collection<NodeAttributesElement> nodeCol = nodeAttrsMap.get(nodeId);
                assertEquals(1, nodeCol.size());
                NodeAttributesElement nae = nodeCol.iterator().next();
                assertEquals(HitGeneNetworkAnnotator.DEFAULT_QUERYNODE_ATTR_NAME,
                        nae.getName());
                assertEquals("true", nae.getValue());
                assertEquals(ATTRIBUTE_DATA_TYPE.BOOLEAN, nae.getDataType());
            }            
            
        }
        
        @Test
        public void testAnnotateOnValidNetworkCustomAttrName() throws EnrichmentException {
            EnrichmentQueryResult eqr = new EnrichmentQueryResult();
            eqr.setNetworkUUID(UUID.randomUUID().toString());
            Set<String> hitGenes = new HashSet<>();
            hitGenes.add("node1");
            hitGenes.add("node3");
            eqr.setHitGenes(hitGenes);
            InternalDatabaseResults idr = new InternalDatabaseResults();

            NiceCXNetwork net = new NiceCXNetwork();
            
            net.addNode(new NodesElement(1, "node1", "node1_rep"));
            net.addNode(new NodesElement(2, "node2", "node2_rep"));
            net.addNode(new NodesElement(3, "node3", "node3_rep"));
            net.addNode(new NodesElement(4, "node4", "node4_rep"));
            
            net.getMetadata().setElementCount(NodeAttributesElement.ASPECT_NAME, 5L);
            Map<String, Map<String, Set<Long>>> geneMap = new HashMap<>();
            Map<String, Set<Long>> netGeneMap = new HashMap<>();
            netGeneMap.put("node1", new HashSet<>(Arrays.asList(1L)));
            
            geneMap.put(eqr.getNetworkUUID(), netGeneMap);
            
            idr.setNetworkToGeneToNodeMap(geneMap);
            
            HitGeneNetworkAnnotator annotator = new HitGeneNetworkAnnotator(idr, "foo");
         
            annotator.annotateNetwork(net, eqr);
            assertNotNull(net.getMetadata());
            assertEquals(6L, (long)net.getMetadata().getElementCount(NodeAttributesElement.ASPECT_NAME));

            Map<Long, Collection<NodeAttributesElement>> nodeAttrsMap = net.getNodeAttributes();
            
            assertEquals(1, nodeAttrsMap.size());
            
            for (Long nodeId : Arrays.asList(1L)){
                Collection<NodeAttributesElement> nodeCol = nodeAttrsMap.get(nodeId);
                assertEquals(1, nodeCol.size());
                NodeAttributesElement nae = nodeCol.iterator().next();
                assertEquals("foo",
                        nae.getName());
                assertEquals("true", nae.getValue());
                assertEquals(ATTRIBUTE_DATA_TYPE.BOOLEAN, nae.getDataType());
            }            
            
        }

}
