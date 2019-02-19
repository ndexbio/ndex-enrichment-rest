/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.services;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.*;
import org.jboss.resteasy.plugins.server.resourcefactory.POJOResourceFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 *
 * @author churas
 */
public class TestStatus {
    
    public Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
    
    
    public TestStatus() {
    }
   
  
    @Test
    public void testGet() throws Exception {
        POJOResourceFactory noDefaults = new POJOResourceFactory(Status.class);

        Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
        dispatcher.getRegistry().addResourceFactory(noDefaults);
        
        MockHttpRequest request = MockHttpRequest.get("/status");
        
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        assertEquals(200, response.getStatus());
        assertTrue(response.getContentAsString().contains("{\"restVersion\":"));
        
    }
    
}
