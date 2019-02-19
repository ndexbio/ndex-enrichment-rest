/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.ndexbio.enrichment.rest.model;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents status of the server
 * @author churas
 */
public class ServerStatus {
    
    
    public static final String OK_STATUS = "ok";
    public static final String ERROR_STATUS = "error";
    
    private String _status;
    private int _pcDiskFull;
    private double _systemLoad;
    private String _restVersion;

    public ServerStatus(){
    }

    /**
     * Gets status of server which can be either {@link #OK_STATUS} or
     * {@link #ERROR_STATUS}
     * @return Status as a string
     */
    @Schema(description="Status of server", allowableValues={ServerStatus.OK_STATUS,
                                                             ServerStatus.ERROR_STATUS})
    public String getStatus() {
        return _status;
    }

    @Schema(description="Gets load of server")
    public double getSystemLoad() {
        return _systemLoad;
    }

    public void setSystemLoad(double _systemLoad) {
        this._systemLoad = _systemLoad;
    }

    public void setStatus(String _status) {
        this._status = _status;
    }

    @Schema(description="Gets how full disk is as a percentage 0 - 100")
    public int getPcDiskFull() {
        return _pcDiskFull;
    }

    public void setPcDiskFull(int _pcDiskFull) {
        this._pcDiskFull = _pcDiskFull;
    }

    @Schema(description="Gets version of this service")
    public String getRestVersion() {
        return _restVersion;
    }

    public void setRestVersion(String _restVersion) {
        this._restVersion = _restVersion;
    }    
}
