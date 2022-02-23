package org.ndexbio.enrichment.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author churas
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class InternalDatabaseResults extends DatabaseResults {
    
    private Map<String, InternalNdexConnectionParams> _databaseConnectionMap;
    private List<InternalGeneMap> _geneMapList;
    private Map<String, Integer> _databaseUniqueGeneCount;
    private Set<String> _networksToExclude;
    private Map<String, Map<String, Set<Long>>> _networkToGeneToNodeMap;
    private Map<String, Double> _idfMap;
	private Map<String, Set<String>> _networkGeneList;
    
    private int _universeUniqueGeneCount;
    private int _totalNetworkCount;

    public Map<String, InternalNdexConnectionParams> getDatabaseConnectionMap() {
        return _databaseConnectionMap;
    }

    public void setDatabaseConnectionMap(Map<String, InternalNdexConnectionParams> _databaseConnectionMap) {
        this._databaseConnectionMap = _databaseConnectionMap;
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
    
    public int getTotalNetworkCount() {
    	return _totalNetworkCount;
    }
    
    public void setTotalNetworkCount(int _totalNetworkCount) {
    	this._totalNetworkCount = _totalNetworkCount;
    }

    /**
     * Gets set of network UUIDs that should be excluded from database
     * @return 
     */
    public Set<String> getNetworksToExclude() {
        return _networksToExclude;
    }

    public void setNetworksToExclude(Set<String> _networksToExclude) {
        this._networksToExclude = _networksToExclude;
    }

    public Map<String, Map<String, Set<Long>>> getNetworkToGeneToNodeMap() {
        return _networkToGeneToNodeMap;
    }

    public void setNetworkToGeneToNodeMap(Map<String, Map<String, Set<Long>>> _networkToGeneToNodeMap) {
        this._networkToGeneToNodeMap = _networkToGeneToNodeMap;
    }
    
    public void setIdfMap(Map<String, Double> _idfMap) {
    	this._idfMap = _idfMap;
    }
    
    public Map<String, Double> getIdfMap() {
    	return this._idfMap;
    }

	public Map<String, Set<String>> getNetworkGeneList() {
		return _networkGeneList;
	}

	public void setNetworkGeneList(Map<String, Set<String>> _networkGeneList) {
		this._networkGeneList = _networkGeneList;
	}
}
