package org.ndexbio.enrichment.rest.engine.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.ndexbio.cxio.aspects.datamodels.CyVisualPropertiesElement;
import org.ndexbio.cxio.aspects.datamodels.Mapping;
import org.ndexbio.cxio.core.NdexCXNetworkWriter;
import org.ndexbio.cxio.core.interfaces.AspectElement;
import org.ndexbio.cxio.core.writers.NiceCXNetworkWriter;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.model.cx.NiceCXNetwork;
import org.ndexbio.model.exceptions.NdexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates Style to use attribute name set in constructor as
 * the default pass through mapping for network
 * @author churas
 */
public class LabelNetworkAnnotator implements NetworkAnnotator {

    public static final String CY_VISUAL_PROPERTIES = "cyVisualProperties";
    public static final String NODE_LABEL = "NODE_LABEL";
    public static final String NODES_DEFAULT = "nodes:default";
    public static final String MAPPING_TYPE = "PASSTHROUGH";
    
    static Logger _logger = LoggerFactory.getLogger(LabelNetworkAnnotator.class);

    private String _definition;
    private String _type;
    
    /**
     * Constructor
     * 
     * The {@code definition} needs to be formatted in {@code COL=<COL NAME>,T=string} where
     * {@code <COL NAME>} must be a node attribute of type {@code string} in the network
     * 
     * @param definition Sets {@link #NODE_LABEL} mapping, should look like COL=<COL NAME>,T=string
     * @param type if {@code null} then {@link #MAPPING_TYPE} is used
     */
    public LabelNetworkAnnotator(final String definition, final String type){
        _definition = definition;
        if (type == null){
            _type = LabelNetworkAnnotator.MAPPING_TYPE;
        } else {    
            _type = type;
        }
    }
    
    /**
     * Updates the {@link #NODE_LABEL} property under {@link #NODES_DEFAULT} 
     * {@link #CY_VISUAL_PROPERTIES} property in the {@code cxNetwork} pass in.
     * This update is done in place. The {@link #NODE_LABEL} mapping defines
     * what labels are displayed on the network. See {@link #LabelNetworkAnnotator(java.lang.String, java.lang.String) }
     * for more information
     * 
     * 
     * @param cxNetwork Network to update the {@link #NODE_LABEL} mapping
     * @param query Ignored
     * @param eqr Ignored
     * @throws EnrichmentException if {@code cxNetwork} is null
     */
    @Override
    public void annotateNetwork(NiceCXNetwork cxNetwork, EnrichmentQuery query, EnrichmentQueryResult eqr) throws EnrichmentException {
        if (cxNetwork == null){
            throw new EnrichmentException("network is null");
        }
        Map<String, Collection<AspectElement>> opaqueAspects = cxNetwork.getOpaqueAspectTable();
        // if there are no visual properties, bail for now
        if (!opaqueAspects.containsKey(LabelNetworkAnnotator.CY_VISUAL_PROPERTIES)){
            _logger.error("Network lacks any visual properties. No changes made");
            return;
        }
        
        Collection<AspectElement> aspectCol = opaqueAspects.get(LabelNetworkAnnotator.CY_VISUAL_PROPERTIES);
        for (AspectElement ae : aspectCol){
            if (ae instanceof CyVisualPropertiesElement == false){
                continue;
            }
            
            CyVisualPropertiesElement cvp = (CyVisualPropertiesElement)ae;
            if (cvp.getProperties_of().equals(LabelNetworkAnnotator.NODES_DEFAULT) == false){
                continue;
            }
            
            SortedMap<String, Mapping> mappings_hash = cvp.getMappings();
            // if no mapping found, add one
            if (mappings_hash == null){
                mappings_hash = new TreeMap<>();
                cvp.setMappings(mappings_hash);
            }
            Mapping nodeLabel = null;
            // get node label if found
            if (mappings_hash.containsKey(LabelNetworkAnnotator.NODE_LABEL) == true){
               nodeLabel = mappings_hash.get(LabelNetworkAnnotator.NODE_LABEL);
              
            } else {
                // no node_label found, create a new object and put into map
                nodeLabel = new Mapping();
                mappings_hash.put(LabelNetworkAnnotator.NODE_LABEL, nodeLabel);
            }
            
            // update type and definition
            nodeLabel.setType(LabelNetworkAnnotator.MAPPING_TYPE);
            nodeLabel.setDefinition(_definition);
            // there should only be one of these so if we made it here, we are
            // done
            return;
        }
        // @TODO to support this we'd need to create the nodes:default visual property and 
        //       update the meta data count
        _logger.error("Network lacks " + LabelNetworkAnnotator.NODES_DEFAULT + " CyVisualProperty. No changes made");
    }
    
}
