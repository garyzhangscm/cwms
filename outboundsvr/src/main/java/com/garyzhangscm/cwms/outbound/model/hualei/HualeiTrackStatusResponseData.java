/**
 * Copyright 2019
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

package com.garyzhangscm.cwms.outbound.model.hualei;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

//[{"data":[{"businessId":"","businessStatus":"","business_channeltransfercode":"","business_customerweightf":"","business_grossweight":"","business_pieces":"1","business_seqinvoicecode":"","business_serveweightf":"","childrenTrackDetails":null,"consigneeCountry":"US","consignee_address":"","consignee_name":"","customer_code":"EULOGIA","file_path":"","flight_number":"","order_customnote":"","productKindName":"XZH-USPS海外仓","referenceNumber":"EULOGIA0000000100","shipper_name":"","time_zone":"","trackContent":"货物电子信息已经收到","trackDate":"2023-05-10
//13:18:43","trackDetails":[{"business_id":"","country_code":"","flight_number":"","is_weic":"","system_id":"","time_zone":"","track_city":"","track_content":"货物电子信息已经收到","track_createdate":"","track_date":"2023-05-10
//13:18:43","track_id":"","track_kind":"","track_location":"","track_signdate":"","track_signperson":"","track_state":"","track_substate":"","track_zippost":""}],"trackLocation":"","trackSignperson":"","trackingNumber":"9405509202043044222210"}],"ack":"true"}]

public class HualeiTrackStatusResponseData {
    private String trackingNumber;
    private String referenceNumber;
    private String trackContent;

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getTrackContent() {
        return trackContent;
    }

    public void setTrackContent(String trackContent) {
        this.trackContent = trackContent;
    }
}
