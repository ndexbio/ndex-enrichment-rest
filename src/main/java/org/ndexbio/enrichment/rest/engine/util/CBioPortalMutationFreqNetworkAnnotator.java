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
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
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

    public static final String MUTATION_FREQUENCY = "mutation";
    
    public static final String IQUERY_LABEL = "iquerylabel";
    
    public static final String IQUERY_MUTFREQ_LIST = "iquery::mutationfrequency";
    
    
    static Logger _logger = LoggerFactory.getLogger(CBioPortalMutationFreqNetworkAnnotator.class);
    private InternalDatabaseResults _idr;
    private LabelNetworkAnnotator _labelAnnotator;
    
    public CBioPortalMutationFreqNetworkAnnotator(InternalDatabaseResults idr){
        _idr = idr;
        _labelAnnotator = new LabelNetworkAnnotator("COL=" +
                CBioPortalMutationFreqNetworkAnnotator.IQUERY_LABEL + ",T=string", null);
    }
    
    @Override
    public void annotateNetwork(NiceCXNetwork cxNetwork, EnrichmentQuery query, EnrichmentQueryResult eqr) throws EnrichmentException {        

        if (query == null){
            _logger.info("query is null");
            return;
        }
        
        if (query.getGeneAnnotationServices() == null){
            _logger.info("TODO need to return if geneAnnotationServices is null");
            
        }
        
        String mutationFrequencyURL = "Uncomment this later"; //query.getGeneAnnotationServices().get(CBioPortalMutationFreqNetworkAnnotator.MUTATION_FREQUENCY);
        
        if (mutationFrequencyURL == null){
            _logger.error("TODO: need to return if mutation URL is null");
            
        }

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
            _logger.info("No genes for network with ID: " + eqr.getNetworkUUID());
            return;
        }

        Map<Long, Set<String>> nodeIdToGenes = this.getNodeIdToGenesMap(geneToNodeMap);
        
        Map<String, Double> mutFreqMap = getMutationFrequency(mutationFrequencyURL,
                geneToNodeMap.keySet());
        
        if (mutFreqMap == null || mutFreqMap.isEmpty()){
            _logger.warn("No mutation frequency data obtained");
            return;
        }
        long nodeAttrCntr = this.getNodeAttributesElementCounter(cxNetwork);
        Set<String> geneSet = null;
        
        NodeAttributesElement nae = null;
        StringBuilder sb = new StringBuilder();
        Map<Long, NodesElement> nodes = cxNetwork.getNodes();
        
        for (Long nodeId : nodes.keySet()){
            geneSet = nodeIdToGenes.get(nodeId);
            if (geneSet == null || geneSet.isEmpty()){
                // we need to set the iquery label to the "name" column or 
                // ideally what ever was used as the "name" column
                nae = new NodeAttributesElement(nodeId,
                            CBioPortalMutationFreqNetworkAnnotator.IQUERY_LABEL,
                    nodes.get(nodeId).getNodeName(),
                                    ATTRIBUTE_DATA_TYPE.STRING);
                    cxNetwork.addNodeAttribute(nae);
                    nodeAttrCntr++;
                continue;
            }
            // okay we have 1 or more genes
            // dump into list with format GENE::mutationFrequency
            List<String> mutFreqs = new ArrayList<>();
            sb.setLength(0);
            for (String gene : geneSet){
                mutFreqs.add(gene + "::" + mutFreqMap.get(gene).toString());
                if (sb.length() > 0){
                    sb.append(",");
                }
                sb.append(gene);
                sb.append(" ");
                sb.append(String.format("%.1f", mutFreqMap.get(gene)));
                sb.append("%");
                
            }
            _logger.info("Adding iquery::mutationfrequency attribute to (" 
                    + Long.toString(nodeId) + ") with: " + mutFreqs.toString());
            nae = new NodeAttributesElement(nodeId,
                            CBioPortalMutationFreqNetworkAnnotator.IQUERY_MUTFREQ_LIST,
                    mutFreqs,
                                    ATTRIBUTE_DATA_TYPE.LIST_OF_STRING);
                    cxNetwork.addNodeAttribute(nae);
                    nodeAttrCntr++;
                    
            nae = new NodeAttributesElement(nodeId,
                            CBioPortalMutationFreqNetworkAnnotator.IQUERY_LABEL,
                    sb.toString(),
                                    ATTRIBUTE_DATA_TYPE.STRING);
                    cxNetwork.addNodeAttribute(nae);
                    nodeAttrCntr++;
            
        }
        _labelAnnotator.annotateNetwork(cxNetwork, query, eqr);
        _logger.debug("Updating node attributes counter to " + Long.toString(nodeAttrCntr));
	cxNetwork.getMetadata().setElementCount(NodeAttributesElement.ASPECT_NAME, nodeAttrCntr);

    }
    
    /**
     * Flips {@code geneToNodeMap} parameter where key is node Id and value is a set of 
     * genes that belong to the node
     * @param geneToNodeMap Map of genes to node Ids with that gene
     * @return Map of node Id to genes on that node
     */
    private Map<Long, Set<String>> getNodeIdToGenesMap(final Map<String, Set<Long>> geneToNodeMap){
        Map<Long, Set<String>> nodeIdToGenes = new HashMap<>();
        Set<Long> nodeIdForGenes;
        Set<String> geneSet = null;
        for (String gene : geneToNodeMap.keySet()){
            nodeIdForGenes = geneToNodeMap.get(gene);
            for (Long nodeId : nodeIdForGenes){
                geneSet = nodeIdToGenes.get(nodeId);
                if (geneSet == null){
                    geneSet = new HashSet<>();
                    nodeIdToGenes.put(nodeId, geneSet);
                }
                geneSet.add(gene);
            }
        }
        return nodeIdToGenes;
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
    
    /**
     * Gets mutation frequency for genes passed in
     * 
     * @TODO Replace this random generator with call to 
     * 
     * @param genes Set of genes to get mutation frequency for
     * @return Map where key is gene and value is mutation frequency where 0.0 is 0% and 
     *         100 is 100%
     */
    public Map<String, Double> getMutationFrequency(final String mutationFrequencyURL, Set<String> genes){
        
        Map<String, Double> mutFreqs = new HashMap<>();
        if (genes == null){
            return mutFreqs;
        }
        for(String gene : genes){
            mutFreqs.put(gene, Math.random()*100.0);
        }
        return mutFreqs;
    }
}
