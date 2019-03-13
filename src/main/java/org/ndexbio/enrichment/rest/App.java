/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
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
import org.ndexbio.enrichment.rest.model.DatabaseResult;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.enrichment.rest.model.InternalGeneMap;
import org.ndexbio.enrichment.rest.model.InternalNdexConnectionParams;
import org.ndexbio.enrichment.rest.services.Configuration;
import org.ndexbio.enrichment.rest.services.EnrichmentHttpServletDispatcher;
import org.ndexbio.model.cx.NiceCXNetwork;
import org.ndexbio.model.object.NetworkSearchResult;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;
import org.ndexbio.rest.client.NdexRestClientUtilities;

/**
 *
 * @author churas
 */
public class App {
    
    static Logger _logger = LoggerFactory.getLogger(App.class);

    public static final String DESCRIPTION = "\nNDEx Enrichment REST service\n\n"
            + "For usage information visit:  https://github.com/ndexbio/ndex-enrichment-rest\n\n";
    
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
                    System.out.println(DESCRIPTION);
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
                downloadNetworks();
                
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
		
		
                final int port = Integer.valueOf(props.getProperty(App.RUNSERVER_PORT, "8081"));
                System.out.println("\nSpinning up server for status invoke: http://localhost:" + Integer.toString(port) + "/status\n\n");
                System.out.flush();
                
                //We are creating a print stream based on our RolloverFileOutputStream
		PrintStream logStream = new PrintStream(os);

                //We are redirecting system out and system error to our print stream.
		System.setOut(logStream);
		System.setErr(logStream);

                final Server server = new Server(port);

                final ServletContextHandler webappContext = new ServletContextHandler(server, props.getProperty(App.RUNSERVER_CONTEXTPATH, "/"));
                
                HashMap<String, String> initMap = new HashMap<>();
                initMap.put("resteasy.servlet.mapping.prefix", "/");
                initMap.put("javax.ws.rs.Application", "org.ndexbio.enrichment.rest.EnrichmentApplication");
                final ServletHolder restEasyServlet = new ServletHolder(
                     new EnrichmentHttpServletDispatcher());
                
                restEasyServlet.setInitOrder(1);
                restEasyServlet.setInitParameters(initMap);
                webappContext.addServlet(restEasyServlet, "/*");
                webappContext.addFilter(CorsFilter.class, "/*", null);
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
        DatabaseResult dr = new DatabaseResult();
        dr.setDescription("This is a description of a signor database");
        dr.setName("signor");
        dr.setNumberOfNetworks("50");
        dr.setImageURL("http://signor.uniroma2.it/img/signor_logo.png");
        String druuid = "89a90a24-2fa8-4a57-ae4b-7c30a180e8e6";
        dr.setUuid(druuid);
        
        DatabaseResult drtwo = new DatabaseResult();
        drtwo.setDescription("This is a description of a ncipid database");
        drtwo.setName("ncipid");
        drtwo.setNumberOfNetworks("200");
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
        cParam.setNetworkOwner("signoruser");
        ndexParam.put(druuid, cParam);
        
        cParam = new InternalNdexConnectionParams();
        cParam.setPassword("somepassword");
        cParam.setUser("bob");
        cParam.setServer("dev.ndexbio.org");
        cParam.setNetworkOwner("ncipiduser");
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
        
        sb.append("# Sets directory where log files will be written for Jetty web server\n");
        sb.append(App.RUNSERVER_LOGDIR + " = /tmp/logs\n\n");
        
        sb.append("# Sets port Jetty web service will be run under\n");
        sb.append(App.RUNSERVER_PORT + " = 8081\n\n");
        
        sb.append("# sets Jetty Context Path for Enrichment\n");
        sb.append(App.RUNSERVER_CONTEXTPATH + " = /\n\n");
        
        sb.append("# Valid log levels DEBUG INFO WARN ERROR ALL\n");
        sb.append(App.RUNSERVER_LOGLEVEL + " = INFO\n");

        return sb.toString();
    }
    
    public static NdexRestClientModelAccessLayer getNdexClient(InternalNdexConnectionParams params) throws Exception {
        NdexRestClient nrc = new NdexRestClient(params.getUser(), params.getPassword(), 
                params.getServer(), "Enrichment/0.1.0");
        
        return new NdexRestClientModelAccessLayer(nrc);
    }
    public static void downloadNetworks() throws Exception {
        Configuration config = Configuration.getInstance();
        InternalDatabaseResults idr = config.getNDExDatabases();
        ObjectMapper mappy = new ObjectMapper();
        Set<String> universeUniqueGeneSet = new HashSet<>();
        List<InternalGeneMap> geneMapList = new LinkedList<InternalGeneMap>();
        Map<String, Integer> databaseUniqueGeneCount = new HashMap<>();
        Set<String> networksToExclude = idr.getNetworksToExclude();
        for (DatabaseResult dr : idr.getResults()){
            _logger.debug("Downloading networks for: " + dr.getName());
            InternalGeneMap geneMap = new InternalGeneMap();
            geneMap.setDatabaseUUID(dr.getUuid());
            
            InternalNdexConnectionParams cParams = idr.getDatabaseConnectionMap().get(dr.getUuid());
            
            _logger.debug("Owner for maps is: " + cParams.getNetworkOwner());
            File databasedir = new File(config.getEnrichmentDatabaseDirectory() + File.separator + dr.getUuid());
            if (databasedir.isDirectory() == false){
                _logger.debug("Creating directory " + databasedir.getAbsolutePath());
                databasedir.mkdirs();
            }
            NdexRestClientModelAccessLayer client = getNdexClient(cParams);
            NetworkSearchResult nrs = client.findNetworks("", cParams.getNetworkOwner(), 0, 500);
            _logger.debug("Found " + nrs.getNumFound() + " networks");
            int networkCount = 0;
            
            Set<String> uniqueGeneSet = new HashSet<>();
            for (NetworkSummary ns :  nrs.getNetworks()){
                if (networksToExclude.contains(ns.getExternalId().toString())){
                    _logger.debug("Network: " + ns.getName() + " in exclude list. skipping.");
                    continue;
                }
                _logger.debug(ns.getName() + " Nodes => " + Integer.toString(ns.getNodeCount()) + " Edges => " + Integer.toString(ns.getEdgeCount()));
                NiceCXNetwork network = saveNetwork(client, ns.getExternalId(), databasedir);
                updateGeneMap(network, ns.getExternalId().toString(), geneMap,
                        uniqueGeneSet);
                networkCount++;
            }
            client.getNdexRestClient().signOut();
            dr.setNumberOfNetworks(Integer.toString(networkCount));
            universeUniqueGeneSet.addAll(uniqueGeneSet);
            geneMapList.add(geneMap);
            databaseUniqueGeneCount.put(dr.getUuid(), uniqueGeneSet.size());
            uniqueGeneSet.clear();
        }
        idr.setUniverseUniqueGeneCount(universeUniqueGeneSet.size());
        idr.setDatabaseUniqueGeneCount(databaseUniqueGeneCount);
        idr.setGeneMapList(geneMapList);
        _logger.debug("Attempting to write: " + config.getDatabaseResultsFile().getAbsolutePath());
        mappy.writerWithDefaultPrettyPrinter().writeValue(config.getDatabaseResultsFile(), idr);
        return;
    }
    
    public static void updateGeneMap(final NiceCXNetwork network, final String externalId, InternalGeneMap geneMap,
            final Set<String> uniqueGeneSet) throws Exception {
        
        Map<Long, Collection<NodeAttributesElement>> attribMap = network.getNodeAttributes();
        Map<String, Set<String>> mappy = geneMap.getGeneMap();
        if (mappy == null){
            _logger.debug("Adding mappy");
            mappy = new HashMap<>();
            geneMap.setGeneMap((Map<String, Set<String>>)mappy);
        }
        for (NodesElement ne : network.getNodes().values()){
            Collection<NodeAttributesElement> nodeAttribs = attribMap.get(ne.getId());

            // If there are node attributes and one is named "type" then
            // only include the node name if type is gene or protein
            if (nodeAttribs != null){
                boolean validgene = false;
                for (NodeAttributesElement nae : nodeAttribs){
                    if (nae.getName().toLowerCase().equals("type")){
                        if (nae.getValue().toLowerCase().equals("gene") ||
                              nae.getValue().toLowerCase().equals("protein")){
                            validgene = true;
                            break;
                        }
                    }
                }
                if (validgene == false){
                    continue;
                }
            }
            String name = ne.getNodeName();

            if (mappy.containsKey(name) == false){
                mappy.put(name, new HashSet<String>());
            }
            if (mappy.get(name).contains(externalId) == false){
                mappy.get(name).add(externalId);
            }
            uniqueGeneSet.add(name);
        }
    }
    
    public static NiceCXNetwork saveNetwork(NdexRestClientModelAccessLayer client, final UUID networkuuid, final File savedir) throws Exception{
        Configuration config = Configuration.getInstance();
        File dest = new File(savedir.getAbsolutePath() + File.separator + networkuuid.toString() + ".cx");
        
        FileOutputStream fos = new FileOutputStream(dest);
        InputStream instream = client.getNetworkAsCXStream(networkuuid);
        byte[] buffer = new byte[8 * 1024];
        int bytesRead;
        while ((bytesRead = instream.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }
        try {
            instream.close();
        }
        catch(IOException ex){
            _logger.error("error closing input stream", ex);
        }
        try {
            fos.close();
        }
        catch(IOException ex){
            _logger.error("error closing output stream", ex);
        }
        
        ObjectMapper mappy = new ObjectMapper();
        FileInputStream fis = new FileInputStream(dest);
        return NdexRestClientUtilities.getCXNetworkFromStream(fis);
    }
}
