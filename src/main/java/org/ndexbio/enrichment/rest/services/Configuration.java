package org.ndexbio.enrichment.rest.services;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import org.ndexbio.enrichment.rest.engine.EnrichmentEngine;
import org.ndexbio.enrichment.rest.exceptions.EnrichmentException;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains configuration for Enrichment. The configuration
 * is extracted by looking for a file under the environment
 * variable NDEX_ENRICH_CONFIG and if that fails defaults are
 * used
 * @author churas
 */
public class Configuration {
    
    public static final String NDEX_ENRICH_CONFIG = "NDEX_ENRICH_CONFIG";
    
    public static final String DATABASE_DIR = "enrichment.database.dir";
    public static final String TASK_DIR = "enrichment.task.dir";
    
    public static final String NDEX_USER = "ndex.user";
    public static final String NDEX_PASS = "ndex.password";
    public static final String NDEX_SERVER = "ndex.server";
    public static final String NDEX_USERAGENT = "ndex.useragent";
    
    
    public static final String DATABASE_RESULTS_JSON_FILE = "databaseresults.json";
    
    private static Configuration INSTANCE;
    private static final Logger _logger = LoggerFactory.getLogger(Configuration.class);
    private static String _alternateConfigurationFile;
    private static NdexRestClientModelAccessLayer _client;
    private static EnrichmentEngine _enrichmentEngine;
    private static String _enrichDatabaseDir;
    private static String _enrichTaskDir;
    /**
     * Constructor that attempts to get configuration from properties file
     * specified via configPath
     */
    private Configuration(final String configPath)
    {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(configPath));
        }
        catch(FileNotFoundException fne){
            _logger.error("No configuration found at " + configPath, fne);
        }
        catch(IOException io){
            _logger.error("Unable to read configuration " + configPath, io);
        }
        
        _enrichDatabaseDir = props.getProperty(Configuration.DATABASE_DIR, "/tmp");
        _enrichTaskDir = props.getProperty(Configuration.TASK_DIR);
        
        _client = getNDExClient(props);
        
    }
    
    public NdexRestClientModelAccessLayer getNDExClient(){
        return _client;
    }
    
    protected void setEnrichmentEngine(EnrichmentEngine ee){
        _enrichmentEngine = ee;
    }
    public EnrichmentEngine getEnrichmentEngine(){
        return _enrichmentEngine;
    }
    
    /**
     * Gets directory where enrichment database is stored on the file system
     * @return 
     */
    public String getEnrichmentDatabaseDirectory(){
        return _enrichDatabaseDir;
    }
    
    /**
     * Gets directory where enrichment task results should be stored
     * @return 
     */
    public String getEnrichmentTaskDirectory(){
        return _enrichTaskDir;
    }

    /**
     * 
     * @return 
     */
    public InternalDatabaseResults getNDExDatabases(){
        ObjectMapper mapper = new ObjectMapper();
        File dbres = new File(getEnrichmentDatabaseDirectory() + File.separator +
                              Configuration.DATABASE_RESULTS_JSON_FILE);
        try {
            return mapper.readValue(dbres, InternalDatabaseResults.class);
        }
        catch(IOException io){
            _logger.error("caught io exception trying to load " + dbres.getAbsolutePath(), io);
        }
        return null;
    }

    /**
     * Using configuration create 
     * @return ndex client
     */
    protected NdexRestClientModelAccessLayer getNDExClient(Properties props){
        
        try {
            String user = props.getProperty(Configuration.NDEX_USER,"");
            String pass = props.getProperty(Configuration.NDEX_PASS,"");
            String server = props.getProperty(Configuration.NDEX_SERVER,"");
            String useragent = props.getProperty(Configuration.NDEX_USERAGENT,"Enrichment");
            NdexRestClient nrc = new NdexRestClient(user, pass, server, useragent);
            _client = new NdexRestClientModelAccessLayer(nrc);
            return _client;
        }
        catch(JsonProcessingException jpe){
            _logger.error("Caught exception", jpe);
        }
        catch(IOException io){
            _logger.error("Caught exception", io);
        }
        catch(NdexException ne){
            _logger.error("caught exception", ne);
        }
        return null;
    }
    
    /**
     * Gets singleton instance of configuration
     * @return {@link org.ndexbio.enrichment.rest.services.Configuration} object with configuration loaded
     * @throws EnrichmentException If there was an error reading configuration
     */
    public static Configuration getInstance()
    {
    	if ( INSTANCE == null)  { 
            
            try {
                String configPath = System.getenv(Configuration.NDEX_ENRICH_CONFIG);
                if (_alternateConfigurationFile != null){
                    configPath = _alternateConfigurationFile;
                    _logger.info("Alternate configuration path specified: " + configPath);
                }
                INSTANCE = new Configuration(configPath);
            } catch (Exception ex) {
                _logger.error("Error loading configuration", ex);
            }
    	} 
        return INSTANCE;
    }
    
    /**
     * Reloads configuration
     * @return {@link org.ndexbio.enrichment.rest.services.Configuration} object
     */
    public static Configuration reloadConfiguration()  {
        INSTANCE = null;
        return getInstance();
    }
    
    /**
     * Lets caller set an alternate path to configuration. Added so the command
     * line application can set path to configuration and it makes testing easier
     * @param configFilePath - Path to configuration file
     */
    public static void  setAlternateConfigurationFile(final String configFilePath) {
    	_alternateConfigurationFile = configFilePath;
    }
}
