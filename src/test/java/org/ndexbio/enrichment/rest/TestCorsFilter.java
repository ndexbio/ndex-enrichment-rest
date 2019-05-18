/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest;

import java.io.IOException;
import static org.easymock.EasyMock.*;


import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.easymock.EasyMockSupport;
import org.junit.Before;

/**
 *
 * @author churas
 */
public class TestCorsFilter {
    
    private EasyMockSupport support = new EasyMockSupport();
    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;
    private FilterChain mockFilterChain;
    
    @Before
    public void setupResponse() {
        mockRequest = support.mock(HttpServletRequest.class);
        mockResponse = support.mock(HttpServletResponse.class);
        mockResponse.addHeader("Access-Control-Allow-Origin", "*");
        mockResponse.addHeader("Access-Control-Allow-Methods","GET, OPTIONS, HEAD, PUT, POST, DELETE");
        mockResponse.addHeader("Access-Control-Allow-Headers", "Origin, payload, Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
        mockResponse.addHeader("Access-Control-Expose-Headers", "Location");
        mockFilterChain = support.mock(FilterChain.class);
    }
    
    @Test
    public void testdoFilterOnNonOptionsRequest() throws IOException, ServletException {
        expect(mockRequest.getMethod()).andReturn("GET");
        mockFilterChain.doFilter(mockRequest, mockResponse);
        support.replayAll();
        CorsFilter cf = new CorsFilter();
        cf.doFilter(mockRequest, mockResponse, mockFilterChain);
        support.verifyAll();
        
    }
    
    @Test
    public void testdoFilterOptionsRequest() throws IOException, ServletException {
        expect(mockRequest.getMethod()).andReturn("OPTIONS");
        mockResponse.setStatus(HttpServletResponse.SC_ACCEPTED);
        support.replayAll();
        CorsFilter cf = new CorsFilter();
        cf.doFilter(mockRequest, mockResponse, mockFilterChain);
        support.verifyAll();
        
    }
}
