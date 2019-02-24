/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.model;

import java.util.List;

/**
 *
 * @author churas
 */
public class EnrichmentQueryResult {
    private String _networkUUID;
    private String _databaseUUID;
    private String _databaseName;
    private String _description;
    private int _percentOverlap;
    private int _nodes;
    private int _edges;
    private double _pValue;
    private int _rank;
    private List<String> _hitGenes;


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

    public List<String> getHitGenes() {
        return _hitGenes;
    }

    public void setHitGenes(List<String> _hitGenes) {
        this._hitGenes = _hitGenes;
    }
}
