package org.ndexbio.enrichment.rest.engine.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import org.ndexbio.cxio.aspects.datamodels.ATTRIBUTE_DATA_TYPE;
import org.ndexbio.cxio.aspects.datamodels.NodeAttributesElement;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.model.cx.NiceCXNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets query genes found in the network to selected
 * @author churas
 */
public class SetQueryGeneSelectedNetworkAnnotator implements NetworkAnnotator {

    
    /**
     * Attribute that denotes in Cytoscape if a Node or edge is selected 
     */
    public static final String SELECTED = "selected";
    
    static Logger _logger = LoggerFactory.getLogger(SetQueryGeneSelectedNetworkAnnotator.class);
    private InternalDatabaseResults _idr;
    
    /**
     * Constructor
     * 
     * @param idr Database for Enrichment service 
     * @throws EnrichmentException if {@code idr} is {@code null} or 
     *        {@link org.ndexbio.enrichment.rest.model.InternalDatabaseResults#getNetworkToGeneToNodeMap() } is {@code null}
     */
    public SetQueryGeneSelectedNetworkAnnotator(InternalDatabaseResults idr) throws EnrichmentException{
        if (idr == null){
            throw new EnrichmentException("InternalDatabaseResults is null");
        }
        if (idr.getNetworkToGeneToNodeMap() == null){
            throw new EnrichmentException("InternalDatabaseResults NetworkToGeneToNodeMap is null");
        }
        
        _idr = idr;
    }
    
    /**
     * Sets {@code selected} attribute to {@code True} for all query genes found
     * in network. This tells viewers to highlight those nodes
     * 
     * @param cxNetwork Network to annotate in place
     * @param query Query sent to the service
     * @param eqr The result of the query
     * @throws EnrichmentException If there was a problem
     */
    @Override
    public void annotateNetwork(NiceCXNetwork cxNetwork, EnrichmentQuery query, EnrichmentQueryResult eqr) throws EnrichmentException {
        if (cxNetwork == null){
            throw new EnrichmentException("network is null");
        }

        if (eqr == null){
            throw new EnrichmentException("EnrichmentQueryResult is null");
        }
        
        if (eqr.getNetworkUUID() == null){
            throw new EnrichmentException("network UUID is null");
        }
        
        if (eqr.getHitGenes() == null || eqr.getHitGenes().isEmpty()){
            _logger.debug("No hits on this network. weird");
            return;
        }
        // get mapping of genes to node Ids
        Map<String, Set<Long>> geneToNodeMap = _idr.getNetworkToGeneToNodeMap().get(eqr.getNetworkUUID());
        if (geneToNodeMap == null || geneToNodeMap.isEmpty()){
            _logger.info("No genes for network with ID: " + eqr.getNetworkUUID());
            return;
        }
        
        long nodeAttrCntr = this.getNodeAttributesElementCounter(cxNetwork);

        // iterate through the hits and add selected node attribute with
        // value set to true. If selected node attribute exists, just make
        // sure it is set to true
        for (String gene : eqr.getHitGenes()){
            if (gene == null){
                continue;
            }
            Set<Long> hitGeneNodeIds = geneToNodeMap.get(gene);
            if (hitGeneNodeIds == null){
                continue;
            }
            for (Long hitNodeId : hitGeneNodeIds){
                NodeAttributesElement selectedAttr = getSelectedNodeAttributesElement(cxNetwork, hitNodeId);
                if (selectedAttr == null){
                    selectedAttr = new NodeAttributesElement(hitNodeId,
                            SetQueryGeneSelectedNetworkAnnotator.SELECTED,
                            "true",
                                    ATTRIBUTE_DATA_TYPE.BOOLEAN);
                    cxNetwork.addNodeAttribute(selectedAttr);
                    nodeAttrCntr++;
                } else {
                    selectedAttr.setSingleStringValue("true");
                }
            }
        }
        _logger.debug("Updating node attributes counter to " + Long.toString(nodeAttrCntr));
        if (cxNetwork.getMetadata() != null){
            cxNetwork.getMetadata().setElementCount(NodeAttributesElement.ASPECT_NAME, nodeAttrCntr);
        }
        
    }
    
    private NodeAttributesElement getSelectedNodeAttributesElement(NiceCXNetwork cxNetwork,
            final Long nodeId){
        if (cxNetwork.getNodeAttributes() == null){
            _logger.error("Network does not have any node attributes");
            return null;
        }
        Collection <NodeAttributesElement> nodeAttrs = cxNetwork.getNodeAttributes().get(nodeId);
        if (nodeAttrs == null || nodeAttrs.isEmpty()){
            return null;
        }
        for (NodeAttributesElement nodeAttribute : nodeAttrs){
            if (nodeAttribute.getName() == null){
                continue;
            }
            if (nodeAttribute.getName().equals(SetQueryGeneSelectedNetworkAnnotator.SELECTED)){
                return nodeAttribute;
            }
        }
        return null;
    }
    /**
     * Gets the value of the node attributes counter from meta data
     * 
     * @param cxNetwork
     * @return value of nodes attribute counter or {@code 0} if not found
     */
    private long getNodeAttributesElementCounter(NiceCXNetwork cxNetwork){
        try {
            return cxNetwork.getMetadata().getElementCount(NodeAttributesElement.ASPECT_NAME);
        } catch(NullPointerException npe){
            _logger.error("No element counter for " + NodeAttributesElement.ASPECT_NAME);
        }
        return 0;
    }
}
