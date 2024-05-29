package org.ndexbio.enrichment.rest.services;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
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
    
    public static final String APPLICATION_PATH = "/enrichment";
    public static final String V_ONE_PATH = "/v1";
    public static final String NDEX_ENRICH_CONFIG = "NDEX_ENRICH_CONFIG";
    
    public static final String DATABASE_DIR = "enrichment.database.dir";
    public static final String TASK_DIR = "enrichment.task.dir";
    public static final String HOST_URL = "enrichment.host.url";
    
    public static final String NUM_WORKERS = "enrichment.number.workers";
    
    public static final String NDEX_USER = "ndex.user";
    public static final String NDEX_PASS = "ndex.password";
    public static final String NDEX_SERVER = "ndex.server";
    public static final String NDEX_USERAGENT = "ndex.useragent";
	public static final String NUM_RESULTS = "number.returned.results";
	public static final String SORT_ALGO = "sort.algorithm";
	
	public static final String SELECT_HIT_GENES = "select.hit.genes";
	
	public static final String SIMULATE_PERCENT_ALTERED = "simulate.percent.altered";
    
    
    public static final String DATABASE_RESULTS_JSON_FILE = "databaseresults.json";
	
	public static final String CACHE_INITIAL_SIZE = "cache.initial.size";
	public static final String CACHE_MAXIMUM_SIZE = "cache.maximum.size";
	public static final String CACHE_EXPIRE_AFTER_ACCESS_DURATION = "cache.expire.after.access.duration";
	public static final String CACHE_EXPIRE_AFTER_ACCESS_UNIT = "cache.expire.after.access.unit";

    private static Configuration INSTANCE;
    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);
    private static String _alternateConfigurationFile;
    private static EnrichmentEngine _enrichmentEngine;
    private static String _enrichDatabaseDir;
    private static String _enrichTaskDir;
    private static String _enrichHostURL;
	private static String _sortAlgorithm;
    private static int _numWorkers;
	private static int _numResults;
	private static boolean _selectHitGenes;
	private static boolean _simulatePercentAltered;
	private static int _cacheInitialSize;
	private static long _cacheMaximumSize;
	private static long _cacheExpireAfterAccessDuration;
	private static TimeUnit _cacheExpireAfterAccessUnit;
	
	
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
            LOGGER.error("No configuration found at " + configPath, fne);
            throw new EnrichmentException("FileNotFound Exception when attempting to load " 
                    + configPath + " : " +
                    fne.getMessage());
        }
        catch(IOException io){
            LOGGER.error("Unable to read configuration " + configPath, io);
            throw new EnrichmentException("IOException when trying to read configuration file " + configPath +
                     " : " + io);
        }
        
        _enrichDatabaseDir = props.getProperty(Configuration.DATABASE_DIR, "/tmp");
        _enrichTaskDir = props.getProperty(Configuration.TASK_DIR, "/tmp");
        _enrichHostURL = props.getProperty(Configuration.HOST_URL, "");
        _numWorkers = Integer.parseInt(props.getProperty(Configuration.NUM_WORKERS, "1"));
        if (_enrichHostURL.trim().isEmpty()){
            _enrichHostURL = "";
        } else if (!_enrichHostURL.endsWith("/")){
            _enrichHostURL =_enrichHostURL + "/";
        }
		_numResults = Integer.parseInt(props.getProperty(Configuration.NUM_RESULTS, "25"));
		_sortAlgorithm = props.getProperty(Configuration.SORT_ALGO, "similarity");
		_selectHitGenes = Boolean.parseBoolean(props.getProperty(Configuration.SELECT_HIT_GENES, "false"));
		_simulatePercentAltered = Boolean.parseBoolean(props.getProperty(Configuration.SIMULATE_PERCENT_ALTERED, "false"));
		_cacheInitialSize = Integer.parseInt(props.getProperty(Configuration.CACHE_INITIAL_SIZE, "600"));
		_cacheMaximumSize = Long.parseLong(props.getProperty(Configuration.CACHE_MAXIMUM_SIZE, "600"));
		_cacheExpireAfterAccessDuration = Long.parseLong(props.getProperty(Configuration.CACHE_EXPIRE_AFTER_ACCESS_DURATION, "5"));
		_cacheExpireAfterAccessUnit = TimeUnit.valueOf(props.getProperty(Configuration.CACHE_EXPIRE_AFTER_ACCESS_UNIT, "DAYS"));
    }
        
    protected void setEnrichmentEngine(EnrichmentEngine ee){
        _enrichmentEngine = ee;
    }
    public EnrichmentEngine getEnrichmentEngine(){
        return _enrichmentEngine;
    }

	/**
	 * Denotes whether to simulate percent altered values for query genes
	 * if <b>no</b> {@code alterationData} is included
	 * 
	 * @return {@code true} if percent altered should be simulated
	 */
	public boolean getSimulatePercentAltered(){
		return _simulatePercentAltered;
	}
	
	/**
	 * Denotes whether this service should select aka highlight the
	 * hit genes from the query
	 * 
	 * @return {@code true} if hit genes should be selected 
	 */
	public boolean getSelectHitGenes(){
		return _selectHitGenes;
	}
	
    /**
     * Gets alternate URL prefix for the host running this service.
     * @return String containing alternate URL ending with / or empty
     *         string if not is set
     */
    public String getHostURL(){
        return _enrichHostURL;
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
    public int getNumberWorkers() {
    	return _numWorkers;
    }
	
	/**
	 * Gets default number of results to return for a query
	 * @return 
	 */
	public int getNumberOfResultsToReturn(){
		return _numResults;
	}
	
	/**
	 * Gets sort algorithm used to sort results
	 * @return 
	 */
	public String getSortAlgorithm(){
		return _sortAlgorithm;
	}

    public File getDatabaseResultsFile(){
        
        return new File(getEnrichmentDatabaseDirectory() + File.separator +
                              Configuration.DATABASE_RESULTS_JSON_FILE);
    }

	public int getCacheInitialSize() {
		return _cacheInitialSize;
	}

	public long getCacheMaximumSize() {
		return _cacheMaximumSize;
	}

	public long getCacheExpireAfterAccessDuration() {
		return _cacheExpireAfterAccessDuration;
	}

	public TimeUnit getCacheExpireAfterAccessUnit() {
		return _cacheExpireAfterAccessUnit;
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
            LOGGER.error("caught io exception trying to load " + dbres.getAbsolutePath(), io);
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
                    LOGGER.info("Alternate configuration path specified: " + configPath);
                } else {
                    try {
                        configPath = System.getenv(Configuration.NDEX_ENRICH_CONFIG);
                    } catch(SecurityException se){
                        LOGGER.error("Caught security exception ", se);
                    }
                }
                if (configPath == null){
                    InitialContext ic = new InitialContext();
                    configPath = (String) ic.lookup("java:comp/env/" + Configuration.NDEX_ENRICH_CONFIG); 

                }
                INSTANCE = new Configuration(configPath);
            } catch (NamingException ex) {
                LOGGER.error("Error loading configuration", ex);
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
     * This also sets the internal instance object to {@code null} so subsequent
     * calls to {@link #getInstance() } will load a new instance with this configuration
     * @param configFilePath - Path to configuration file
     */
    public static void  setAlternateConfigurationFile(final String configFilePath) {
    	_alternateConfigurationFile = configFilePath;
        INSTANCE = null;
    }
}
