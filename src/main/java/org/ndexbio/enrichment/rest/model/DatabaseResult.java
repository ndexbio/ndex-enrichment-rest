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
public class DatabaseResult {
    private String _uuid;
    private String _description;
    private String _name;
    private String _numberOfNetworks;

    public String getUuid() {
        return _uuid;
    }

    public void setUuid(String _uuid) {
        this._uuid = _uuid;
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String _description) {
        this._description = _description;
    }

    public String getName() {
        return _name;
    }

    public void setName(String _name) {
        this._name = _name;
    }

    public String getNumberOfNetworks() {
        return _numberOfNetworks;
    }

    public void setNumberOfNetworks(String _numberOfNetworks) {
        this._numberOfNetworks = _numberOfNetworks;
    }
    
}
