/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.model;

/**
 *
 * @author churas
 */
public class EnrichmentQueryStatus {
    private String _status;
    private String _message;
    private int _progress;
    private int _wallTime;
    
        public String getStatus() {
        return _status;
    }

    public void setStatus(String _status) {
        this._status = _status;
    }

    public String getMessage() {
        return _message;
    }

    public void setMessage(String _message) {
        this._message = _message;
    }

    public int getProgress() {
        return _progress;
    }

    public void setProgress(int _progress) {
        this._progress = _progress;
    }

    public int getWallTime() {
        return _wallTime;
    }

    public void setWallTime(int _wallTime) {
        this._wallTime = _wallTime;
    }

}
