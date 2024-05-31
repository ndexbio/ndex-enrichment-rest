package org.ndexbio.enrichment.rest.client;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import kong.unirest.HttpResponse;
import kong.unirest.HttpStatus;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import kong.unirest.UnirestInstance;
import kong.unirest.jackson.JacksonObjectMapper;
import org.ndexbio.enrichment.rest.model.DatabaseResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryStatus;
import org.ndexbio.enrichment.rest.model.Task;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;

/**
 * Enrichment REST client
 * @author churas
 */
public class EnrichmentRestClientImpl implements EnrichmentRestClient {
    
    public static final String APPLICATION_JSON = "application/json";
    public static final String ACCEPT = "accept";
    public static final String CONTENT_TYPE = "Content-Type";
    private String _restEndPoint;
    private String _userAgent = "EnrichmentRestClient/0.8.0";
    private UnirestInstance _unirest;

    public EnrichmentRestClientImpl(final String restEndPoint, final String userAgent) {
        this(null, restEndPoint, userAgent);
    }
	
	protected EnrichmentRestClientImpl(UnirestInstance restInstance, final String restEndPoint,
			                           final String userAgent){
		if (userAgent != null){
            _userAgent = _userAgent + " " + userAgent;
        }
        if (restEndPoint == null){
            throw new IllegalArgumentException("restEndPoint cannot be null");
        } else if (restEndPoint.substring(restEndPoint.length() - 1).equals("/")) {
        	_restEndPoint = restEndPoint.substring(0, restEndPoint.length() - 1);
        } else {
        _restEndPoint = restEndPoint;
        }
		if (restInstance != null){
			_unirest = restInstance;
		} else {
	        _unirest = Unirest.spawnInstance();
		    _unirest.config().setObjectMapper(new JacksonObjectMapper());
		}
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
    public void setTimeouts(int connectionTimeout,
                           int socketTimeout){
            _unirest.config().socketTimeout(socketTimeout).connectTimeout(connectionTimeout);
    }

    
    private String getQueryPostEndPoint() throws EnrichmentException {
        return _restEndPoint;
    }
    /**
     * Submits query for processing
     * @param query query to process
     * @return UUID as a string that is an identifier for query
     */
    @Override
    public String query(EnrichmentQuery query) throws EnrichmentException {
        if (query == null){
            throw new EnrichmentException("query cannot be null");
        }
        try {
            HttpResponse<Task> taskRes = _unirest.post(getQueryPostEndPoint())
                    .header(ACCEPT, APPLICATION_JSON)
                    .header(CONTENT_TYPE, APPLICATION_JSON)
                    .body(query).asObject(Task.class);
            Task task = taskRes.getBody();
            return task.getId();
        } catch(UnirestException ex){
            throw new EnrichmentException("Caught an exception: " + ex.getMessage());
        }
    }
    
    private String getSummaryOfDatabasesEndPoint() throws EnrichmentException {
        return _restEndPoint + "/database";
    }
    
    /**
     * Gets a summary of databases in engine
     * @return DatabaseResults object
     * @throws EnrichmentException if there is an error
     */
    @Override
    public DatabaseResults getDatabaseResults() throws EnrichmentException{
        try {
            HttpResponse<DatabaseResults> dbRes = _unirest.get(getSummaryOfDatabasesEndPoint())
                    .header(ACCEPT, APPLICATION_JSON)
                    .asObject(DatabaseResults.class);
            return dbRes.getBody();
        } catch(UnirestException ex){
            throw new EnrichmentException("Caught an exception: " + ex.getMessage());
        }
    }
    
    private String getQueryResultsEndPoint(final String id) throws EnrichmentException {
        return _restEndPoint + "/" + id;
    }
    /**
     * Gets query results
     * @param id
     * @param start
     * @param size
     * @return
     * @throws EnrichmentException  if there is an error
     */
    @Override
    public EnrichmentQueryResults getQueryResults(final String id, int start, int size) throws EnrichmentException{
        if (id == null){
            throw new IllegalArgumentException("id cannot be null");
        }
        try {
            HttpResponse<EnrichmentQueryResults> dbRes = _unirest.get(getQueryResultsEndPoint(id))
                    .header(ACCEPT, APPLICATION_JSON)
                    .queryString("start", Integer.toString(start))
                    .queryString("size", Integer.toString(size))
                    .asObject(EnrichmentQueryResults.class);
            return dbRes.getBody();
        } catch(UnirestException ex){
            throw new EnrichmentException("Caught an exception: " + ex.getMessage());
        }
    }
    
    private String getQueryStatusEndPoint(final String id) throws EnrichmentException {
        return _restEndPoint + "/" + id + "/status";
    }

    /**
     * Gets query status
     * @param id
     * @return
     * @throws EnrichmentException if there is an error
     */
    @Override
    public EnrichmentQueryStatus getQueryStatus(final String id) throws EnrichmentException {
        if (id == null){
            throw new IllegalArgumentException("id cannot be null");
        }
        try {
            HttpResponse<EnrichmentQueryStatus> dbRes = _unirest.get(getQueryStatusEndPoint(id))
                    .header(ACCEPT, APPLICATION_JSON)
                    .asObject(EnrichmentQueryStatus.class);
            return dbRes.getBody();
        } catch(UnirestException ex){
            throw new EnrichmentException("Caught an exception: " + ex.getMessage());
        }
    }
    
    private String getDeleteEndPoint(final String id) throws EnrichmentException {
        return _restEndPoint + "/" + id;
    }
    /**
     * Deletes query
     * @param id
     * @throws EnrichmentException if there is an error
     */
    @Override
    public void delete(final String id) throws EnrichmentException {
        if (id == null){
            throw new IllegalArgumentException("id cannot be null");
        }
        try {
            HttpResponse<JsonNode> dbRes = _unirest.delete(getDeleteEndPoint(id)).asJson();
            return;
        } catch(UnirestException ex){
            throw new EnrichmentException("Caught an exception: " + ex.getMessage());
        }
    }
    private String getNetworkOverlayEndPoint(final String id) throws EnrichmentException {
        return _restEndPoint + "/" + id + "/overlaynetwork";
    }
    /**
     * Gets a network as CX
     * @param id
     * @param databaseUUID
     * @param networkUUID
     * @return
     * @throws EnrichmentException 
     */
    @Override
    public InputStream getNetworkOverlayAsCX(final String id, final String databaseUUID, final String networkUUID) throws EnrichmentException{
        if (id == null){
            throw new IllegalArgumentException("id cannot be null");
        }
        try {
			
            HttpResponse<byte[]> dbRes = _unirest.get(getNetworkOverlayEndPoint(id))
                    .header(ACCEPT, APPLICATION_JSON)
                    .queryString("databaseUUID", databaseUUID)
                    .queryString("networkUUID", networkUUID).asBytes();
            
			// Fix for https://ndexbio.atlassian.net/browse/UD-2230
			// where error response isn't caught and raised as an exception
			if (dbRes.getStatus() != HttpStatus.OK){
				if (dbRes.getStatus() == HttpStatus.GONE){
					return null;
				}
				throw new EnrichmentException("HTTP Error: "
				                              + Integer.toString(dbRes.getStatus()));
			}
			// TODO need to get this to directly return in the inputstream
            //      instead of getting the body as a byte array and then
            //      wrapping it in an InputStream
            return new ByteArrayInputStream(dbRes.getBody());
        } catch(UnirestException ex){
            throw new EnrichmentException("Caught an exception: " + ex.getMessage());
        }
    }

    @Override
    public void shutdown() throws EnrichmentException {
       
        _unirest.shutDown();
        Unirest.shutDown();
    }

    
    
}
