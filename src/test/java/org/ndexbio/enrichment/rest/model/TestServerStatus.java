/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.model;

import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author churas
 */
public class TestServerStatus {
    
    public TestServerStatus() {
    }
    

    @Test
    public void testGettersAndSetters(){
        ServerStatus ss = new ServerStatus();
        assertEquals(null, ss.getLoad());
        assertEquals(0, ss.getPcDiskFull());
        assertEquals(null, ss.getQueries());
        assertEquals(null, ss.getRestVersion());
        assertEquals(null, ss.getStatus());
        
        ArrayList<Float> load = new ArrayList<>();
        load.add(Float.NaN);
        ss.setLoad(load);
        ss.setPcDiskFull(10);
        ArrayList<Integer> query = new ArrayList<>();
        query.add(1);
        ss.setQueries(query);
        
        ss.setRestVersion("version");
        ss.setStatus("status");
        
        assertEquals((float)Float.NaN, (float)ss.getLoad().get(0));
        assertEquals(10, ss.getPcDiskFull());
        assertEquals(1, (int)ss.getQueries().get(0));
        assertEquals("version", ss.getRestVersion());
        assertEquals("status", ss.getStatus());
        
        
        
    }
}
