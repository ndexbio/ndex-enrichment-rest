package org.ndexbio.enrichment.rest;


import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Handler;
import org.ndexbio.cxio.aspects.datamodels.NodeAttributesElement;
import org.ndexbio.cxio.aspects.datamodels.NodesElement;
import org.eclipse.jetty.util.RolloverFileOutputStream;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
import org.ndexbio.ndexsearch.rest.model.DatabaseResult;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.enrichment.rest.model.InternalGeneMap;
import org.ndexbio.enrichment.rest.model.InternalNdexConnectionParams;
import org.ndexbio.ndexsearch.rest.model.NetworkInfo;
import org.ndexbio.enrichment.rest.services.Configuration;
import org.ndexbio.enrichment.rest.services.EnrichmentHttpServletDispatcher;
import org.ndexbio.model.cx.NiceCXNetwork;
import org.ndexbio.model.object.NetworkSet;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;
import org.ndexbio.rest.client.NdexRestClientUtilities;

/**
 *
 * @author churas
 */
public class App {
    
    static Logger _logger = LoggerFactory.getLogger(App.class);

	public static final String APP_PROPERTIES = "app.properties";
    
    /**
     * Sets logging level valid values DEBUG INFO WARN ALL ERROR
     */
    public static final String RUNSERVER_LOGLEVEL = "runserver.log.level";

    /**
     * Sets log directory for embedded Jetty
     */
    public static final String RUNSERVER_LOGDIR = "runserver.log.dir";
    
    /**
     * Sets port for embedded Jetty
     */
    public static final String RUNSERVER_PORT = "runserver.port";
        
    /**
     * Sets context path for embedded Jetty
     */
    public static final String RUNSERVER_CONTEXTPATH = "runserver.contextpath";
    
    public static final String MODE = "mode";
    public static final String CONF = "conf";    
    public static final String CREATEDB_MODE = "createdb";
	public static final String DBRES = "dbresults";
    public static final String EXAMPLE_CONF_MODE = "exampleconf";
    public static final String EXAMPLE_DBRES_MODE = "exampledbresults";
    public static final String RUNSERVER_MODE = "runserver";
    
    public static final String SUPPORTED_MODES = CREATEDB_MODE +
                                                    ", " + EXAMPLE_CONF_MODE +
                                                    ", " + EXAMPLE_DBRES_MODE +
                                                    ", " + RUNSERVER_MODE;
    
    public static void main(String[] args){

        final List<String> helpArgs = Arrays.asList("h", "help", "?");
        try {
            OptionParser parser = new OptionParser() {

                {
                    accepts(MODE, "Mode to run. Supported modes: " + SUPPORTED_MODES).withRequiredArg().ofType(String.class).required();
                    accepts(CONF, "Configuration file")
                            .withRequiredArg().ofType(String.class);
					accepts(DBRES, "Loads alternate dbresults "
							+ "JSON file for database creation "
							+ "(Optionally used with --" + MODE
							+ " "
							+ CREATEDB_MODE
							+ ")").withRequiredArg().ofType(File.class);
                    acceptsAll(helpArgs, "Show Help").forHelp();
                }
            };
            
            OptionSet optionSet = null;
            try {
                optionSet = parser.parse(args);
            } catch (OptionException oe) {
                System.err.println("\nThere was an error parsing arguments: "
                        + oe.getMessage() + "\n\n");
                parser.printHelpOn(System.err);
                System.exit(1);
            }

            //help check
            for (String helpArgName : helpArgs) {
                if (optionSet.has(helpArgName)) {
                    System.out.println(App.getDescription());
                    parser.printHelpOn(System.out);
                    System.exit(2);
                }
            }
            
            String mode = optionSet.valueOf(MODE).toString();

            if (mode.equals(EXAMPLE_CONF_MODE)){
                System.out.println(generateExampleConfiguration());
                System.out.flush();
                return;
            }
            if (mode.equals(EXAMPLE_DBRES_MODE)){
                System.out.println(generateExampleDatabaseResults());
                System.out.flush();
                return;
            }
            
            if (mode.equals(CREATEDB_MODE)){
                if (optionSet.has(CONF) == false){
                    throw new EnrichmentException("--" + CONF + " required for --" + MODE + " mode");
                }
                Configuration.setAlternateConfigurationFile(optionSet.valueOf(CONF).toString());
				InternalDatabaseResults idr = null;
				if (optionSet.has(DBRES) == true){
					ObjectMapper mapper = new ObjectMapper();
					File dbresFile = new File(optionSet.valueOf(DBRES).toString());
					System.out.println("Using " + dbresFile.getAbsolutePath() +
							" set via --" + DBRES + " flag as input database");
					try {
						idr = mapper.readValue(dbresFile, InternalDatabaseResults.class);
					}
					catch(IOException io){
						throw new EnrichmentException("Error loading database " +
								dbresFile.getAbsolutePath() + " : " + io.getMessage());
					}
				} else {
					idr = Configuration.getInstance().getNDExDatabases();
				}
                downloadNetworks(NdexRestClientModelAccessLayerFactory.getInstance(), idr,
						Configuration.getInstance().getEnrichmentDatabaseDirectory(),
						Configuration.getInstance().getDatabaseResultsFile());
                
                return;
            }
            
            if (mode.equals(RUNSERVER_MODE)){
                Configuration.setAlternateConfigurationFile(optionSet.valueOf(CONF).toString());
                Properties props = getPropertiesFromConf(optionSet.valueOf(CONF).toString());
                ch.qos.logback.classic.Logger rootLog = 
        		(ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
                rootLog.setLevel(Level.toLevel(props.getProperty(App.RUNSERVER_LOGLEVEL, "INFO")));

                String logDir = props.getProperty(App.RUNSERVER_LOGDIR, ".");
                RolloverFileOutputStream os = new RolloverFileOutputStream(logDir + File.separator + "ndexenrich_yyyy_mm_dd.log", true);
		
                //Remove old tasks
                FileUtils.cleanDirectory(new File(Configuration.getInstance().getEnrichmentTaskDirectory()));
		
                final int port = Integer.valueOf(props.getProperty(App.RUNSERVER_PORT, "8081"));
                System.out.println("\nSpinning up server for status invoke: http://localhost:" + Integer.toString(port) + "/enrichment/v1/status\n\n");
                System.out.flush();
                
                //We are creating a print stream based on our RolloverFileOutputStream
                PrintStream logStream = new PrintStream(os);

                //We are redirecting system out and system error to our print stream.
                System.setOut(logStream);
                System.setErr(logStream);

                final Server server = new Server(port);

                final ServletContextHandler webappContext = new ServletContextHandler(server, props.getProperty(App.RUNSERVER_CONTEXTPATH, "/"));
                
                HashMap<String, String> initMap = new HashMap<>();
                initMap.put("resteasy.servlet.mapping.prefix",
                            Configuration.APPLICATION_PATH);
                initMap.put("javax.ws.rs.Application", "org.ndexbio.enrichment.rest.EnrichmentApplication");
                final ServletHolder restEasyServlet = new ServletHolder(
                     new EnrichmentHttpServletDispatcher());
                
                restEasyServlet.setInitOrder(1);
                restEasyServlet.setInitParameters(initMap);
                webappContext.addServlet(restEasyServlet,
                                         Configuration.APPLICATION_PATH + "/*");
                webappContext.addFilter(CorsFilter.class,
                                        Configuration.APPLICATION_PATH + "/*", null);
                ContextHandlerCollection contexts = new ContextHandlerCollection();
                contexts.setHandlers(new Handler[] { webappContext });
 
                server.setHandler(contexts);
                
                server.start();
                Log.getRootLogger().info("Embedded Jetty logging started.", new Object[]{});
	    
                System.out.println("Server started on port " + port);
                server.join();
                return;
            }
            
        }
        catch(Exception ex){
            ex.printStackTrace();
        }

    }
	
    public static Properties getAppNameAndVersionProperties(){
		Properties props = new Properties();
		try {
			props.load(App.class.getClassLoader().getResourceAsStream(APP_PROPERTIES));
		} catch(IOException io){
			Log.getRootLogger().warn("Unable to get information from " +
					App.APP_PROPERTIES + " needed for description", io);
		}
		return props;
	}
	
	/**
	 * Using {@link #APP_PROPERTIES} configuration file within this jar
	 * this function returns a brief description of this application
	 * @return 
	 */
	public static String getDescription(){
		Properties props = App.getAppNameAndVersionProperties();
		String appName = props.getProperty("project.name", "Unknown");
		String appVersion = props.getProperty("project.version", "Unknown");
		String desc = props.getProperty("description", "");
		return "\n" + appName + " v" + appVersion + "\n\n" + desc;
	}
	
	/**
	 * Loads properties from configuration file {@code path} passed in
	 * @param path configuration file to load
	 * @return Properties loaded with data from {@code path} file
	 * @throws IOException
	 * @throws FileNotFoundException 
	 */
    public static Properties getPropertiesFromConf(final String path) throws IOException, FileNotFoundException {
        Properties props = new Properties();
        props.load(new FileInputStream(path));
        return props;
    }
    
    /**
     * Generates an example databaseresults.json 
     * @return String of example databaseresults.json
     * @throws Exception 
     */
    public static String generateExampleDatabaseResults() throws Exception {
    	NetworkInfo nw = new NetworkInfo();
    	nw.setName("Network Name");
    	nw.setDescription("Network description");
    	nw.setUuid("640e2cef-795d-11e8-a4bf-0ac135e8bacf");
    	nw.setUrl("http://www.ndexbio.org/#/network/640e2cef-795d-11e8-a4bf-0ac135e8bacf");
    	nw.setImageUrl("http://www.home.ndexbio.org/img/pid-logo-ndex.jpg");
    	List<NetworkInfo> networkList = new ArrayList<>();
    	networkList.add(nw);
    	
        DatabaseResult dr = new DatabaseResult();
        dr.setDescription("This is a description of a signor database");
        dr.setName("signor");
        dr.setNetworks(networkList);
        dr.setImageURL("http://signor.uniroma2.it/img/signor_logo.png");
        String druuid = "89a90a24-2fa8-4a57-ae4b-7c30a180e8e6";
        dr.setUuid(druuid);
        
        DatabaseResult drtwo = new DatabaseResult();
        drtwo.setDescription("This is a description of a ncipid database");
        drtwo.setName("ncipid");
        drtwo.setNetworks(networkList);
        drtwo.setImageURL("http://www.home.ndexbio.org/img/pid-logo-ndex.jpg");
        //drtwo.setImageurl("http://ndexbio.org/images/new_landing_page_logo.06974471.png");
        String drtwouuid = "e508cf31-79af-463e-b8b6-ff34c87e1734";
        drtwo.setUuid(drtwouuid);
        
        InternalDatabaseResults idr = new InternalDatabaseResults();
        
        idr.setResults(Arrays.asList(dr, drtwo));
        HashMap<String, InternalNdexConnectionParams> ndexParam = new HashMap<>();
        InternalNdexConnectionParams cParam = new InternalNdexConnectionParams();
        cParam.setPassword("somepassword");
        cParam.setUser("bob");
        cParam.setServer("dev.ndexbio.org");
        cParam.setNetworkSetId("f884cd40-5426-49e6-a311-fc046802b5f6");
        ndexParam.put(druuid, cParam);
        
        cParam = new InternalNdexConnectionParams();
        cParam.setPassword("somepassword");
        cParam.setUser("bob");
        cParam.setServer("dev.ndexbio.org");
        cParam.setNetworkSetId("bf0616dd-5d7e-403a-92f3-6e12cc02eb37");
        ndexParam.put(drtwouuid, cParam);
        idr.setDatabaseConnectionMap(ndexParam);
        
        HashSet<String> excludeNetworks = new HashSet<>();
        excludeNetworks.add("309e834a-3005-41f2-8d28-46f2594aaaa8");
        excludeNetworks.add("4671adc9-670d-474c-84db-37774fc885ba");
        idr.setNetworksToExclude(excludeNetworks);
        ObjectMapper mappy = new ObjectMapper();
        
        return mappy.writerWithDefaultPrettyPrinter().writeValueAsString(idr);
    }
    /**
     * Generates example Configuration file writing to standard out
     * @throws Exception 
     */
    public static String generateExampleConfiguration() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("# Example configuration file for Enrichment service\n\n");
        
        sb.append("# Sets Enrichment database directory\n");
        sb.append(Configuration.DATABASE_DIR + " = /tmp/db\n\n");
        
        sb.append("# Sets Enrichment task directory where results from queries are stored\n");
        sb.append(Configuration.TASK_DIR + " = /tmp/tasks\n\n");
        
        sb.append("# Sets HOST URL prefix (value is prefixed to Location header when query is invoked. Can be commented out)\n");
        sb.append("# " + Configuration.HOST_URL + " = http://ndexbio.org\n");
        
        sb.append("# Sets directory where log files will be written for Jetty web server\n");
        sb.append(App.RUNSERVER_LOGDIR + " = /tmp/logs\n\n");
        
        sb.append("# Sets port Jetty web service will be run under\n");
        sb.append(App.RUNSERVER_PORT + " = 8081\n\n");
        
        sb.append("# sets Jetty Context Path for Enrichment\n");
        sb.append(App.RUNSERVER_CONTEXTPATH + " = /\n\n");
        
        sb.append("# Valid log levels DEBUG INFO WARN ERROR ALL\n");
        sb.append(App.RUNSERVER_LOGLEVEL + " = INFO\n");
	    
	sb.append("# Number of workers in thread pool\n");
	sb.append(Configuration.NUM_WORKERS + " = 1\n");

        return sb.toString();
    }

    public static void downloadNetworks(NdexRestClientModelAccessLayerFactory clientFactory,
			InternalDatabaseResults idr, final String enrichmentDatabaseDir,
			final File databaseResultsFile) throws Exception {
        ObjectMapper mappy = new ObjectMapper();
        Set<String> universeUniqueGeneSet = new HashSet<>();
        List<InternalGeneMap> geneMapList = new LinkedList<>();
        Map<String, Integer> databaseUniqueGeneCount = new HashMap<>();
        Set<String> networksToExclude = idr.getNetworksToExclude();
		StringBuffer failedNetworks = null;
        int totalNetworkCount = 0;
        if (idr.getResults() == null){
			throw new NullPointerException("Results in database passed in was null");
		}
		Set<String> uniqueGeneSet = new HashSet<>();
        for (DatabaseResult dr : idr.getResults()){
            _logger.debug("Downloading networks for: " + dr.getName());
            InternalGeneMap geneMap = new InternalGeneMap();
            geneMap.setDatabaseUUID(dr.getUuid());
            
            InternalNdexConnectionParams cParams = idr.getDatabaseConnectionMap().get(dr.getUuid());
            
            _logger.debug("networkset id for maps is: " + cParams.getNetworkSetId());
            File databasedir = new File(enrichmentDatabaseDir + File.separator + dr.getUuid());
            if (databasedir.isDirectory() == false){
                _logger.debug("Creating directory " + databasedir.getAbsolutePath());
                databasedir.mkdirs();
            }
            NdexRestClientModelAccessLayer client = clientFactory.getNdexClient(cParams);
            NetworkSet ns = client.getNetworkSetById(UUID.fromString(cParams.getNetworkSetId()), null);
            if (ns == null){
                throw new EnrichmentException("null returned when querying for networks "
                                              + "in networkset returned null: " +
                                              cParams.getNetworkSetId()+ " with uuid: " +
                                              dr.getUuid());
            }
            _logger.debug("Found " + ns.getNetworks().size() + " networks");
            if (ns.getNetworks().size() <= 0){
                throw new EnrichmentException("No networks found in networkset: " +
                                              cParams.getNetworkSetId()+ " with uuid: " +
                                              dr.getUuid());
            }
            int networkCount = 0;
            List<NetworkInfo> networkList = new ArrayList<>();
            
            uniqueGeneSet.clear();
            for (UUID netid :  ns.getNetworks()){
                if (networksToExclude != null && networksToExclude.contains(netid.toString())){
                    _logger.debug("Network: " + netid.toString() + " in exclude list. skipping.");
                    continue;
                }
               
                _logger.debug("Saving network: " + netid.toString());
				NiceCXNetwork network = null;
				try {
					network = saveNetwork(client, netid, databasedir);
				} catch(Exception e){
					_logger.error("Error saving network", e);
					network = null;
				}
				if (network == null){
					String errorMsg = "Unable to save network: "
							+ netid.toString()
							+ " in database: "
							+ dr.getName() + " (" + dr.getUuid() +")";
					_logger.error(errorMsg
							+ " Skipping...");
					if (failedNetworks == null){
						failedNetworks = new StringBuffer();
					}
					failedNetworks.append(errorMsg);
					failedNetworks.append("\n");
					continue;
				}
				String networkUrl = getNetworkUrl(cParams.getServer(), netid.toString());
				NetworkInfo simpleNetwork = getSimpleNetwork(network, netid.toString(), networkUrl, dr.getImageURL());
                networkList.add(simpleNetwork);
				int geneCount = updateGeneMap(network, netid.toString(), geneMap,
                                              uniqueGeneSet, idr);
				simpleNetwork.setGeneCount(geneCount);
                networkCount++;
            }
            client.getNdexRestClient().signOut();
            dr.setNetworks(networkList);
            totalNetworkCount += networkCount;
            universeUniqueGeneSet.addAll(uniqueGeneSet);
            geneMapList.add(geneMap);
            databaseUniqueGeneCount.put(dr.getUuid(), uniqueGeneSet.size());
            dr.setUrl(getNetworkSetUrl(cParams.getServer(), cParams.getNetworkSetId()));
			dr.setNumberOfNetworks(Integer.toString(networkCount));
        }
        idr.setUniverseUniqueGeneCount(universeUniqueGeneSet.size());
        idr.setDatabaseUniqueGeneCount(databaseUniqueGeneCount);
        idr.setGeneMapList(geneMapList);
        idr.setIdfMap(makeIdfMap(geneMapList, totalNetworkCount));
        idr.setTotalNetworkCount(totalNetworkCount);
        
        _logger.debug("Attempting to write: " + databaseResultsFile.getAbsolutePath());
        mappy.writerWithDefaultPrettyPrinter().writeValue(databaseResultsFile, idr);
		if (failedNetworks != null){
			throw new EnrichmentException(failedNetworks.toString());
		}
    }
    
    private static Map<String, Double> makeIdfMap(List<InternalGeneMap> geneMapList, int totalNetworkCount) {
    	Map<String, Integer> idfPrecursor = new HashMap<>();
    	for (InternalGeneMap internalGeneMap : geneMapList) {
    		Map<String, Set<String>> geneMap = internalGeneMap.getGeneMap();
    		for (String gene : geneMap.keySet()) {
    			idfPrecursor.merge(gene, geneMap.get(gene).size(), (oldNum, newNum) -> oldNum + newNum);
    		}
    	}
    	Map<String, Double> idfMap = new HashMap<>();
    	for (String gene : idfPrecursor.keySet()) {
    		idfMap.put(gene, Math.log(totalNetworkCount / (1 + idfPrecursor.get(gene))));
    	}
    	return idfMap;
    }
	
	/**
	 * Compares {@code value} with known node types that denote if a node is
	 * of type "protein" in that the node name is a gene
	 * @param value
	 * @return true if {@code value} denotes a node of type protein otherwise false 
	 */
    public static boolean isTypeProtein(final String value){
		if (value == null){
			return false;
		}
		String lowerCaseValue = value.toLowerCase();
		if (lowerCaseValue.equals("gene") ||
				lowerCaseValue.equals("protein") ||
                lowerCaseValue.equals("geneproduct")){
			return true;
		}
		return false;
	}
	
	/**
	 * Compares {@code value} with known node types that denote if a node is
	 * of type "complex" in that it contains a list of genes within "member"
	 * node attribute.
	 * @param value
	 * @return true if {@code value} denotes a node of type complex otherwise false 
	 */
	public static boolean isTypeComplex(final String value){
		if (value == null){
			return false;
		}
		String lowerCaseValue = value.toLowerCase();
		if (lowerCaseValue.equals("complex") ||
				lowerCaseValue.equals("proteinfamily") ||
                lowerCaseValue.equals("compartment")){
			return true;
		}
		return false;
		
		
	}
    /**
     * Adds network to gene map which has the following structure:
     * 
     * gene names => [ list of network UUIDs for networks that have this gene]
     * 
     * 
     * @param network network to examine
     * @param externalId id of network passed in
     * @param geneMap gene names => [ list of network UUIDs]
     * @param uniqueGeneSet unique set of genes
     * @throws Exception 
     */
    public static int updateGeneMap(final NiceCXNetwork network,
            final String externalId, InternalGeneMap geneMap,
            final Set<String> uniqueGeneSet,
            InternalDatabaseResults idr) throws Exception {
        
        Map<Long, Collection<NodeAttributesElement>> attribMap = network.getNodeAttributes();
        Map<String, Set<String>> mappy = geneMap.getGeneMap();
        if (mappy == null){
            _logger.debug("Adding mappy");
            mappy = new HashMap<>();
            geneMap.setGeneMap(mappy);
        }
        
        Map<String, Set<Long>> geneToNodeMap = new HashMap<>();
		int geneCount = 0;
        for (NodesElement ne : network.getNodes().values()){
            Collection<NodeAttributesElement> nodeAttribs = attribMap.get(ne.getId());
            
            // If there are node attributes and one is named "type" then
            // only include the node name if type is gene or protein
            if (nodeAttribs == null){
                continue;
            }
            boolean validgene = false;
            boolean validcomplex = false;
            for (NodeAttributesElement nae : nodeAttribs){
                if (nae.getName().toLowerCase().equals("type")){
					if (App.isTypeProtein(nae.getValue())){
                        validgene = true;
                        break;
                    }
                    if (App.isTypeComplex(nae.getValue())){
                        validcomplex = true;
                        break;
                    }
                }
            }
            if (validgene == true){
                String name = getValidGene(ne.getNodeName());
                if (name == null){
                    continue;
                }
                if (mappy.containsKey(name) == false){
                    mappy.put(name, new HashSet<String>());
                }
                if (mappy.get(name).contains(externalId) == false){
                    mappy.get(name).add(externalId);
                }
                if (geneToNodeMap.containsKey(name) == false){
                    geneToNodeMap.put(name, new HashSet<Long>());
                }
                geneToNodeMap.get(name).add(ne.getId());
                
                uniqueGeneSet.add(name);
				geneCount++;
                continue;
            }
            if (validcomplex == true){
                for (NodeAttributesElement nae : nodeAttribs){
                    if (nae.getName().toLowerCase().equals("member")){
                        for (String entry : nae.getValues()){
                            String name = getValidGene(entry);
                            if (name == null){
                                continue;
                            }
                            if (mappy.containsKey(name) == false){
                                mappy.put(name, new HashSet<String>());
                            }
                            if (mappy.get(name).contains(externalId) == false){
                                mappy.get(name).add(externalId);
                            }
                            if (geneToNodeMap.containsKey(name) == false){
                                geneToNodeMap.put(name, new HashSet<Long>());
                            }
                            geneToNodeMap.get(name).add(ne.getId());
                            uniqueGeneSet.add(name);
							geneCount++;
                        }
                        break;
                    }
                }
            }
        }
        if (geneToNodeMap.size() > 0){
            Map<String, Map<String, Set<Long>>> geneToNodeBigMap = idr.getNetworkToGeneToNodeMap();
            if (geneToNodeBigMap == null){
                geneToNodeBigMap = new HashMap<>();
            } else 
            geneToNodeBigMap.put(externalId, geneToNodeMap);
            idr.setNetworkToGeneToNodeMap(geneToNodeBigMap);
        }
		return geneCount;

    }
    
    public static String getValidGene(final String potentialGene){
        if (potentialGene == null){
            _logger.warn("Gene passed in is null");
            return null;
        }
        String strippedGene = potentialGene;
        // strip off hgnc.symbol: prefix if found
        if (potentialGene.startsWith("hgnc.symbol:") && potentialGene.length()>12){
            strippedGene = potentialGene.substring(potentialGene.indexOf(":") + 1);
        }
        
        //if (strippedGene.length()>30 || !strippedGene.matches("^[^\\(\\)\\s,']+$")){
        if (strippedGene.length()>30 || !strippedGene.matches("(^[A-Z][A-Z0-9-]*$)|(^C[0-9]+orf[0-9]+$)")) {
            _logger.debug("Gene: " + strippedGene + " does not appear to be valid. Skipping...");
            return null;
        }
        
        return strippedGene;
    }
    /**
     * Saves network with 'networkuuid' to directory specified by 'savedir'
     * with the name 'networkuuid'.cx
     * @param client NDEx java client used to get network from NDEx
     * @param networkuuid id of network to download
     * @param savedir directory to write network.
     * @return NiceCXNetwork network downloaded
     * @throws Exception 
     */
    public static NiceCXNetwork saveNetwork(NdexRestClientModelAccessLayer client, final UUID networkuuid, final File savedir) throws Exception{

		File dest = new File(savedir.getAbsolutePath() +
				File.separator + networkuuid.toString() + ".cx");
        
		try (FileOutputStream fos = new FileOutputStream(dest)) {
			try (InputStream instream = client.getNetworkAsCXStream(networkuuid)) {
				byte[] buffer = new byte[8 * 1024];
				int bytesRead;
				while ((bytesRead = instream.read(buffer)) != -1) {
					fos.write(buffer, 0, bytesRead);
				}
			}
			FileInputStream fis = new FileInputStream(dest);
			return NdexRestClientUtilities.getCXNetworkFromStream(fis);
		}
    }
    
    public static NetworkInfo getSimpleNetwork(NiceCXNetwork network, String networkUuid, String networkUrl, String imageUrl) {
    	NetworkInfo nw = new NetworkInfo();
    	nw.setName(network.getNetworkName());
    	nw.setDescription(network.getNetworkDescription());
    	nw.setUuid(networkUuid);
    	nw.setUrl(networkUrl);
    	nw.setImageUrl(imageUrl);
    	return nw;
    }
    
    public static String getNetworkUrl(String server, String networkUuid) {
    	return trimServerString(server) + "/#/network/" + networkUuid;
    }
    
    public static String getNetworkSetUrl(String server, String networkSetUuid) {
    	return trimServerString(server) + "/#/networkset/" + networkSetUuid;
    }

    public static String trimServerString(String server) {
        if (server.startsWith("http://")) {
            server = server.substring("http://".length());
        } else if (server.startsWith("https://")) {
            server = server.substring("https://".length());
        }
  
        Pattern pattern = Pattern.compile("/v");
        Matcher matcher = pattern.matcher(server);
        int lastIndex = server.length();
        
        while (matcher.find()) {
          lastIndex = matcher.start();
        }
        
        server = server.substring(0, lastIndex);
        return server;
    }
}
