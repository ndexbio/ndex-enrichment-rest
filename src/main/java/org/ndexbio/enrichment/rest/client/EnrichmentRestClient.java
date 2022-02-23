/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.client;

import java.io.InputStream;
import org.ndexbio.enrichment.rest.model.DatabaseResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryStatus;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;

/**
 *
 * @author churas
 */
public interface EnrichmentRestClient {

    /**
     * Deletes query
     * @param id
     * @throws EnrichmentException if there is an error
     */
    void delete(final String id) throws EnrichmentException;

    /**
     * Gets a summary of databases in engine
     * @return DatabaseResults object
     * @throws EnrichmentException if there is an error
     */
    DatabaseResults getDatabaseResults() throws EnrichmentException;

    /**
     * Gets a network as CX
     * @param id
     * @param databaseUUID
     * @param networkUUID
     * @return
     * @throws EnrichmentException
     */
    InputStream getNetworkOverlayAsCX(final String id, final String databaseUUID, final String networkUUID) throws EnrichmentException;

    /**
     * Gets query results
     * @param id
     * @param start
     * @param size
     * @return
     * @throws EnrichmentException  if there is an error
     */
    EnrichmentQueryResults getQueryResults(final String id, int start, int size) throws EnrichmentException;

    /**
     * Gets query status
     * @param id
     * @return
     * @throws EnrichmentException if there is an error
     */
    EnrichmentQueryStatus getQueryStatus(final String id) throws EnrichmentException;

    /**
     * Submits query for processing
     * @param query query to process
     * @return UUID as a string that is an identifier for query
     */
    String query(EnrichmentQuery query) throws EnrichmentException;
    
    /**
     * Releases any resources and shutsdown any threads etc..
     * @throws EnrichmentException 
     */
    void shutdown() throws EnrichmentException;
    
}
