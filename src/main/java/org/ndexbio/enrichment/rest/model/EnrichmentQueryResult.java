package org.ndexbio.enrichment.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;

/**
 *
 * @author churas
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnrichmentQueryResult {
    private String _networkUUID;
    private String _databaseUUID;
    private String _databaseName;
    private String _description;
    private String _url;
    private String _imageURL;
    private int _percentOverlap;
    private int _nodes;
    private int _edges;
    private double _pValue;
    private double _similarity;
    private int _rank;
    private Set<String> _hitGenes;
    private int _totalNetworkCount;
    private int _totalGeneCount;


    public String getNetworkUUID() {
        return _networkUUID;
    }

    public void setNetworkUUID(String _networkUUID) {
        this._networkUUID = _networkUUID;
    }

    public String getDatabaseUUID() {
        return _databaseUUID;
    }

    public void setDatabaseUUID(String _databaseUUID) {
        this._databaseUUID = _databaseUUID;
    }

    public String getDatabaseName() {
        return _databaseName;
    }

    public void setDatabaseName(String _databaseName) {
        this._databaseName = _databaseName;
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String _description) {
        this._description = _description;
    }
    
    public void setUrl(String _url) {
    	this._url = _url;
    }
    
    public String getUrl() {
    	return _url;
    }

    public String getImageURL() {
        return _imageURL;
    }

    public void setImageURL(String _imageURL) {
        this._imageURL = _imageURL;
    }

    public int getPercentOverlap() {
        return _percentOverlap;
    }

    public void setPercentOverlap(int _percentOverlap) {
        this._percentOverlap = _percentOverlap;
    }

    public int getNodes() {
        return _nodes;
    }

    public void setNodes(int _nodes) {
        this._nodes = _nodes;
    }

    public int getEdges() {
        return _edges;
    }

    public void setEdges(int _edges) {
        this._edges = _edges;
    }

    public double getpValue() {
        return _pValue;
    }

    public void setpValue(double _pValue) {
        this._pValue = _pValue;
    }

    public int getRank() {
        return _rank;
    }

    public void setRank(int _rank) {
        this._rank = _rank;
    }

    public Set<String> getHitGenes() {
        return _hitGenes;
    }

    public void setHitGenes(Set<String> _hitGenes) {
        this._hitGenes = _hitGenes;
    }

    public void setSimilarity(double _similarity) {
            this._similarity = _similarity;
    }

    public double getSimilarity() {
            return _similarity;
    }

    public void setTotalNetworkCount(int _totalNetworkCount) {
            this._totalNetworkCount = _totalNetworkCount;
    }

    public int getTotalNetworkCount() {
            return _totalNetworkCount;
    }

    public int getTotalGeneCount() {
        return _totalGeneCount;
    }

    public void setTotalGeneCount(int _totalGeneCount) {
        this._totalGeneCount = _totalGeneCount;
    }
        
        
}
