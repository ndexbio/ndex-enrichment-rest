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
public class EnrichmentQueryResults extends EnrichmentQueryStatus {

    private int _numberOfHits;
    private int _start;
    private int _size;
    private List<EnrichmentQueryResult> _results;

    public int getNumberOfHits() {
        return _numberOfHits;
    }

    public void setNumberOfHits(int _numberOfHits) {
        this._numberOfHits = _numberOfHits;
    }

    public int getStart() {
        return _start;
    }

    public void setStart(int _start) {
        this._start = _start;
    }

    public int getSize() {
        return _size;
    }

    public void setSize(int _size) {
        this._size = _size;
    }

    public List<EnrichmentQueryResult> getResults() {
        return _results;
    }

    public void setResults(List<EnrichmentQueryResult> _results) {
        this._results = _results;
    }
    
}
