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
import org.ndexbio.ndexsearch.rest.model.AlterationData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Instances of this class takes alterationData passed in the query from CBioPortal 
 * and updates 
 * the network's style to show an alternate
 * attribute added which has the gene name and percent altered updated via the 
 * {@link #annotateNetwork(org.ndexbio.model.cx.NiceCXNetwork, org.ndexbio.enrichment.rest.model.EnrichmentQuery, org.ndexbio.enrichment.rest.model.EnrichmentQueryResult) }
 * call
 * 
 * @author churas
 */
public class CBioPortalAlterationDataNetworkAnnotator implements NetworkAnnotator {

    /**
	 * Prepended to simulated percent Altered data values
	 */
	public static final String SIMULATED_PERCENT_ALTERED_PREFIX="s";
    
    /**
     * Attribute name containing alternate label that includes the
     * gene name along with mutation frequency
     */
    public static final String IQUERY_LABEL = "iquerylabel";
    
    /**
     * List attribute name that contains a list of genes along with their
     * percent altered. 
     */
    public static final String IQUERY_PERCENTALTERED_LIST = "iquerypercentaltered";
    
    /**
     * Used in place of percent altered where a value was not received
     * 
     */
    public static final String UNKNOWN_FREQ = "?";
    
    /**
     * Delimiter in {@link #IQUERY_PERCENTALTERED_LIST} attribute items that separates
     * the gene name from the percent altered
     */
    public static final String FREQ_ARRAY_DELIMITER = "::";
    
    
    static Logger _logger = LoggerFactory.getLogger(CBioPortalAlterationDataNetworkAnnotator.class);
    private InternalDatabaseResults _idr;
    private NetworkAnnotator _labelAnnotator;
	private boolean _simulatePercentAltered;
    
    /**
     * Constructor
     * 
     * @param idr Database for Enrichment service
	 * @param simulatePercentAltered If true simulate percentAltered for genes lacking alteration data
     * @throws EnrichmentException if {@code idr} is {@code null} or 
     *        {@link org.ndexbio.enrichment.rest.model.InternalDatabaseResults#getNetworkToGeneToNodeMap() } is {@code null}
     */
    public CBioPortalAlterationDataNetworkAnnotator(InternalDatabaseResults idr, boolean simulatePercentAltered) throws EnrichmentException{
        if (idr == null){
            throw new EnrichmentException("InternalDatabaseResults is null");
        }
        if (idr.getNetworkToGeneToNodeMap() == null){
            throw new EnrichmentException("InternalDatabaseResults NetworkToGeneToNodeMap is null");
        }
        
        _idr = idr;
		_simulatePercentAltered = simulatePercentAltered;
        _labelAnnotator = null;
        _labelAnnotator = new LabelNetworkAnnotator("COL=" +
                CBioPortalMutationFreqNetworkAnnotator.IQUERY_LABEL + ",T=string", null);
    }
	
	/**
     * Constructor
     * 
     * @param idr Database for Enrichment service
     * @throws EnrichmentException if {@code idr} is {@code null} or 
     *        {@link org.ndexbio.enrichment.rest.model.InternalDatabaseResults#getNetworkToGeneToNodeMap() } is {@code null}
     */
    public CBioPortalAlterationDataNetworkAnnotator(InternalDatabaseResults idr) throws EnrichmentException{
		this(idr, false);
	}
    
	
    
    /**
     * Sets alternate Network Label annotator
     * @param labelAnnotator 
     */
    protected void setAlternateNetworkLabelAnnotator(NetworkAnnotator labelAnnotator){
        _labelAnnotator = labelAnnotator;
    }
    
    private Map<String, AlterationData> validateInputsAndGetAlterationData(NiceCXNetwork cxNetwork,
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
        
        if (query == null){
            _logger.info("query is null, no annotation performed");
            return null;
        }
		
		if (_simulatePercentAltered == false && query.getAlterationData() == null){
			_logger.info("No alteration data, no annotation performed");
			return null;
		}
		
		return getAlterationMap(query);
    }
	
	private Map<String, AlterationData> getAlterationMap(final EnrichmentQuery query){
		if (_simulatePercentAltered == true 
				&& (query.getAlterationData() == null || query.getAlterationData().isEmpty())){
			Map<String, AlterationData> alterationMap = new HashMap<>();
			if (query.getGeneList() == null || query.getGeneList().isEmpty()){
				return alterationMap;
			}
			_logger.info("Simulating percent altered");
			
			AlterationData ad = null;
			for (String gene : query.getGeneList()){
				ad = new AlterationData();
				ad.setGene(gene);
				ad.setPercentAltered(CBioPortalAlterationDataNetworkAnnotator.SIMULATED_PERCENT_ALTERED_PREFIX
						+ Long.valueOf(Math.round(Math.random()*99.0)).intValue()
				        + "%");
				alterationMap.put(gene, ad);
			}
			return alterationMap;
		}
		Map<String, AlterationData> alterationMap = new HashMap<>();
		for (AlterationData ad : query.getAlterationData()){
			alterationMap.put(ad.getGene(), ad);
		}
		return alterationMap;

	}
    
    /**
     * Annotates the {@code cxNetwork} with percent altered data.
     * 
     * This is done by getting the percentAltered value from AlterationData found in 
	 * query 
     * and adding that percent altered information via two network attributes {@link #IQUERY_LABEL} and
     * {@link #IQUERY_PERCENTALTERED_LIST}
     * 
     * The {@link #IQUERY_LABEL} is a node attribute that will contain a string 
     * GENE PERCENTALTERED% for genes in the query or the original node name.
     * 
     * The {@link #IQUERY_PERCENTALTERED_LIST} is a node attribute that will contain a list
     * of strings with format GENE::MUTFREQ where :: is the delimiter defined by {@link #FREQ_ARRAY_DELIMITER}
     * 
     * To make the new labels visible this method adjusts the style in the {@code cxNetwork}
     * to display the {@link #IQUERY_LABEL} attribute as the node label. This is done
     * via the {@link LabelNetworkAnnotator} created internally in the constructor.
     * 
     * @param cxNetwork Network to annotate in place
     * @param query The query sent in to the service, which also contains AlterationData
     * @param eqr The result of the query
     * @throws EnrichmentException If there was a problem
     */
    @Override
    public void annotateNetwork(NiceCXNetwork cxNetwork, EnrichmentQuery query, EnrichmentQueryResult eqr) throws EnrichmentException {        

        final Map<String,AlterationData> alterationMap = validateInputsAndGetAlterationData(cxNetwork,
                query, eqr);
        
        if (alterationMap == null){
            // something was invalid or unset just return
            return;
        }
        
        Map<String, Set<Long>> geneToNodeMap = _idr.getNetworkToGeneToNodeMap().get(eqr.getNetworkUUID());
        if (geneToNodeMap == null || geneToNodeMap.isEmpty()){
            _logger.info("No genes for network with ID: " + eqr.getNetworkUUID());
            return;
        }

        Map<Long, Set<String>> nodeIdToGenes = getNodeIdToGenesMap(geneToNodeMap);

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
                            CBioPortalAlterationDataNetworkAnnotator.IQUERY_LABEL,
                    nodes.get(nodeId).getNodeName(),
                                    ATTRIBUTE_DATA_TYPE.STRING);
                    cxNetwork.addNodeAttribute(nae);
                    nodeAttrCntr++;
                continue;
            }
            // okay we have 1 or more genes
            // dump into list with format GENE::mutationFrequency
			// and fill sb buffer with GENE (%), GENE (%) 
			// for each gene with percent altered values
            List<String> pcAlteredList = new ArrayList<>();
            sb.setLength(0);
			boolean addColon = false;
			int alteredGeneCount = 0;
			if (geneSet.size() > 1){
				sb.append(nodes.get(nodeId).getNodeName());
				addColon = true;
			}
            for (String gene : geneSet){
                AlterationData ad = alterationMap.get(gene);
				if (ad == null){
					continue;
				}
				String pcAltered = ad.getPercentAltered();
				
                if (pcAltered == null){
                    continue;
                }
				
				// only render first two
				// genes percent altered
				if (alteredGeneCount < 2){
					if (addColon == true){
						sb.append(": ");
						addColon = false;
					} else if (sb.length() > 0){
						sb.append(",");
					}
					sb.append(gene);
					sb.append(" (");
					sb.append(pcAltered);
					sb.append(")");
				} else if (alteredGeneCount == 2){
					sb.append("...");
				}
				alteredGeneCount++;
                pcAlteredList.add(gene 
                            + CBioPortalAlterationDataNetworkAnnotator.FREQ_ARRAY_DELIMITER
                            + pcAltered);
            }
            _logger.debug("Adding " + CBioPortalAlterationDataNetworkAnnotator.IQUERY_PERCENTALTERED_LIST
                    + " attribute to (" 
                    + Long.toString(nodeId) + ") with: " + pcAlteredList.toString());
            nae = new NodeAttributesElement(nodeId,
                            CBioPortalAlterationDataNetworkAnnotator.IQUERY_PERCENTALTERED_LIST,
                    pcAlteredList,
                                    ATTRIBUTE_DATA_TYPE.LIST_OF_STRING);
                    cxNetwork.addNodeAttribute(nae);
                    nodeAttrCntr++;
            _logger.debug("Adding " + CBioPortalAlterationDataNetworkAnnotator.IQUERY_LABEL
                    + " attribute to (" 
                    + Long.toString(nodeId) + ") with: " + sb.toString());
                
			// this copies over the node name for non complex nodes containing no genes
			// with alteration data
			if (sb.length() == 0){
				sb.append(nodes.get(nodeId).getNodeName());
			} 
		    nae = new NodeAttributesElement(nodeId,
                            CBioPortalAlterationDataNetworkAnnotator.IQUERY_LABEL,
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
}
