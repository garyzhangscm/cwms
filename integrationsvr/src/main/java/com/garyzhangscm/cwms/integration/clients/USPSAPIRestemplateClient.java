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

package com.garyzhangscm.cwms.integration.clients;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.garyzhangscm.cwms.integration.exception.RequestValidationFailException;
import com.garyzhangscm.cwms.integration.model.usps.AddressValidateResponse;
import com.garyzhangscm.cwms.integration.model.usps.Error;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Component
public class USPSAPIRestemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(USPSAPIRestemplateClient.class);


    @Autowired
    @Qualifier("getObjMapper")
    private ObjectMapper objectMapper;


    @Value("${usps.api.username:\"\"}")
    private String username;


    public AddressValidateResponse validateAddress(String addressLine1, String addressLine2, String city,
                                   String state, String zipCode)   {
        // https://secure.shippingapis.com/ShippingAPI.dll?API=Verify&XML=
        // <AddressValidateRequest USERID="658N3CLAYT857">
        //  <Revision>1</Revision>
        //  <Address ID="0">
        //    <Address1>xxxx</Address1>
        //	  <Address2>yyyy</Address2>
        //	  <City>zzz</City>
        //	  <State>CA</State>
        //	  <Zip5>90001</Zip5>
        //	  <Zip4/>
        //  </Address>
        //</AddressValidateRequest>
        String xml = getAddressValidateRequestXML(addressLine1, addressLine2, city,
                state, zipCode);
        logger.debug("Start to send xml for address validation: \n{}", xml);

        String url = "https://secure.shippingapis.com/ShippingAPI.dll?API=Verify&XML=" + xml;

        RestTemplate restTemplate = new RestTemplate();
        String response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    null,
                    String.class).getBody();

        logger.debug("get response from validateAddress:\n {}",
                     response);
        XmlMapper xmlMapper = new XmlMapper();
        try {
            AddressValidateResponse addressValidateResponse
                    = xmlMapper.readValue(response, AddressValidateResponse.class);
            // logger.debug("convert to address validation response: \n{}", addressValidateResponse);

            return validateAddress(addressLine1, addressLine2,
                    city, state, zipCode, addressValidateResponse);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw RequestValidationFailException.raiseException("error while validate address " +
                    e.getMessage());
        }
    }

    /**
     * Validate the response from USPS against the address from the user. If USPS correct the
     * address, then we will raise error to the user
     * @param addressLine1
     * @param addressLine2
     * @param city
     * @param state
     * @param zipCode
     * @param addressValidateResponse
     * @return
     */
    public AddressValidateResponse validateAddress(String addressLine1, String addressLine2, String city,
                                                   String state, String zipCode,
                                                   AddressValidateResponse addressValidateResponse)   {
        // there's error with the address, we won't need to validate it again
        if (Objects.nonNull(addressValidateResponse.getAddress().getError())) {
            return addressValidateResponse;
        }
        // we will only validate the city, state, zipcode
        String errorMessage = "";
        if (!addressValidateResponse.getAddress().getCity().equalsIgnoreCase(city)) {
            errorMessage += "city " + city + " is not correct, suggest city: " +
                    addressValidateResponse.getAddress().getCity() + "; ";
        }
        if (!addressValidateResponse.getAddress().getState().equalsIgnoreCase(state)) {
            errorMessage += "state " + state + " is not correct, suggest state: " +
                    addressValidateResponse.getAddress().getState() + "; ";
        }

        String zip5 = "";
        String zip4 = "";
        if (Strings.isNotBlank(zipCode)) {
            if(zipCode.contains("-")) {
                String[] zipCodes = zipCode.split("-");
                if (zipCodes.length != 2) {
                    // zipCode is not in the right format, let USPS
                    // verify
                    zip5 = zipCode;
                }
                else {
                    zip5 = zipCodes[0];
                    zip4 = zipCodes[1];
                }
            }
            else {
                zip5 = zipCode;
            }
        }

        if (!addressValidateResponse.getAddress().getZip5().equalsIgnoreCase(zip5)) {
            errorMessage = "zip " + zip5 + " is not correct, suggest zip: " +
                    addressValidateResponse.getAddress().getZip5() + "; ";
        }

        if (Strings.isNotBlank(zip4) &&
                !addressValidateResponse.getAddress().getZip4().equalsIgnoreCase(zip4)) {
            errorMessage = "ZIP+4 " + zip4 + " is not correct, suggest zip: " +
                    addressValidateResponse.getAddress().getZip4() + "; ";
        }

        if (Strings.isNotBlank(errorMessage)) {
            addressValidateResponse.getAddress().setError(
                    new Error(
                            "-1",
                            "USPS-Address-Correction",
                            errorMessage
                    )
            );
        }
        else {
            addressValidateResponse.getAddress().setError(null);
        }
        return addressValidateResponse;
    }

    /**
     * create the XML string for the address
     * @param addressLine1
     * @param addressLine2
     * @param city
     * @param state
     * @param zipCode
     * @return
     */
    private String getAddressValidateRequestXML(String addressLine1, String addressLine2, String city, String state, String zipCode) {
        // we allow the user to pass in xxxxx or xxxxx-yyyy as zip code
        String zip5 = "";
        String zip4 = "";
        if (Strings.isNotBlank(zipCode)) {
            if(zipCode.contains("-")) {
                String[] zipCodes = zipCode.split("-");
                if (zipCodes.length != 2) {
                    // zipCode is not in the right format, let USPS
                    // verify
                    zip5 = zipCode;
                }
                else {
                    zip5 = zipCodes[0];
                    zip4 = zipCodes[1];
                }
            }
            else {
                zip5 = zipCode;
            }
        }
        StringBuilder stringBuilder = new StringBuilder()
                .append("<AddressValidateRequest USERID=\"").append(username).append("\">")
                .append("<Revision>1</Revision>")
                .append("<Address ID=\"0\">")
                .append("<Address1>").append(addressLine1).append("</Address1>")
                .append("<Address2>").append(addressLine2).append("</Address2>")
                .append("<City>").append(city).append("</City>")
                .append("<State>").append(state).append("</State>")
                .append("<Zip5>").append(zip5).append("</Zip5>");
        if (Strings.isNotBlank(zip4)) {
            stringBuilder
                    .append("<Zip4>").append(zip4).append("</Zip4>");
        }
        else {
            stringBuilder
                    .append("<Zip4/>");
        }
        return stringBuilder.append("</Address>")
                .append("</AddressValidateRequest>")
                .toString();
    }


}
