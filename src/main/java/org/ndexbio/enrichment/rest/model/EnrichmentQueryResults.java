package org.ndexbio.enrichment.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 *
 * @author churas
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnrichmentQueryResults extends EnrichmentQueryStatus {

    private int _numberOfHits;
    private int _start;
    private int _size;
    private List<EnrichmentQueryResult> _results;

    public EnrichmentQueryResults(){
        super();
    }
    public EnrichmentQueryResults(long startTime){
        super(startTime);
    }
    
    /**
     * Copy constructor that copies the {@code eqr} data with exception of the
     * {@link #getResults()} data. That is set to value of {@code results} parameter
     * passed in.
     * @param eqr {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResults} to copy data from
     * @param results List of {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResult} 
     *        to set as results for this object
     */
    public EnrichmentQueryResults(EnrichmentQueryResults eqr, List<EnrichmentQueryResult> results){
        super(eqr);
        if (eqr != null){
            this._numberOfHits = eqr.getNumberOfHits();
            this._start = eqr.getStart();
            this._size = eqr.getSize();
        }
        _results = results;
    }
    
    public EnrichmentQueryResults updateStartTime(EnrichmentQueryResults eqs) {
        super.updateStartTime(eqs);
        return this;
    }

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
