package org.ndexbio.enrichment.rest.engine.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ndexbio.cxio.aspects.datamodels.ATTRIBUTE_DATA_TYPE;
import org.ndexbio.cxio.aspects.datamodels.NodeAttributesElement;
import org.ndexbio.cxio.aspects.datamodels.NodesElement;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.model.cx.NiceCXNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * THIS IS BROKEN AND MORE OF A PSEUDO CODE AT THE MOMENT
 * @author churas
 */
public class CBioPortalMutationFreqNetworkAnnotator implements NetworkAnnotator {

    static Logger _logger = LoggerFactory.getLogger(CBioPortalMutationFreqNetworkAnnotator.class);
    private InternalDatabaseResults _idr;
    
    public CBioPortalMutationFreqNetworkAnnotator(InternalDatabaseResults idr){
        _idr = idr;
    }
    
    @Override
    public void annotateNetwork(NiceCXNetwork cxNetwork, EnrichmentQueryResult eqr) throws EnrichmentException {        
        if (cxNetwork == null){
            throw new EnrichmentException("network is null");
        }
        
        if (eqr == null){
            throw new EnrichmentException("EnrichmentQueryResult is null");
        }
        
        if (eqr.getNetworkUUID() == null){
            throw new EnrichmentException("network UUID is null");
        }
        
        Map<String, Set<Long>> geneToNodeMap = _idr.getNetworkToGeneToNodeMap().get(eqr.getNetworkUUID());
        if (geneToNodeMap == null){
            _logger.debug("No genes for network with ID: " + eqr.getNetworkUUID());
            return;
        }
        
        long nodeAttrCntr = 0;
        try {
            nodeAttrCntr = cxNetwork.getMetadata().getElementCount(NodeAttributesElement.ASPECT_NAME);
        } catch(NullPointerException npe){
            _logger.error("No element counter for " + NodeAttributesElement.ASPECT_NAME);
        }
        
        
        // @TODO query to get mutation map where key is gene & value is mutation frequency
        //       CODE NEEDS TO JUST RETURN IF NO MUTATION INFO IS FOUND
        try {
            Thread.sleep(1000);
        } catch(InterruptedException ie){
            // do nothing
        }
        
        Map<Long, Set<String>> nodeIdToGenes = new HashMap<>();
        Set<Long> nodeIdForGenes;
        Set<String> geneSet;
        for (String gene : geneToNodeMap.keySet()){
            nodeIdForGenes = geneToNodeMap.get(gene);
            for (Long nodeId : nodeIdForGenes){
                geneSet = nodeIdToGenes.get(gene);
                if (geneSet == null){
                    geneSet = new HashSet<>();
                    nodeIdToGenes.put(nodeId, geneSet);
                }
                geneSet.add(gene);
            }
        }
        
        Map<Long, NodesElement> nodes = cxNetwork.getNodes();
        for (Long nodeId : nodes.keySet()){
            geneSet = nodeIdToGenes.get(nodeId);
            if (geneSet == null){
                continue;
            }
            if (geneSet.isEmpty()){
                continue;
            }
            // okay we have 1 or more genes
            // dump into list with format GENE::mutationFrequency
            List<String> mutFreqs = new ArrayList<>();
            
            for (String gene : geneSet){
                // @TODO get mutation frequency for gene
                double mutationFrequency = Math.random()*100.0;
                mutFreqs.add(gene + "::" + Double.toString(mutationFrequency));
            }
            _logger.info("Adding iquery::mutationfrequency attribute to (" 
                    + Long.toString(nodeId) + ") with: " + mutFreqs.toString());
            NodeAttributesElement nae = new NodeAttributesElement(nodeId,
                            "iquery::mutationfrequency", mutFreqs,
                                    ATTRIBUTE_DATA_TYPE.LIST_OF_STRING);
                    cxNetwork.addNodeAttribute(nae);
                    nodeAttrCntr++;
        }
        
        _logger.debug("Updating node attributes counter to " + Long.toString(nodeAttrCntr));
	cxNetwork.getMetadata().setElementCount(NodeAttributesElement.ASPECT_NAME, nodeAttrCntr);

    }
    
    
}
