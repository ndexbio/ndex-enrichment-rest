package org.ndexbio.enrichment.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.util.Properties;
import org.ndexbio.enrichment.rest.model.InternalNdexConnectionParams;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

/**
 * Singleton Factory to create {@link org.ndexbio.rest.client.NdexRestClientModelAccessLayer} objects
 * 
 * @author churas
 */
public class NdexRestClientModelAccessLayerFactory {
	
	private static NdexRestClientModelAccessLayerFactory INSTANCE;
	private static Properties APP_PROPS;

	/**
	 * Private constructor
	 */
	private NdexRestClientModelAccessLayerFactory(){
		APP_PROPS = App.getAppNameAndVersionProperties();
	}
	
	/**
	 * Gets instance of factory
	 * @return 
	 */
	public static NdexRestClientModelAccessLayerFactory getInstance() {
		if (INSTANCE == null){
			INSTANCE = new NdexRestClientModelAccessLayerFactory();
		}
		return INSTANCE;
	}
	
	/**
	 * Gets NDEx client connected to server specified by {@code params} passed
	 * in. 
	 * @param params contains user, password, and server information
	 * @return
	 * @throws JsonProcessingException
	 * @throws IOException
	 * @throws NdexException 
	 */
	public NdexRestClientModelAccessLayer getNdexClient(InternalNdexConnectionParams params) throws JsonProcessingException, IOException, NdexException {
		NdexRestClient nrc = new NdexRestClient(params.getUser(), params.getPassword(), 
                params.getServer(), APP_PROPS.getProperty("project.artifactid", "Unknown") + "/" +
						APP_PROPS.getProperty("project.version", "Unknown"));
        
        return new NdexRestClientModelAccessLayer(nrc);
	}
}
