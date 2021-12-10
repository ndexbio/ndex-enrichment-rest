package org.ndexbio.enrichment.rest.engine.util;

import java.util.Map;
import java.util.Set;
import org.ndexbio.cxio.aspects.datamodels.ATTRIBUTE_DATA_TYPE;
import org.ndexbio.cxio.aspects.datamodels.NodeAttributesElement;
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
        
        long nodeAttrCntr = -1;
        try {
            nodeAttrCntr = cxNetwork.getMetadata().getElementCount(NodeAttributesElement.ASPECT_NAME);
        } catch(NullPointerException npe){
            _logger.error("No element counter for " + NodeAttributesElement.ASPECT_NAME);
        }
        
        // @TODO query to get mutation map where key is gene & value is mutation frequency
        try {
            Thread.sleep(1000);
        } catch(InterruptedException ie){
            // do nothing
        }
        for (String gene : geneToNodeMap.keySet()){
            // @TODO get mutation frequency for gene
            double mutationFrequency = Math.random()*100.0;
            
            // @TODO need to handle complex nodes cause they will have 
            //       multiple mutation frequencies
            
            Set<Long> nodeIdSet = geneToNodeMap.get(gene);
            if (nodeIdSet != null){
                for (Long nodeId : nodeIdSet){
                    NodeAttributesElement nae = new NodeAttributesElement(nodeId,
                            "iquery::mutationfrequency", 
                            Double.toString(mutationFrequency),
                                    ATTRIBUTE_DATA_TYPE.DOUBLE);
                    cxNetwork.addNodeAttribute(nae);
                    nodeAttrCntr++;
                }
            }
            
        }
        _logger.debug("Updating node attributes counter to " + Long.toString(nodeAttrCntr));
	cxNetwork.getMetadata().setElementCount(NodeAttributesElement.ASPECT_NAME, nodeAttrCntr);

    }
    
    
}
