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
import org.ndexbio.ndexsearch.rest.model.GeneList;
import org.ndexbio.ndexsearch.rest.model.MutationFrequencies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instances of this class queries the Mutation Frequency Service from CBioPortal 
 * to get mutation frequencies for genes in the network passed in and updates 
 * the network's style to show an alternate
 * attribute added which has the gene name and mutation frequency updated via the 
 * {@link #annotateNetwork(org.ndexbio.model.cx.NiceCXNetwork, org.ndexbio.enrichment.rest.model.EnrichmentQuery, org.ndexbio.enrichment.rest.model.EnrichmentQueryResult) }
 * call
 * 
 * @author churas
 */
public class CBioPortalMutationFreqNetworkAnnotator implements NetworkAnnotator {

    /**
     * Name of service containing mutation frequency found in result
     * obtained from REST service call
     */
    public static final String MUTATION_FREQUENCY = "mutation";
    
    /**
     * Attribute name containing alternate label that includes the
     * gene name along with mutation frequency
     */
    public static final String IQUERY_LABEL = "iquerylabel";
    
    /**
     * List attribute name that contains a list of genes along with their
     * mutation frequencies. 
     */
    public static final String IQUERY_MUTFREQ_LIST = "iquerymutationfrequency";
    
    /**
     * Used in place of mutation frequency where a value was not received
     * from service
     */
    public static final String UNKNOWN_FREQ = "?";
    
    /**
     * Delimiter in {@link #IQUERY_MUTFREQ_LIST} attribute items that separates
     * the gene name from the mutation frequency
     */
    public static final String FREQ_ARRAY_DELIMITER = "::";
    
    
    static Logger _logger = LoggerFactory.getLogger(CBioPortalMutationFreqNetworkAnnotator.class);
    private InternalDatabaseResults _idr;
    private NetworkAnnotator _labelAnnotator;
    private MutationFrequencyRestClient _client;
    
    /**
     * Constructor
     * 
     * @param idr Database for Enrichment service 
     * @throws EnrichmentException if {@code idr} is {@code null} or 
     *        {@link org.ndexbio.enrichment.rest.model.InternalDatabaseResults#getNetworkToGeneToNodeMap() } is {@code null}
     */
    public CBioPortalMutationFreqNetworkAnnotator(InternalDatabaseResults idr) throws EnrichmentException{
        if (idr == null){
            throw new EnrichmentException("InternalDatabaseResults is null");
        }
        if (idr.getNetworkToGeneToNodeMap() == null){
            throw new EnrichmentException("InternalDatabaseResults NetworkToGeneToNodeMap is null");
        }
        
        _idr = idr;
        _labelAnnotator = null;
        //_labelAnnotator = new LabelNetworkAnnotator("COL=" +
        //        CBioPortalMutationFreqNetworkAnnotator.IQUERY_LABEL + ",T=string", null);
        _client = new MutationFrequencyRestClientImpl();
    }
    
    /**
     * Sets alternate MutationFrequencyRestClient
     * @param client Alternate client to use
     */
    protected void setAlternateMutationFrequencyRestClient(MutationFrequencyRestClient client){
        _client = client;
    }
    
    /**
     * Sets alternate Network Label annotator
     * @param labelAnnotator 
     */
    protected void setAlternateNetworkLabelAnnotator(NetworkAnnotator labelAnnotator){
        _labelAnnotator = labelAnnotator;
    }
    
    private String validateInputsAndGetMutationService(NiceCXNetwork cxNetwork,
            EnrichmentQuery query, EnrichmentQueryResult eqr) throws EnrichmentException {
        
        if (cxNetwork == null){
            throw new EnrichmentException("network is null");
        }

        if (eqr == null){
            throw new EnrichmentException("EnrichmentQueryResult is null");
        }
        
        if (eqr.getNetworkUUID() == null){
            throw new EnrichmentException("network UUID is null");
        }
        
        if (_client == null){
            throw new EnrichmentException("REST client is null");
        }
        
        if (query == null){
            _logger.info("query is null, no annotation performed");
            return null;
        }
        
        Map<String, String> annotationServices = query.getGeneAnnotationServices();
        if (annotationServices == null || annotationServices.isEmpty()){
            _logger.debug("geneAnnotationServices is null or empty, no annotation performed");
            return null;
        }
        
        String mutationFrequencyURL = annotationServices.get(CBioPortalMutationFreqNetworkAnnotator.MUTATION_FREQUENCY);
        
        if (mutationFrequencyURL == null){
            _logger.error("mutation URL is null, no annotation performed");
            return null;            
        }
        return mutationFrequencyURL;
    }
    
    /**
     * Annotates the {@code cxNetwork} with mutation frequency data.
     * 
     * This is done by querying the 
     * REST endpoint specified under {@code query.geneAnnotationServices.get("mutation"}}
     * and adding that mutation information via two network attributes {@link #IQUERY_LABEL} and
     * {@link #IQUERY_MUTFREQ_LIST}
     * 
     * The {@link #IQUERY_LABEL} is a node attribute that will contain a string 
     * GENE MUTFREQ% for genes in the query or the original node name.
     * 
     * The {@link #IQUERY_MUTFREQ_LIST} is a node attribute that will contain a list
     * of strings with format GENE::MUTFREQ where :: is the delimiter defined by {@link #FREQ_ARRAY_DELIMITER}
     * 
     * To make the new labels visible this method adjusts the style in the {@code cxNetwork}
     * to display the {@link #IQUERY_LABEL} attribute as the node label. This is done
     * via the {@link LabelNetworkAnnotator} created internally in the constructor.
     * 
     * @param cxNetwork Network to annotate in place
     * @param query The query sent in to the service, which is needed to get the REST
     *              end point to get the mutation frequencies
     * @param eqr The result of the query
     * @throws EnrichmentException If there was a problem
     */
    @Override
    public void annotateNetwork(NiceCXNetwork cxNetwork, EnrichmentQuery query, EnrichmentQueryResult eqr) throws EnrichmentException {        

        final String mutationFrequencyURL = validateInputsAndGetMutationService(cxNetwork,
                query, eqr);
        
        if (mutationFrequencyURL == null){
            // something was invalid or unset just return
            return;
        }
        
        Map<String, Set<Long>> geneToNodeMap = _idr.getNetworkToGeneToNodeMap().get(eqr.getNetworkUUID());
        if (geneToNodeMap == null || geneToNodeMap.isEmpty()){
            _logger.info("No genes for network with ID: " + eqr.getNetworkUUID());
            return;
        }

        Map<Long, Set<String>> nodeIdToGenes = getNodeIdToGenesMap(geneToNodeMap);

        Map<String, Double> mutFreqMap = getMutationFrequency(mutationFrequencyURL,
                geneToNodeMap.keySet());

        long nodeAttrCntr = this.getNodeAttributesElementCounter(cxNetwork);
        Set<String> geneSet;

        NodeAttributesElement nae;
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
                Double freq = mutFreqMap.get(gene);
                if (freq == null){
                    freq = Double.NaN;
                }
                if (sb.length() > 0){
                    sb.append(",");
                }
                sb.append(gene);
                sb.append(" (");
                
                if (Double.isNaN(freq)){
                    mutFreqs.add(gene
                            + CBioPortalMutationFreqNetworkAnnotator.FREQ_ARRAY_DELIMITER 
                            + CBioPortalMutationFreqNetworkAnnotator.UNKNOWN_FREQ);
                    sb.append(CBioPortalMutationFreqNetworkAnnotator.UNKNOWN_FREQ);
                } else {
                    mutFreqs.add(gene 
                            + CBioPortalMutationFreqNetworkAnnotator.FREQ_ARRAY_DELIMITER
                            + freq.toString());
                   
                    sb.append(String.format("%.1f", freq));
                    sb.append("%");
                }
                sb.append(")");
            }
            _logger.debug("Adding " + CBioPortalMutationFreqNetworkAnnotator.IQUERY_MUTFREQ_LIST
                    + " attribute to (" 
                    + Long.toString(nodeId) + ") with: " + mutFreqs.toString());
            nae = new NodeAttributesElement(nodeId,
                            CBioPortalMutationFreqNetworkAnnotator.IQUERY_MUTFREQ_LIST,
                    mutFreqs,
                                    ATTRIBUTE_DATA_TYPE.LIST_OF_STRING);
                    cxNetwork.addNodeAttribute(nae);
                    nodeAttrCntr++;
            _logger.debug("Adding " + CBioPortalMutationFreqNetworkAnnotator.IQUERY_LABEL
                    + " attribute to (" 
                    + Long.toString(nodeId) + ") with: " + sb.toString());
                    
            nae = new NodeAttributesElement(nodeId,
                            CBioPortalMutationFreqNetworkAnnotator.IQUERY_LABEL,
                    sb.toString(),
                                    ATTRIBUTE_DATA_TYPE.STRING);
                    cxNetwork.addNodeAttribute(nae);
                    nodeAttrCntr++;
            
        }
        if (_labelAnnotator != null){
            _labelAnnotator.annotateNetwork(cxNetwork, query, eqr);
        }
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
     * Gets mutation frequency for genes passed in by querying REST endpoint
     * 
     * @param genes Set of genes to get mutation frequency for
     * @return Map where key is gene and value is mutation frequency where 0.0 is 0% and 
     *         100 is 100%
     */
    private Map<String, Double> getMutationFrequency(final String mutationFrequencyURL,
            Set<String> genes){
        
        GeneList geneList = new GeneList();

        List<String> aList = new ArrayList<>();
        aList.addAll(genes);
        geneList.setGenes(aList);
        try {
            MutationFrequencies mutFreqs = _client.getMutationFrequencies(mutationFrequencyURL,
                    geneList);
            
            return mutFreqs.getMutationFrequencies();
        } catch(EnrichmentException ee){
            _logger.error("Caught exception, going to just "
                    + " return empty Map : " + ee.getMessage(), ee);
            return new HashMap<>();
        } catch(NullPointerException npe){
            _logger.error("Caught NPE exception, going to just return " 
                    + " empty Map : " + npe.getMessage(), npe);
            return new HashMap<>();
        }
    }
}
