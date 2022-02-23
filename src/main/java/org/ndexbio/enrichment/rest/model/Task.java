package org.ndexbio.enrichment.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * @author churas
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Task {
    
    private String _id;
    private String _webURL;
    
    public String getId() {
        return _id;
    }

    public void setId(String _id) {
        this._id = _id;
    }

    public String getWebURL() {
        return _webURL;
    }

    public void setWebURL(String _webURL) {
        this._webURL = _webURL;
    }
}
