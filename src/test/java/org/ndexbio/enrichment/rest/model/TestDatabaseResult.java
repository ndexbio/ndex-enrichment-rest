package org.ndexbio.enrichment.rest.model;

import org.junit.jupiter.api.Test;
import org.ndexbio.ndexsearch.rest.model.DatabaseResult;
import org.ndexbio.ndexsearch.rest.model.NetworkInfo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author churas
 */
public class TestDatabaseResult {
    
    @Test
    public void testGettersAndSetters(){
        DatabaseResult dr = new DatabaseResult();
        assertEquals(null, dr.getDescription());
        assertEquals(null, dr.getName());
        assertEquals(null, dr.getNetworks());
        assertEquals(null, dr.getUuid());
        assertEquals(null, dr.getImageURL());
        dr.setDescription("description");
        dr.setName("name");
        NetworkInfo nw = new NetworkInfo();
        nw.setName("network");
        List<NetworkInfo> networkList = new ArrayList<>();
        networkList.add(nw);
        dr.setNetworks(networkList);
        dr.setUuid("2");
        dr.setImageURL("image");
        assertEquals("description", dr.getDescription());
        assertEquals("name", dr.getName());
        assertEquals("network", dr.getNetworks().get(0).getName());
        assertEquals("2", dr.getUuid());
        assertEquals("image", dr.getImageURL());
    }
}
