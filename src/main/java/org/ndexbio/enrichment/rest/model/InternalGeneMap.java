package org.ndexbio.enrichment.rest.model;

import java.util.Map;
import java.util.Set;

/**
 *
 * @author churas
 */
public class InternalGeneMap {
    
    private String _databaseUUID;
    private Map<String, Set<String>> _geneMap;

    public String getDatabaseUUID() {
        return _databaseUUID;
    }

    public void setDatabaseUUID(String _databaseUUID) {
        this._databaseUUID = _databaseUUID;
    }

    public Map<String, Set<String>> getGeneMap() {
        return _geneMap;
    }

    public void setGeneMap(Map<String, Set<String>> _geneMap) {
        this._geneMap = _geneMap;
    }
    
}
