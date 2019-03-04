/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileWriter;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ndexbio.enrichment.rest.model.ErrorResponse;

/**
 *
 * @author churas
 */
public class TestEnrichment {
    
    @Rule
    public TemporaryFolder _folder = new TemporaryFolder();
   

    @Test
    public void testGetWithIdThatDoesNotExist() throws Exception {

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

            MockHttpRequest request = MockHttpRequest.get(Configuration.BASE_REST_PATH + "12345");

            MockHttpResponse response = new MockHttpResponse();
            Configuration.setAlternateConfigurationFile(confFile.getAbsolutePath());
            dispatcher.invoke(request, response);
            assertEquals(500, response.getStatus());
            ObjectMapper mapper = new ObjectMapper();
            ErrorResponse er = mapper.readValue(response.getOutput(),
                    ErrorResponse.class);
            assertEquals("Error getting results for id: 12345", er.getMessage());
        } finally {
            _folder.delete();
        }
    }

}
