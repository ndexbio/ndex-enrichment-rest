/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.net.URI;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.enrichment.rest.engine.EnrichmentEngine;
import org.ndexbio.enrichment.rest.model.EnrichmentQuery;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryResults;
import org.ndexbio.enrichment.rest.model.EnrichmentQueryStatus;
import org.ndexbio.enrichment.rest.model.ErrorResponse;
import org.ndexbio.enrichment.rest.model.Task;
import org.ndexbio.enrichment.rest.model.exceptions.EnrichmentException;

/**
 *
 * @author churas
 */
public class TestEnrichment {
    
    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();
   
    @Test
    public void testRequestEnrichmentWhereEnrichmentEngineNotLoaded() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Enrichment());

            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH);
            EnrichmentQuery query = new EnrichmentQuery();
            ObjectMapper omappy = new ObjectMapper();
            request.contentType(MediaType.APPLICATION_JSON);
            
            request.content(omappy.writeValueAsBytes(query));


            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            Configuration.getInstance().setEnrichmentEngine(null);
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error requesting enrichment", er.getMessage());
            assertEquals("Enrichment Engine not loaded", er.getDescription());
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testRequestEnrichmentWhereQueryRaisesError() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Enrichment());

            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH);
            EnrichmentQuery query = new EnrichmentQuery();
            ObjectMapper omappy = new ObjectMapper();
            request.contentType(MediaType.APPLICATION_JSON);
            
            request.content(omappy.writeValueAsBytes(query));


            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            
            // create mock enrichment engine that returns null
            EnrichmentEngine mockEngine = createMock(EnrichmentEngine.class);
            expect(mockEngine.query(notNull())).andThrow(new EnrichmentException("some error"));
            replay(mockEngine);
            Configuration.getInstance().setEnrichmentEngine(mockEngine);
            
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error requesting enrichment", er.getMessage());
            assertEquals("some error", er.getDescription());
            verify(mockEngine);

        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testRequestEnrichmentWhereQueryReturnsNull() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Enrichment());

            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH);
            EnrichmentQuery query = new EnrichmentQuery();
            ObjectMapper omappy = new ObjectMapper();
            request.contentType(MediaType.APPLICATION_JSON);
            
            request.content(omappy.writeValueAsBytes(query));


            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            
            // create mock enrichment engine that returns null
            EnrichmentEngine mockEngine = createMock(EnrichmentEngine.class);
            expect(mockEngine.query(notNull())).andReturn(null);
            replay(mockEngine);
            Configuration.getInstance().setEnrichmentEngine(mockEngine);
            
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error requesting enrichment", er.getMessage());
            assertEquals("No id returned from enrichment engine", er.getDescription());
            verify(mockEngine);

        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testRequestEnrichmentWhereQuerySuccess() throws Exception {
        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Enrichment());

            MockHttpRequest request = MockHttpRequest.post(Configuration.V_ONE_PATH);
            EnrichmentQuery query = new EnrichmentQuery();
            ObjectMapper omappy = new ObjectMapper();
            request.contentType(MediaType.APPLICATION_JSON);
            
            request.content(omappy.writeValueAsBytes(query));


            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            
            // create mock enrichment engine that returns null
            EnrichmentEngine mockEngine = createMock(EnrichmentEngine.class);
            expect(mockEngine.query(notNull())).andReturn("12345");
            replay(mockEngine);
            Configuration.getInstance().setEnrichmentEngine(mockEngine);
            
            dispatcher.invoke(request, response);
            assertEquals(202, response.getStatus());
            
            MultivaluedMap<String, Object> resmap = response.getOutputHeaders();
            assertEquals(new URI("/v1/12345"), resmap.getFirst("Location"));
            ObjectMapper mapper = new ObjectMapper();
            Task t = mapper.readValue(response.getOutput(),
                    Task.class);
            assertEquals("12345", t.getId());
            verify(mockEngine);

        } finally {
            _folder.delete();
        }
    }

    @Test
    public void testGetWhereEnrichmentEngineNotLoaded() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Enrichment());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            Configuration.getInstance().setEnrichmentEngine(null);
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error getting results for id: 12345", er.getMessage());
            assertEquals("Enrichment Engine not loaded", er.getDescription());
        } finally {
            _folder.delete();
        }
    }
    
    @Test
    public void testGetWhereIdDoesNotExist() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Enrichment());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            
            // create mock enrichment engine that returns null
            EnrichmentEngine mockEngine = createMock(EnrichmentEngine.class);
            expect(mockEngine.getQueryResults("12345", 0, 0)).andReturn(null);
            replay(mockEngine);
            Configuration.getInstance().setEnrichmentEngine(mockEngine);
            
            dispatcher.invoke(request, response);
            assertEquals(410, response.getStatus());
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setEnrichmentEngine(null);

        }
    }
    
    @Test
    public void testGetWhereIdExistsAndStartSizeSet() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Enrichment());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH +
                                                          "/12345?start=1&size=2");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            
            // create mock enrichment engine that returns null
            EnrichmentEngine mockEngine = createMock(EnrichmentEngine.class);
            EnrichmentQueryResults eqr = new EnrichmentQueryResults();
            eqr.setNumberOfHits(100);
            expect(mockEngine.getQueryResults("12345", 1, 2)).andReturn(eqr);
            replay(mockEngine);
            Configuration.getInstance().setEnrichmentEngine(mockEngine);
            
            dispatcher.invoke(request, response);
            assertEquals(200, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            EnrichmentQueryResults res = mapper.readValue(response.getOutput(),
                    EnrichmentQueryResults.class);
            assertEquals(100, res.getNumberOfHits());
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setEnrichmentEngine(null);
        }
    }
    
    @Test
    public void testGetStatusWhereEnrichmentEngineNotLoaded() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Enrichment());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345/status");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            Configuration.getInstance().setEnrichmentEngine(null);

            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error getting results for id: 12345", er.getMessage());
            assertEquals("Enrichment Engine not loaded", er.getDescription());
        } finally {
            _folder.delete();
            Configuration.getInstance().setEnrichmentEngine(null);

        }
    }
    
    @Test
    public void testGetStatusWhereIdDoesNotExist() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Enrichment());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345/status");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            
            // create mock enrichment engine that returns null
            EnrichmentEngine mockEngine = createMock(EnrichmentEngine.class);
            expect(mockEngine.getQueryStatus("12345")).andReturn(null);
            replay(mockEngine);
            Configuration.getInstance().setEnrichmentEngine(mockEngine);
            
            dispatcher.invoke(request, response);
            assertEquals(410, response.getStatus());
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setEnrichmentEngine(null);

        }
    }
    
    @Test
    public void testGetStatusWhereIdExists() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Enrichment());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH +
                                                          "/12345/status");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            
            // create mock enrichment engine that returns null
            EnrichmentEngine mockEngine = createMock(EnrichmentEngine.class);
            EnrichmentQueryStatus eqs = new EnrichmentQueryStatus();
            eqs.setProgress(55);
            expect(mockEngine.getQueryStatus("12345")).andReturn(eqs);
            replay(mockEngine);
            Configuration.getInstance().setEnrichmentEngine(mockEngine);
            
            dispatcher.invoke(request, response);
            assertEquals(200, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            EnrichmentQueryStatus res = mapper.readValue(response.getOutput(),
                    EnrichmentQueryStatus.class);
            assertEquals(55, res.getProgress());
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setEnrichmentEngine(null);
        }
    }
    
    @Test
    public void testDeleteWhereEnrichmentEngineNotLoaded() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Enrichment());

            MockHttpRequest request = MockHttpRequest.delete(Configuration.V_ONE_PATH + "/12345");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            Configuration.getInstance().setEnrichmentEngine(null);

            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error deleting: 12345", er.getMessage());
            assertEquals("Enrichment Engine not loaded", er.getDescription());
        } finally {
            _folder.delete();
            Configuration.getInstance().setEnrichmentEngine(null);

        }
    }
    
    @Test
    public void testDeleteSuccessful() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Enrichment());

            MockHttpRequest request = MockHttpRequest.delete(Configuration.V_ONE_PATH +
                                                          "/12345");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            
            // create mock enrichment engine that returns null
            EnrichmentEngine mockEngine = createMock(EnrichmentEngine.class);
            mockEngine.delete("12345");
            replay(mockEngine);
            Configuration.getInstance().setEnrichmentEngine(mockEngine);
            
            dispatcher.invoke(request, response);
            assertEquals(200, response.getStatus());
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setEnrichmentEngine(null);
        }
    }
    
    @Test
    public void testGetOverlayNetworkWhereEnrichmentEngineNotLoaded() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Enrichment());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345/overlaynetwork");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            Configuration.getInstance().setEnrichmentEngine(null);

            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error getting overlay network for id: 12345", er.getMessage());
            assertEquals("Enrichment Engine not loaded", er.getDescription());
        } finally {
            _folder.delete();
            Configuration.getInstance().setEnrichmentEngine(null);

        }
    }
    
    @Test
    public void testGetOverlayNetworkWhereIdDoesNotExist() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Enrichment());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345/overlaynetwork?databaseUUID=dbid&networkUUID=netid");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            
            // create mock enrichment engine that returns null
            EnrichmentEngine mockEngine = createMock(EnrichmentEngine.class);
            expect(mockEngine.getNetworkOverlayAsCX("12345", "dbid", "netid")).andReturn(null);
            replay(mockEngine);
            Configuration.getInstance().setEnrichmentEngine(mockEngine);
            
            dispatcher.invoke(request, response);
            assertEquals(410, response.getStatus());
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setEnrichmentEngine(null);

        }
    }
    
    @Test
    public void testGetOverlayNetworkWhereIdExists() throws Exception {

        try {
            File tempDir = _folder.newFolder();
            File confFile = new File(tempDir.getAbsolutePath() + File.separator + "foo.conf");
            
            FileWriter fw = new FileWriter(confFile);
            
            fw.write(Configuration.DATABASE_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.write(Configuration.TASK_DIR + " = " + tempDir.getAbsolutePath() + "\n");
            fw.flush();
            fw.close();
            Dispatcher dispatcher = MockDispatcherFactory.createDispatcher();
            dispatcher.getRegistry().addSingletonResource(new Enrichment());

            MockHttpRequest request = MockHttpRequest.get(Configuration.V_ONE_PATH + "/12345/overlaynetwork?databaseUUID=dbid&networkUUID=netid");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            
            // create mock enrichment engine that returns null
            EnrichmentEngine mockEngine = createMock(EnrichmentEngine.class);
            String jsonStr = "{\"a\": \"b\"}";
            byte[] strAsByte = jsonStr.getBytes();
            ByteArrayInputStream iStream = new ByteArrayInputStream(strAsByte);
            expect(mockEngine.getNetworkOverlayAsCX("12345", "dbid", "netid")).andReturn(iStream);
            replay(mockEngine);
            Configuration.getInstance().setEnrichmentEngine(mockEngine);
            
            dispatcher.invoke(request, response);
            assertEquals(200, response.getStatus());
            assertEquals(jsonStr, response.getContentAsString());
            verify(mockEngine);
        } finally {
            _folder.delete();
            Configuration.getInstance().setEnrichmentEngine(null);
        }
    }
}
