package org.ndexbio.enrichment.rest.engine.util;

import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.ndexsearch.rest.model.GeneList;
import org.ndexbio.ndexsearch.rest.model.MutationFrequencies;

/**
 *
 * @author churas
 */
public interface MutationFrequencyRestClient {

    MutationFrequencies getMutationFrequencies(final String restEndPoint, GeneList geneList) throws EnrichmentException;

    /**
     * Sets connect and socket timeouts
     * @param connectionTimeout
     * @param socketTimeout
     */
    void setTimeouts(int connectionTimeout, int socketTimeout);

    //@Override
    void shutdown() throws EnrichmentException;
    
}
