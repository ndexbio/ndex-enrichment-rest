/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author churas
 */
public class TestApp {
    
    @Test
    public void testGenerateExampleConfiguration() throws Exception{
        String res = App.generateExampleConfiguration();
        assertTrue(res.contains("# Example configuration file for Enrichment service"));
    }
    
    @Test
    public void testExampleModes(){
        String[] args = {"--mode", App.EXAMPLE_CONF_MODE};
        App.main(args);
        String[] oargs = {"--mode", App.EXAMPLE_DBRES_MODE};
        App.main(oargs);
    }
    
    @Test
    public void testGetValidGene(){
        // check null works fine
        assertEquals(null, App.getValidGene(null));
        
        // check standard gene comes back okay
        assertEquals("TP53", App.getValidGene("TP53"));
        
        // check too long gene comes back null
        assertEquals(null,
                     App.getValidGene("AAAAAAAAAAAAAAAAAAAA"
                             + "AAAAAAAAAAAAAAAAAAAAA"));
        
        // invalid gene
        assertEquals(null, App.getValidGene("hi,how"));
        
        // with hgnc.symbol prefix
        assertEquals("HELLO", App.getValidGene("hgnc.symbol:HELLO"));
    }
}
