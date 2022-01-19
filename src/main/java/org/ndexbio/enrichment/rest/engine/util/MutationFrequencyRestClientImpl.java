package org.ndexbio.enrichment.rest.engine.util;

import javax.ws.rs.core.HttpHeaders;
import kong.unirest.HttpResponse;
import kong.unirest.HttpStatus;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import kong.unirest.jackson.JacksonObjectMapper;
import org.ndexbio.enrichment.rest.model.ErrorResponse;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.ndexsearch.rest.model.GeneList;
import org.ndexbio.ndexsearch.rest.model.MutationFrequencies;


/**
 * Instances of this class allow caller to call Mutation Frequency service
 * where one can POST a {@link org.ndexbio.ndexsearch.rest.model.GeneList} 
 * object as JSON and get back a {@link org.ndexbio.ndexsearch.rest.model.MutationFrequencies}
 * object
 * 
 * @author churas
 */
public class MutationFrequencyRestClientImpl implements MutationFrequencyRestClient {
    
    public static final String APPLICATION_JSON = "application/json";

    private String _userAgent = "MutationFrequencyRestClient/0.1.0";
    private UnirestInstance _unirest;
    
    /**
     * Constructor
     */
    public MutationFrequencyRestClientImpl(){
        this(null);
    }
    
    /**
     * Constructor that lets one append text to default the user agent 
     * MutationFrequencyRestClient/< version 0.1.0> for
     * calls made from this instance
     * @param userAgent If not {@code null} this is appended to default user agent
     */
    public MutationFrequencyRestClientImpl(final String userAgent){
        if (userAgent != null){
            _userAgent = _userAgent + " " + userAgent;
        }
        _unirest = Unirest.spawnInstance();
        _unirest.config().setObjectMapper(new JacksonObjectMapper());

    }
    
    /**
     * Gets the UnirestInstance. This is for testing purposes only.
     * @return 
     */
    protected UnirestInstance getUnirestInstance(){
        return _unirest;
    }
    
    /**
     * Sets connect and socket timeouts
     * @param connectionTimeout
     * @param socketTimeout 
    */
    @Override
    public void setTimeouts(int connectionTimeout,
                          int socketTimeout){
        _unirest.config().socketTimeout(socketTimeout).connectTimeout(connectionTimeout);
    }
    
    /**
     * Queries {@code restEndPoint} for mutation frequencies
     * 
     * @param restEndPoint Full URL to endpoint to post to 
     * @param geneList Genes to get mutation frequencies for
     * @return Mutation frequencies for genes passed in, where a value of 100.0 means
     *         100% and 0.0 means 0%
     *         
     * @throws EnrichmentException If there is a problem
     */
    @Override
    public MutationFrequencies getMutationFrequencies(final String restEndPoint,
            GeneList geneList) throws EnrichmentException {
        HttpResponse<MutationFrequencies> mutFreqs = _unirest.post(restEndPoint)
                .header(HttpHeaders.ACCEPT, APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                .header(HttpHeaders.USER_AGENT, _userAgent)
                .body(geneList)
                .asObject(MutationFrequencies.class);
        
        // if we got an error just return that
        ErrorResponse er = mutFreqs.mapError(ErrorResponse.class);
        if (er != null){
            throw new EnrichmentException(er.asJson());
        }
        
        // If we did not get a 200 status code, just return that
        // this can happen with a 404 cause no error would be generated
        if (mutFreqs.getStatus() != HttpStatus.OK){
            throw new EnrichmentException("Received status code: " 
                    + Integer.toString(mutFreqs.getStatus()) + " : " 
                    + mutFreqs.getStatusText());
        }        
        
        return mutFreqs.getBody();
    }
    
    /**
     * Shutsdown Unirest, must be called after done with this object otherwise
     * JVM may not shutdown
     * @throws EnrichmentException If there is a problem
     */
    @Override
    public void shutdown() throws EnrichmentException {
        _unirest.shutDown();
        Unirest.shutDown();
    }
}
