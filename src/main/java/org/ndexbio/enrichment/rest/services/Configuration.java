package org.ndexbio.enrichment.rest.services;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.ndexbio.enrichment.rest.engine.EnrichmentEngine;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
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
    private static EnrichmentEngine _enrichmentEngine;
    private static String _enrichDatabaseDir;
    private static String _enrichTaskDir;
    /**
     * Constructor that attempts to get configuration from properties file
     * specified via configPath
     */
    private Configuration(final String configPath) throws EnrichmentException
    {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(configPath));
        }
        catch(FileNotFoundException fne){
            _logger.error("No configuration found at " + configPath, fne);
            throw new EnrichmentException("FileNotFound Exception when attempting to load " 
                    + configPath + " : " +
                    fne.getMessage());
        }
        catch(IOException io){
            _logger.error("Unable to read configuration " + configPath, io);
            throw new EnrichmentException("IOException when trying to read configuration file " + configPath +
                     " : " + io);
        }
        
        _enrichDatabaseDir = props.getProperty(Configuration.DATABASE_DIR, "/tmp");
        _enrichTaskDir = props.getProperty(Configuration.TASK_DIR, "/tmp");
                
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

    public File getDatabaseResultsFile(){
        
        return new File(getEnrichmentDatabaseDirectory() + File.separator +
                              Configuration.DATABASE_RESULTS_JSON_FILE);
    }
    /**
     * 
     * @return 
     */
    public InternalDatabaseResults getNDExDatabases(){
        ObjectMapper mapper = new ObjectMapper();
        File dbres = getDatabaseResultsFile();
        try {
            return mapper.readValue(dbres, InternalDatabaseResults.class);
        }
        catch(IOException io){
            _logger.error("caught io exception trying to load " + dbres.getAbsolutePath(), io);
        }
        return null;
    }
    
    /**
     * Gets singleton instance of configuration
     * @return {@link org.ndexbio.enrichment.rest.services.Configuration} object with configuration loaded
     * @throws EnrichmentException if there was a problem reading the configuration
     */
    public static Configuration getInstance() throws EnrichmentException
    {
    	if (INSTANCE == null)  { 
            
            try {
                String configPath = null;
                if (_alternateConfigurationFile != null){
                    configPath = _alternateConfigurationFile;
                    _logger.info("Alternate configuration path specified: " + configPath);
                } else {
                    try {
                        configPath = System.getenv(Configuration.NDEX_ENRICH_CONFIG);
                    } catch(SecurityException se){
                        _logger.error("Caught security exception ", se);
                    }
                }
                if (configPath == null){
                    InitialContext ic = new InitialContext();
                    configPath = (String) ic.lookup("java:comp/env/" + Configuration.NDEX_ENRICH_CONFIG); 

                }
                INSTANCE = new Configuration(configPath);
            } catch (NamingException ex) {
                _logger.error("Error loading configuration", ex);
                throw new EnrichmentException("NamingException encountered. Error loading configuration: " 
                         + ex.getMessage());
            }
    	} 
        return INSTANCE;
    }
    
    /**
     * Reloads configuration
     * @return {@link org.ndexbio.enrichment.rest.services.Configuration} object
     * @throws EnrichmentException if there was a problem reading the configuration
     */
    public static Configuration reloadConfiguration() throws EnrichmentException  {
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
