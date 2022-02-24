package org.ndexbio.enrichment.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

import org.ndexbio.ndexsearch.rest.model.DatabaseResult;

/**
 * Represents results of databases
 * @author churas
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DatabaseResults {
    
    private List<DatabaseResult> _results;

    /**
     * Default Constructor
     */
    public DatabaseResults(){
        this(null);
    }
    
    /**
     * Creates new {@link #DatabaseResults()} performing shallow copy
     * of data in {@code id}
     * @param idr {@link org.ndexbio.enrichment.rest.model.InternalDatabaseResults} object to copy from
     */
    public DatabaseResults(InternalDatabaseResults idr){
        if (idr == null){
            return;
        }
        _results = idr.getResults();
    }
    
    public List<DatabaseResult> getResults() {
        return _results;
    }

    public void setResults(List<DatabaseResult> _results) {
        this._results = _results;
    }
    
}
