package org.ndexbio.enrichment.rest;

import java.util.Set;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author churas
 */
public class TestEnrichmentApplication {
    
    @Test
    public void testGetSingletons(){
        EnrichmentApplication ea = new EnrichmentApplication();
        Set<Object> objs = ea.getSingletons();
        assertEquals(1, objs.size());
        assertTrue(objs.iterator().next().getClass().getName().contains("CorsFilter"));
    }
    
    @Test
    public void testGetClasses(){
         EnrichmentApplication ea = new EnrichmentApplication();
         Set<Class<?>> classObjs = ea.getClasses();
         assertEquals(5, classObjs.size());
    }
}
