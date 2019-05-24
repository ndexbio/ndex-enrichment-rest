/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileWriter;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.*;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.enrichment.rest.engine.EnrichmentEngine;
import org.ndexbio.enrichment.rest.model.ErrorResponse;
import org.ndexbio.enrichment.rest.model.ServerStatus;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;


/**
 *
 * @author churas
 */
public class TestStatus {
    
    public Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
    
    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();
    
    public TestStatus() {
    }
    
    @Test
    public void testGetEnrichmentEngineNull() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Status());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + Status.STATUS_PATH);

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            Configuration.getInstance().setEnrichmentEngine(null);

            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error retreiving server status", er.getMessage());
            assertEquals("Enrichment Engine not loaded", er.getDescription());
            
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testGetServerStatusIsNull() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Status());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + Status.STATUS_PATH);

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            
            // create mock enrichment engine that returns null
            EnrichmentEngine mockEngine = createMock(EnrichmentEngine.class);
            expect(mockEngine.getServerStatus()).andReturn(null);
            replay(mockEngine);
            
            Configuration.getInstance().setEnrichmentEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error retreiving server status", er.getMessage());
            assertEquals("No Server Status object returned", er.getDescription());
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setEnrichmentEngine(null);
        }
    }
    
    @Test
    public void testGetServerStatusThrowsException() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Status());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + Status.STATUS_PATH);

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            
            // create mock enrichment engine that returns null
            EnrichmentEngine mockEngine = createMock(EnrichmentEngine.class);
            expect(mockEngine.getServerStatus()).andThrow(new EnrichmentException("hi"));
            replay(mockEngine);
            
            Configuration.getInstance().setEnrichmentEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error retreiving server status", er.getMessage());
            assertEquals("hi", er.getDescription());
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setEnrichmentEngine(null);
        }
    }
    
    @Test
    public void testGetServerStatusSuccess() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Status());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + Status.STATUS_PATH);

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            
            // create mock enrichment engine that returns null
            EnrichmentEngine mockEngine = createMock(EnrichmentEngine.class);
            ServerStatus sObj = new ServerStatus();
            sObj.setRestVersion("1.1.1");
            expect(mockEngine.getServerStatus()).andReturn(sObj);
            replay(mockEngine);
            
            Configuration.getInstance().setEnrichmentEngine(mockEngine);

            dispatcher.invoke(request, response);
            assertEquals(200, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ServerStatus res = mapper.readValue(response.getOutput(),
                    ServerStatus.class);
            assertEquals("1.1.1", res.getRestVersion());
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setEnrichmentEngine(null);
        }
    }
}
