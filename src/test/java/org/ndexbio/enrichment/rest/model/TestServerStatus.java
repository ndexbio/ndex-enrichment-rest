/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.model;

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
    public void testConstructor(){
        ServerStatus ss = new ServerStatus();
        assertEquals(ss.getRestVersion(), null);
    }
}
