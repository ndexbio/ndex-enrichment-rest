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
 * Annotates nodes in Network with matching query genes
 *
 * @author churas
 */
public class HitGeneNetworkAnnotator implements NetworkAnnotator {
    
    static Logger _logger = LoggerFactory.getLogger(HitGeneNetworkAnnotator.class);
    
    public static final String DEFAULT_QUERYNODE_ATTR_NAME = "querynode";
    private InternalDatabaseResults _idr;
    private String _queryNodeAttrName;

    /**
     * Constructor
     * @param idr Used to get map of genes to node ids for each network
     * @param queryNodeAttributeName Query Node Attribute name, if {@code null}
     *                               then {@link #DEFAULT_QUERYNODE_ATTR_NAME} is used
     */
    public HitGeneNetworkAnnotator(InternalDatabaseResults idr, 
            final String queryNodeAttributeName){
        _idr = idr;
        
        if (queryNodeAttributeName == null){
            _queryNodeAttrName = HitGeneNetworkAnnotator.DEFAULT_QUERYNODE_ATTR_NAME;
        } else {
            _queryNodeAttrName = queryNodeAttributeName;
        }
    }
    
    /**
     * Annotates network passed in via {@code cxNetwork} by adding a 
     * {@link ATTRIBUTE_DATA_TYPE#BOOLEAN} 
     * node attribute with name set in constructor for all 
     * {@link EnrichmentQueryResult#getHitGenes()} found in network. 
     * 
     * @param cxNetwork Network to annotate
     * @param eqr Query result containing matching hit genes
     * @throws EnrichmentException if {@code cxNetwork}, {@code eqr}, or network UUID is {@code null}
     */
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
        
        long nodeAttrCntr = 0;
        try {
            nodeAttrCntr = cxNetwork.getMetadata().getElementCount(NodeAttributesElement.ASPECT_NAME);
        } catch(NullPointerException npe){
            _logger.error("No element counter for " + NodeAttributesElement.ASPECT_NAME
                            + " using 0");
        }
        Map<String, Set<Long>> geneToNodeMap = _idr.getNetworkToGeneToNodeMap().get(eqr.getNetworkUUID());
        if (geneToNodeMap != null){
            for (String hitGene : eqr.getHitGenes()){
                Set<Long> nodeIdSet = geneToNodeMap.get(hitGene);
                if (nodeIdSet != null){
                    for (Long nodeId : nodeIdSet){
                        NodeAttributesElement nae = new NodeAttributesElement(nodeId,
                                _queryNodeAttrName, "true",
                                        ATTRIBUTE_DATA_TYPE.BOOLEAN);
                        cxNetwork.addNodeAttribute(nae);
                        nodeAttrCntr++;
                    }
                }
            }
        }
        _logger.debug("Updating node attributes counter to " + Long.toString(nodeAttrCntr));
        cxNetwork.getMetadata().setElementCount(NodeAttributesElement.ASPECT_NAME, nodeAttrCntr);
                        
    }
    
}
