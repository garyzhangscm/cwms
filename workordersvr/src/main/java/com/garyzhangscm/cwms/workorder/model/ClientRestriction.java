/**
 * Copyright 2018
 *
 * @author gzhang
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyzhangscm.cwms.workorder.model;


import java.io.Serializable;

// Entity that will be persist in Common service
public class ClientRestriction extends  AuditibleEntity<String> implements Serializable {
    Boolean threePartyLogisticsFlag;
    Boolean nonClientDataAccessible;
    Boolean allClientAccess;
    String clientAccesses;

    public ClientRestriction(Boolean threePartyLogisticsFlag, Boolean nonClientDataAccessible,
                             Boolean allClientAccess, String clientAccesses) {
        this.threePartyLogisticsFlag = threePartyLogisticsFlag;
        this.nonClientDataAccessible = nonClientDataAccessible;
        this.allClientAccess = allClientAccess;
        this.clientAccesses = clientAccesses;
    }


    public Boolean getThreePartyLogisticsFlag() {
        return threePartyLogisticsFlag;
    }

    public void setThreePartyLogisticsFlag(Boolean threePartyLogisticsFlag) {
        this.threePartyLogisticsFlag = threePartyLogisticsFlag;
    }

    public Boolean getNonClientDataAccessible() {
        return nonClientDataAccessible;
    }

    public void setNonClientDataAccessible(Boolean nonClientDataAccessible) {
        this.nonClientDataAccessible = nonClientDataAccessible;
    }

    public String getClientAccesses() {
        return clientAccesses;
    }

    public void setClientAccesses(String clientAccesses) {
        this.clientAccesses = clientAccesses;
    }

    public Boolean getAllClientAccess() {
        return allClientAccess;
    }

    public void setAllClientAccess(Boolean allClientAccess) {
        this.allClientAccess = allClientAccess;
    }
}
