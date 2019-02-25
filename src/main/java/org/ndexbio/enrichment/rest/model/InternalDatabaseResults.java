/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.model;

import java.util.List;
import java.util.Map;

/**
 *
 * @author churas
 */
public class InternalDatabaseResults extends DatabaseResults {
    
    private Map<String, String> _databaseAccountOwnerMap;
    private List<InternalGeneMap> _geneMapList;
    private Map<String, Integer> _databaseUniqueGeneCount;
    private int _universeUniqueGeneCount;

    /**
     * Gets map of account owner for networks on NDEx for given databases
     * The map is <database uuid> => <account name>
     * @return
     */
    public Map<String, String> getDatabaseAccountOwnerMap() {
        return _databaseAccountOwnerMap;
    }

    /**
     * Gets map of account owner for networks on NDEx for given databases
     * The map is <database uuid> => <account name>
     * @return
     */
    public void setDatabaseAccountOwnerMap(Map<String, String> _databaseAccountOwnerMap) {
        this._databaseAccountOwnerMap = _databaseAccountOwnerMap;
    }

    public List<InternalGeneMap> getGeneMapList() {
        return _geneMapList;
    }

    public void setGeneMapList(List<InternalGeneMap> _geneMapList) {
        this._geneMapList = _geneMapList;
    }

    public Map<String, Integer> getDatabaseUniqueGeneCount() {
        return _databaseUniqueGeneCount;
    }

    public void setDatabaseUniqueGeneCount(Map<String, Integer> _databaseUniqueGeneCount) {
        this._databaseUniqueGeneCount = _databaseUniqueGeneCount;
    }

    public int getUniverseUniqueGeneCount() {
        return _universeUniqueGeneCount;
    }

    public void setUniverseUniqueGeneCount(int _universeUniqueGeneCount) {
        this._universeUniqueGeneCount = _universeUniqueGeneCount;
    }

    
    
    
}
