/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment;


import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.ndexbio.cxio.aspects.datamodels.NodeAttributesElement;
import org.ndexbio.cxio.aspects.datamodels.NodesElement;
import org.ndexbio.cxio.core.NdexCXNetworkWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.ndexbio.enrichment.rest.exceptions.EnrichmentException;
import org.ndexbio.enrichment.rest.model.DatabaseResult;
import org.ndexbio.enrichment.rest.model.InternalDatabaseResults;
import org.ndexbio.enrichment.rest.model.InternalGeneMap;
import org.ndexbio.enrichment.rest.services.Configuration;
import org.ndexbio.model.cx.NiceCXNetwork;
import org.ndexbio.model.object.NetworkSearchResult;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;
import org.ndexbio.rest.client.NdexRestClientUtilities;

/**
 *
 * @author churas
 */
public class App {
    
    static Logger _logger = LoggerFactory.getLogger(App.class);

    public static final String MODE = "mode";
    public static final String CONF = "conf";    
    public static final String CREATEDB_MODE = "createdb";
    public static final String EXAMPLE_CONF_MODE = "exampleconf";
    public static final String EXAMPLE_DBRES_MODE = "exampledbresults";
    
    public static final String SUPPORTED_MODES = CREATEDB_MODE +
                                                    ", " + EXAMPLE_CONF_MODE +
                                                    ", " + EXAMPLE_DBRES_MODE;
    
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
                    System.out.println("\n\nHelp\n\n");
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
            
        }
        catch(Exception ex){
            ex.printStackTrace();
        }

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
        String druuid = "89a90a24-2fa8-4a57-ae4b-7c30a180e8e6";
        dr.setUuid(druuid);
        
        DatabaseResult drtwo = new DatabaseResult();
        drtwo.setDescription("This is a description of a ncipid database");
        drtwo.setName("ncipid");
        drtwo.setNumberOfNetworks("200");
        String drtwouuid = "e508cf31-79af-463e-b8b6-ff34c87e1734";
        drtwo.setUuid(drtwouuid);
        
        InternalDatabaseResults idr = new InternalDatabaseResults();
        HashMap<String, String> hmap = new HashMap<>();
        hmap.put(druuid, "signorowner");
        hmap.put(drtwouuid, "ncipidowner");
        idr.setDatabaseAccountOwnerMap(hmap);
        idr.setResults(Arrays.asList(dr, drtwo));
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
        sb.append(Configuration.DATABASE_DIR + " = /tmp\n\n");
        
        sb.append("# Sets Enrichment task directory where results from queries are stored\n");
        sb.append(Configuration.TASK_DIR + " = /tmp/tasks\n\n");

        sb.append(Configuration.DATABASE_RESULTS_JSON_FILE+ " = databaseresults.json\n");
        sb.append(Configuration.NDEX_USER+ " = bob\n");
        sb.append(Configuration.NDEX_PASS+ " = somepassword\n");
        sb.append(Configuration.NDEX_SERVER+ " = public.ndexbio.org\n");
        sb.append(Configuration.NDEX_USERAGENT+ " = Enrichment/1.0\n");
        return sb.toString();
    }

    public static void downloadNetworks() throws Exception {
        Configuration config = Configuration.getInstance();
        NdexRestClientModelAccessLayer client = config.getNDExClient();
        InternalDatabaseResults idr = config.getNDExDatabases();
        ObjectMapper mappy = new ObjectMapper();
        List<InternalGeneMap> geneMapList = new LinkedList<InternalGeneMap>();
        Map<String, Integer> databaseUniqueGeneCount = new HashMap<>();
        for (DatabaseResult dr : idr.getResults()){
            InternalGeneMap geneMap = new InternalGeneMap();
            geneMap.setDatabaseUUID(dr.getUuid());
            String networkOwner = idr.getDatabaseAccountOwnerMap().get(dr.getUuid());
            File databasedir = new File(config.getEnrichmentDatabaseDirectory() + File.separator + dr.getUuid());
            if (databasedir.isDirectory() == false){
                _logger.debug("Creating directory " + databasedir.getAbsolutePath());
                databasedir.mkdirs();
            }
            NetworkSearchResult nrs = client.findNetworks("", networkOwner, 0, 0);
            Set<String> uniqueGeneSet = new HashSet<>();
            for (NetworkSummary ns :  nrs.getNetworks()){
                _logger.debug(ns.getName() + " Nodes => " + Integer.toString(ns.getNodeCount()) + " Edges => " + Integer.toString(ns.getEdgeCount()));
                NiceCXNetwork network = saveNetwork(ns.getExternalId(), databasedir);
                updateGeneMap(network, ns.getExternalId().toString(), geneMap,
                        uniqueGeneSet);
            }
            geneMapList.add(geneMap);
            databaseUniqueGeneCount.put(dr.getUuid(), uniqueGeneSet.size());
            uniqueGeneSet.clear();
        }
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
    
    public static NiceCXNetwork saveNetwork(final UUID networkuuid, final File savedir) throws Exception{
        Configuration config = Configuration.getInstance();
        NdexRestClientModelAccessLayer client = config.getNDExClient();
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
