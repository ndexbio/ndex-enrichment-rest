package org.ndexbio.enrichment.rest.engine.util;

import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResult;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.model.cx.NiceCXNetwork;

/**
 * Interface for classes that annotate NiceCXNetwork objects
 * @author churas
 */
public interface NetworkAnnotator {
    
    /**
     * Implementing classes annotate cxNetwork passed in
     * @param cxNetwork
     * @param query
     * @param eqr
     * @throws EnrichmentException If there is an error with annotation
     */
    public void annotateNetwork(NiceCXNetwork cxNetwork, EnrichmentQuery query, EnrichmentQueryResult eqr) throws EnrichmentException;
    
}
