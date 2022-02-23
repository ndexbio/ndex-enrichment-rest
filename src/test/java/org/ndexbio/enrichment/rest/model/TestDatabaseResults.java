/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.model;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.ndexbio.ndexsearch.rest.model.DatabaseResult;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author churas
 */
public class TestDatabaseResults {
    
    @Test
    public void testCopyConstructor(){
        //null copy
        DatabaseResults dr = new DatabaseResults(null);
        assertEquals(null, dr.getResults());
        
        //valid copy of empty idr
        InternalDatabaseResults idr = new InternalDatabaseResults();
        dr = new DatabaseResults(idr);
        assertEquals(null, dr.getResults());
        
        //valid copy of idr with results
        List<DatabaseResult> results = new ArrayList<>();
        DatabaseResult ares = new DatabaseResult();
        ares.setName("name");
        results.add(ares);
        idr.setResults(results);
        dr = new DatabaseResults(idr);
        assertEquals(1, dr.getResults().size());
        assertEquals("name", dr.getResults().get(0).getName());
        
    }
    
}
