/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment;


import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.ndexbio.cxio.aspects.datamodels.NodeAttributesElement;
import org.ndexbio.cxio.aspects.datamodels.NodesElement;
import org.ndexbio.model.cx.NiceCXNetwork;
import org.ndexbio.model.object.NdexStatus;
import org.ndexbio.model.object.NetworkSearchResult;
import org.ndexbio.model.object.network.NetworkSummary;

import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

/**
 *
 * @author churas
 */
public class App {
    
    public static final String USER = "user";
    public static final String PASS = "pass";
    public static final String SERVER = "server";
    public static final String DB_NAME = "databasename";
    public static final String DB_DIR = "dir";
    public static final String NETWORKOWNER = "networkowner";
           
    public static void main(String[] args){

        final List<String> helpArgs = Arrays.asList("h", "help", "?");
        try {
            OptionParser parser = new OptionParser() {

                {
                    accepts(NETWORKOWNER, "User who owns networks").withRequiredArg().ofType(String.class).required();
                    accepts(USER, "Username")
                            .withRequiredArg().ofType(String.class).required();
                    accepts(PASS, "Password")
                            .withRequiredArg().ofType(String.class).required();
                    accepts(SERVER, "Server")
                            .withRequiredArg().ofType(String.class).defaultsTo("dev.ndexbio.org'").required();
                    accepts(DB_NAME, "database name ie signor, ncipid")
                            .withRequiredArg().ofType(String.class).required();
                    accepts(DB_DIR, "Destination directory")
                            .withRequiredArg().ofType(File.class).required();
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
            
            NdexRestClientModelAccessLayer client = getNdexRestClient(optionSet.valueOf(USER).toString(),
                    optionSet.valueOf(PASS).toString(), optionSet.valueOf(SERVER).toString());
            
            NdexStatus stat = client.getServerStatus();
            System.out.println(stat.getProperties().toString());
            
            File dbdir = (File)optionSet.valueOf(DB_DIR);
            dbdir.mkdirs();
            
            HashMap<String, HashSet<String>> mappy = buildMap(client, dbdir,
                    optionSet.valueOf(NETWORKOWNER).toString());
            
            System.out.println("Mappy size is: " + mappy.size());
            for (String key : mappy.keySet()){
                System.out.println(key + " => " + mappy.get(key).size());
            }
            System.out.println("Mappy size is: " + mappy.size());
            
            System.out.println("Networks with HDAC1");
            for (String val: mappy.get("HDAC1")){
                System.out.println(val);
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
        }

    }
    public static HashMap<String, HashSet<String>> buildMap(NdexRestClientModelAccessLayer client,
            File dbdir, final String networkOwner) throws Exception {
        
        //for (NetworkSummary ns : client.getMyNetworks()){
        //    System.out.println(ns.getName() + " Nodes => " + Integer.toString(ns.getNodeCount()) + " Edges => " + Integer.toString(ns.getEdgeCount()));
        //}
        HashMap<String, HashSet<String>> mappy = new HashMap<String, HashSet<String>>();
        NetworkSearchResult nrs = client.findNetworks("", networkOwner, 0, 0);
        for (NetworkSummary ns :  nrs.getNetworks()){
            System.out.println(ns.getName() + " Nodes => " + Integer.toString(ns.getNodeCount()) + " Edges => " + Integer.toString(ns.getEdgeCount()));
            NiceCXNetwork network = client.getNetwork(ns.getExternalId());
            Map<Long, Collection<NodeAttributesElement>> attribMap = network.getNodeAttributes();
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
                if (mappy.get(name).contains(ns.getExternalId()) == false){
                    mappy.get(name).add(ns.getExternalId().toString());
                }
            }
        }
        return mappy;
    }
    public static NdexRestClientModelAccessLayer getNdexRestClient(final String user,
            final String pass, final String server) throws Exception{
        NdexRestClient nrc = new NdexRestClient(user, pass, server);
        return new NdexRestClientModelAccessLayer(nrc);
    }
    
}
