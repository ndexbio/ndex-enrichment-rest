package org.ndexbio.enrichment.rest.model;

/**
 *
 * @author churas
 */
public class InternalNdexConnectionParams {
    
    private String _user;
    private String _password;
    private String _server;
    private String _networkSetId;

    public String getUser() {
        return _user;
    }

    public void setUser(String _user) {
        this._user = _user;
    }

    public String getPassword() {
        return _password;
    }

    public void setPassword(String _password) {
        this._password = _password;
    }

    public String getServer() {
        return _server;
    }

    public void setServer(String _server) {
        this._server = _server;
    }

    public String getNetworkSetId() {
        return _networkSetId;
    }

    public void setNetworkSetId(String _networkSetId) {
        this._networkSetId = _networkSetId;
    }
}
