/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.ndexbio.enrichment.rest.model.ServerStatus;

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

        Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
        dispatcher.getRegistry().addSingletonResource(new Status());
        
        MockHttpRequest request = MockHttpRequest.get("/enrichment/status");
        
        MockHttpResponse response = new MockHttpResponse();
        dispatcher.invoke(request, response);
        assertEquals(200, response.getStatus());
        ObjectMapper mapper = new ObjectMapper();
        ServerStatus ss = mapper.readValue(response.getOutput(),
                ServerStatus.class);
        assertTrue(ss.getRestVersion() != null);
    }
    
}
