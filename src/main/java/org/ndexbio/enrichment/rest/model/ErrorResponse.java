/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Encapsulates an error encountered by the server
 * @author churas
 */
public class ErrorResponse {
    
    private String _errorCode;
    private String _message;
    private String _description;
    private String _stackTrace;
    private String _threadId;
    private String _timeStamp;
    
    public ErrorResponse(){
        LocalDateTime ldt = LocalDateTime.now(ZoneId.of("UTC"));
        _timeStamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm.s",
                                                 Locale.ENGLISH).format(ldt);
    }
    
    public ErrorResponse(final String message, Exception ex){
        this();
        _message = message;
        _description = ex.getMessage();
        StringBuilder stackTraceStr = new StringBuilder();
        int counter = 0;
        for (StackTraceElement ste : ex.getStackTrace()){
            stackTraceStr.append(ste.toString());
            if (counter >= 2){
                break;
            }
            counter++;
        }
        _stackTrace = stackTraceStr.toString();
        _threadId = Long.toString(Thread.currentThread().getId());
    }

    private String getValueAsJsonString(final String value){
        if (value == null){
            return "null";
        }
        return "\"" + value + "\"";
    }
    /**
     * Fallback implementation of json version of object
     * {"message":"hi",
     *  "stackTrace":"org.ndexbio.enri",
     *  "threadId":"1",
     *  "description":"well",
     *  "errorCode":null,
     *  "timeStamp":null}
     * @return 
     */
    public String asJson(){
        StringBuilder sb = new StringBuilder();
        sb.append("{\"message\": ");
        sb.append(this.getValueAsJsonString(getMessage()));
        sb.append(",\n");
        
        sb.append("\"stackTrace\": ");
        sb.append(this.getValueAsJsonString(getStackTrace()));
        sb.append(",\n");

        sb.append("\"threadId\": ");
        sb.append(this.getValueAsJsonString(getThreadId()));
        sb.append(",\n");

        sb.append("\"description\": ");
        sb.append(this.getValueAsJsonString(getDescription()));
        sb.append(",\n");

        sb.append("\"errorCode\": ");
        sb.append(this.getValueAsJsonString(getErrorCode()));
        sb.append(",\n");
        
        sb.append("\"timeStamp\": ");
        sb.append(this.getValueAsJsonString(getTimeStamp()));
        sb.append("}");
        return sb.toString();
    }

    @Schema(description="Error code to help identify issue")
    public String getErrorCode() {
        return _errorCode;
    }

    public void setErrorCode(String _errorCode) {
        this._errorCode = _errorCode;
    }

    @Schema(description="Human readable description of error")
    public String getMessage() {
        return _message;
    }

    public void setMessage(String _message) {
        this._message = _message;
    }

    @Schema(description="More detailed description of error")
    public String getDescription() {
        return _description;
    }

    public void setDescription(String _description) {
        this._description = _description;
    }

    @Schema(description="Stack trace of error")
    public String getStackTrace() {
        return _stackTrace;
    }

    public void setStackTrace(String _stackTrace) {
        this._stackTrace = _stackTrace;
    }

    @Schema(description="Id of thread running process")
    public String getThreadId() {
        return _threadId;
    }

    public void setThreadId(String _threadId) {
        this._threadId = _threadId;
    }

    @Schema(description="UTC Time stamp in YYYY-MM-DD_HH:MM.S")
    public String getTimeStamp() {
        return _timeStamp;
    }

    public void setTimeStamp(String _timeStamp) {
        this._timeStamp = _timeStamp;
    }
    
}
