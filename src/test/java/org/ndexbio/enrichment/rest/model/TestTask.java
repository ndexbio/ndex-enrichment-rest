package org.ndexbio.enrichment.rest.model;


import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author churas
 */
public class TestTask {
    
    @Test
    public void testGettersAndSetters(){
        Task t = new Task();
        assertEquals(null, t.getId());
        assertEquals(null, t.getWebURL());
        t.setId("hi");
        t.setWebURL("https://foo.com");
        assertEquals("hi", t.getId());
        assertEquals("https://foo.com", t.getWebURL());
    }
    
}
