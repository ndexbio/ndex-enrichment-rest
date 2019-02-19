/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.model;

import java.util.List;

/**
 * Represents results of databases
 * @author churas
 */
public class DatabaseResults {
    
    private List<DatabaseResult> _results;

    public List<DatabaseResult> getResults() {
        return _results;
    }

    public void setResults(List<DatabaseResult> _results) {
        this._results = _results;
    }
    
}
