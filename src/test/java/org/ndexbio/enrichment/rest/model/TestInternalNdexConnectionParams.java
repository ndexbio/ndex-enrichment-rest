package org.ndexbio.enrichment.rest.model;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
/**
 *
 * @author churas
 */
public class TestInternalNdexConnectionParams {
    
    @Test
    public void testGettersAndSetters(){
        InternalNdexConnectionParams p = new InternalNdexConnectionParams();
        assertEquals(null, p.getNetworkSetId());
        assertEquals(null, p.getPassword());
        assertEquals(null, p.getServer());
        assertEquals(null, p.getUser());
        
        p.setNetworkSetId("netset");
        p.setPassword("pass");
        p.setServer("server");
        p.setUser("user");
        assertEquals("netset", p.getNetworkSetId());
        assertEquals("pass", p.getPassword());
        assertEquals("server", p.getServer());
        assertEquals("user", p.getUser());
    }
}
