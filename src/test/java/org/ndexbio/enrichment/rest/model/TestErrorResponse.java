/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;
/**
 *
 * @author churas
 */
public class TestErrorResponse {
    
    @Test
    public void testConstructor(){
        ErrorResponse er = new ErrorResponse("hi", new EnrichmentException("yo"));

        assertEquals("hi", er.getMessage());
        assertEquals("yo", er.getDescription());
        assertEquals(Long.toString(Thread.currentThread().getId()), er.getThreadId());
        assertNotNull(er.getTimeStamp());
        assertTrue(er.getStackTrace().contains("testConstructor"));
    }
    
    @Test
    public void testAsJson(){
        ErrorResponse er = new ErrorResponse("hi", new EnrichmentException("yo"));
        String res = er.asJson();
        assertTrue(res.startsWith("{\"message\": \"hi\","));
        assertTrue(res.contains("\"description\": \"yo\","));
    }
    
    @Test
    public void testGettersAndSetters(){
        ErrorResponse er = new ErrorResponse("hi", new EnrichmentException("yo"));
        assertEquals("hi", er.getMessage());
        assertEquals("yo", er.getDescription());
        assertEquals(null, er.getErrorCode());
        assertTrue(er.getStackTrace().contains("TestErrorResponse"));
        assertEquals(Long.toString(Thread.currentThread().getId()),
                     er.getThreadId());
        er.setMessage("message");
        er.setDescription("description");
        er.setErrorCode("2");
        er.setStackTrace("trace");
        er.setThreadId("thread");
        er.setTimeStamp("hi");
        assertEquals("message", er.getMessage());
        assertEquals("description", er.getDescription());
        assertEquals("2", er.getErrorCode());
        assertEquals("trace", er.getStackTrace());
        assertEquals("thread", er.getThreadId());
        assertEquals("hi", er.getTimeStamp());
    }
}
