/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author churas
 */
public class EnrichmentHttpServletDispatcher extends HttpServletDispatcher {
    
    static Logger _logger = LoggerFactory.getLogger(EnrichmentHttpServletDispatcher.class.getSimpleName());

    private static String _version = "";
    private static String _buildNumber = "";

    public EnrichmentHttpServletDispatcher(){
        super();
        _logger.info("In constructor");
    }

    @Override
    public void init(javax.servlet.ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        _logger.info("In init");
        updateVersion();
    }
    
    @Override
    public void destroy() {
        super.destroy();
        _logger.info("In destroy()");
    }
    
    /**
     * Reads /META-INFO/MANIFEST.MF for version and build information
     * setting _version and _buildNumber to those values if found.
     */
    private void updateVersion(){
        ServletContext application = getServletConfig().getServletContext();
        try(InputStream inputStream = application.getResourceAsStream("/META-INF/MANIFEST.MF")) {
            if ( inputStream !=null) {
                try {
                    Manifest manifest = new Manifest(inputStream);

                    Attributes aa = manifest.getMainAttributes();	

                    String ver = aa.getValue("NDExEnrichment-Version");
                    String bui = aa.getValue("NDExEnrichment-Build"); 
                    _logger.info("NDEx-Version: " + ver + ",Build:" + bui);
                    _buildNumber= bui.substring(0, 5);
                    _version = ver;
                } catch (IOException e) {
                    _logger.error("failed to read MANIFEST.MF", e);
                }     
            }    
        } catch (IOException e1) {
            _logger.error("Failed to close InputStream from MANIFEST.MF", e1);
        }
    }
    
    public static String getVersion(){
        return _version;
    }
    
    public static String getBuildNumber(){
        return _buildNumber;
    }
}
