package org.ndexbio.enrichment.rest.model;

/**
 *
 * @author churas
 */
public class EnrichmentQueryStatus {
    
    public static final String SUBMITTED_STATUS = "submitted";
    public static final String PROCESSING_STATUS = "processing";
    public static final String COMPLETE_STATUS = "complete";
    public static final String FAILED_STATUS = "failed";

    private String _status;
    private String _message;
    private int _progress;
    private long _wallTime;
    private long _startTime;
    
    public EnrichmentQueryStatus(){
        
    }
    
    /**
     * Creates new {@link #EnrichmentQueryStatus} object setting {@link #getStartTime() }
     * to {@code startTime} passed into this method.
     * @param startTime Current time in milliseconds, usually set with value from {@link java.lang.System.currentTimeMillis()}
     */
    public EnrichmentQueryStatus(long startTime){
        _startTime = startTime;
    }
    
    /**
     * Creates new {@link #EnrichmentQueryStatus} by copying data
     * from {@code eqr} passed in
     * @param eqr {@link org.ndexbio.enrichment.rest.model.EnrichmentQueryResults} to copy from
     */
    public EnrichmentQueryStatus(EnrichmentQueryResults eqr){
        if (eqr == null){
            return;
        }
        _status = eqr.getStatus();
        _message = eqr.getMessage();
        _progress = eqr.getProgress();
        _wallTime = eqr.getWallTime();
        _startTime = eqr.getStartTime();
    }
 
    /**
     * Updates start time with value from {@code eqs} passed in if that
     * time is greater then the time in this object.
     * @param eqs
     * @return Returns this object
     */
    public EnrichmentQueryStatus updateStartTime(EnrichmentQueryStatus eqs){
        if (eqs == null){
            return this;
        }
        if (eqs.getStartTime() > _startTime){
            _startTime = eqs.getStartTime();
        }
        return this;
    }
    
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

    public long getWallTime() {
        return _wallTime;
    }

    public void setWallTime(long _wallTime) {
        this._wallTime = _wallTime;
    }

    public long getStartTime() {
        return _startTime;
    }

    public void setStartTime(long _startTime) {
        this._startTime = _startTime;
    }
}
