package com.garyzhangscm.cwms.integration.controller;


import com.intuit.developer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.time.LocalDateTime;

@Endpoint
public class QBWebConnectorEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(QBWebConnectorEndpoint.class);

    private static final String NAMESPACE = "http://developer.intuit.com/";


    @PayloadRoot(
            namespace = "http://developer.intuit.com/",
            localPart = "authenticate")
    @ResponsePayload
    public AuthenticateResponse authenticate(@RequestPayload Authenticate authenticate) {
        logger.debug("Get authenticate request");
        logger.debug("======   Payload   =====");
        logger.debug("username: {}, password: {}",
                authenticate.getStrUserName(), authenticate.getStrPassword());
        // result sent back to the client
        // we can at maximum have 4 strings in the array
        // 1. GUID stands for the session.
        // 2.
        //   - nvu for invalid auth
        //   - none for no more request to quick book
        //   - full path of the company to get work for the company
        //   - empty string to get work for the open company
        // 3. (optional) the number of seconds to
        //     wait before the next update. You would use this to in effect tell that QBWC client
        //     not to bother you for a specified time.
        // 4. (optional) the number of seconds to
        //     be used as the MinimumRunEveryNSeconds time for your web service, which tells
        //     QBWC how frequently your web service needs to be contacted.
        ArrayOfString arrayOfString = new ArrayOfString();

        arrayOfString.getString().add("{F5FCCBC3-AA13-4d28-9DBF-3E571823F2BB}");
        arrayOfString.getString().add("");

        logger.debug("Will return {} to the customer for auth",
                arrayOfString);
        AuthenticateResponse authenticateResponse = new AuthenticateResponse();
        authenticateResponse.setAuthenticateResult(arrayOfString);
        return authenticateResponse;

    }


    @PayloadRoot(
            namespace = "http://developer.intuit.com/",
            localPart = "serverVersion")
    @ResponsePayload
    public ServerVersionResponse serverVersion(@RequestPayload ServerVersion serverVersion) {
        logger.debug("Get serverVersion request");
        logger.debug("======   Payload   =====");
        logger.debug(serverVersion.toString());
        ServerVersionResponse serverVersionResponse = new ServerVersionResponse();
        serverVersionResponse.setServerVersionResult("1.0");
        return serverVersionResponse;

    }

    /**
     * Get Quickbook web connector client version
     * @param clientVersion
     * @return
     */
    @PayloadRoot(
            namespace = "http://developer.intuit.com/",
            localPart = "clientVersion")
    @ResponsePayload
    public ClientVersionResponse clientVersion(@RequestPayload ClientVersion clientVersion) {
        logger.debug("Get clientVersion request");
        logger.debug("======   Payload   =====");
        logger.debug("client version: {}", clientVersion.getStrVersion());
        ClientVersionResponse clientVersionResponse = new ClientVersionResponse();

        return clientVersionResponse;

    }

    @PayloadRoot(
            namespace = "http://developer.intuit.com/",
            localPart = "sendRequestXML")
    @ResponsePayload
    public SendRequestXMLResponse sendRequestXML(@RequestPayload SendRequestXML sendRequestXML) {
        logger.debug("Get sendRequestXML request @ {}",
                LocalDateTime.now());
        logger.debug("======   Payload   =====");
        logger.debug("ticket: {}", sendRequestXML.getTicket());
        logger.debug("strHCPResponse: {}", sendRequestXML.getStrHCPResponse());
        logger.debug("strCompanyFileName: {}", sendRequestXML.getStrCompanyFileName());
        logger.debug("qbXMLCountry: {}", sendRequestXML.getQbXMLCountry());
        logger.debug("qbXMLMajorVers: {}", sendRequestXML.getQbXMLMajorVers());
        logger.debug("qbXMLMinorVers: {}", sendRequestXML.getQbXMLMinorVers());

        SendRequestXMLResponse sendRequestXMLResponse = new SendRequestXMLResponse();
        sendRequestXMLResponse.setSendRequestXMLResult("");
        return sendRequestXMLResponse;

    }


    @PayloadRoot(
            namespace = "http://developer.intuit.com/",
            localPart = "receiveResponseXML")
    @ResponsePayload
    public ReceiveResponseXMLResponse receiveResponseXML(@RequestPayload ReceiveResponseXML receiveResponseXML) {
        logger.debug("Get receiveResponseXML request @ {}",
                LocalDateTime.now());

        logger.debug("======   Payload   =====");
        logger.debug("ticket: {}", receiveResponseXML.getTicket());
        logger.debug("response: {}", receiveResponseXML.getResponse());
        logger.debug("hresult: {}", receiveResponseXML.getHresult());
        logger.debug("message: {}", receiveResponseXML.getMessage());

        ReceiveResponseXMLResponse receiveResponseXMLResponse = new ReceiveResponseXMLResponse();
        receiveResponseXMLResponse.setReceiveResponseXMLResult(80);
        return receiveResponseXMLResponse;

    }


}
