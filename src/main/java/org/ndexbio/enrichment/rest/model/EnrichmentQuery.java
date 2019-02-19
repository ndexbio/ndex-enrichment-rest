/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.model;

import java.util.List;

/**
 * Represents an Enrichment Query
 * @author churas
 */
public class EnrichmentQuery {
    
    private List<String> _geneList;
    private List<String> _databaseList;

    public List<String> getGeneList() {
        return _geneList;
    }

    public void setGeneList(List<String> _geneList) {
        this._geneList = _geneList;
    }

    public List<String> getDatabaseList() {
        return _databaseList;
    }

    public void setDatabaseList(List<String> _databaseList) {
        this._databaseList = _databaseList;
    }
}
