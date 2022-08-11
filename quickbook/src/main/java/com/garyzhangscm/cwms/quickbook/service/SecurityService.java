package com.garyzhangscm.cwms.quickbook.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garyzhangscm.cwms.quickbook.controller.QuickBookOnlineTokenController;
import com.garyzhangscm.cwms.quickbook.controller.WebhooksController;
import com.garyzhangscm.cwms.quickbook.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineConfiguration;
import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineToken;
import com.garyzhangscm.cwms.quickbook.model.QuickbookWebhookPayload;
import com.garyzhangscm.cwms.quickbook.service.qbo.WebhooksServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.xml.bind.DatatypeConverter;

import org.springframework.core.env.Environment;
import javax.annotation.PostConstruct;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.util.Objects;

import com.intuit.ipp.services.WebhooksService;
import com.intuit.ipp.util.Config;

@Service
@Configuration
public class SecurityService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);

    private static final String VERIFIER_KEY = "quickbook.webhooks.verifier.token";
    private static final String ENCRYPTION_KEY = "encryption.key";

    @Autowired
    Environment env;
    @Autowired
    WebhooksServiceFactory webhooksServiceFactory;
    @Autowired
    private QuickBookOnlineTokenService quickBookOnlineTokenService;
    @Autowired
    private QuickBookOnlineConfigurationService quickBookOnlineConfigurationService;

    private SecretKeySpec secretKey;

    @Qualifier("getObjMapper")
    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        try {
            secretKey = new SecretKeySpec(getEncryptionKey().getBytes("UTF-8"), "AES");
        } catch (UnsupportedEncodingException ex) {
            logger.error("Error during initializing secretkeyspec ", ex.getCause());
        }
    }

    /**
     * Validates the payload with the intuit-signature hash
     *
     * @param signature
     * @param payload
     * @return
     */
    public boolean isRequestValid(String signature, String payload) throws JsonProcessingException {

        // set custom config

        // convert the payload to get the realmid
        // we will get the warehouse id from realmid and then
        // get the configuration from warehouse id so that we can get
        // the verifier key
        QuickbookWebhookPayload webhookPayload =
                objectMapper.readValue(payload, QuickbookWebhookPayload.class);
        // we should only have one realmid per message
        if (webhookPayload.getEventNotifications().isEmpty()) {
            logger.debug("the event notification is empty, which means there's nothing in the payload");
            return true;
        }
        String realmId = webhookPayload.getEventNotifications().get(0).getRealmId();
        String verifierKey = getVerifierKey(realmId);
        logger.debug("start to verify the payload by key {}",
                verifierKey);

        Config.setProperty(Config.WEBHOOKS_VERIFIER_TOKEN, verifierKey);
        logger.debug("get webhook verify key: {}",
                Config.getProperty(Config.WEBHOOKS_VERIFIER_TOKEN));

        // create webhooks service
        WebhooksService service = webhooksServiceFactory.getWebhooksService();
        return service.verifyPayload(signature, payload);
    }

    /**
     * Verified key to validate webhooks payload
     * @return
     */
    public String getVerifierKey(String realmId) {

        // return env.getProperty(VERIFIER_KEY);
        // return "3c9ea2d4-bdc2-464d-ad44-aa12e412f694";
        QuickBookOnlineToken quickBookOnlineToken =
                quickBookOnlineTokenService.getByRealmId(realmId);
        if (Objects.isNull(quickBookOnlineToken)) {
            logger.debug("can't get token from realmid, which should not be the case. Fatal error");
            throw ResourceNotFoundException.raiseException("can't find oauth 2 token by realm id " + realmId);
        }
        QuickBookOnlineConfiguration quickBookOnlineConfiguration =
                quickBookOnlineConfigurationService.findByWarehouseId(
                        quickBookOnlineToken.getWarehouseId()
                );


        if (Objects.isNull(quickBookOnlineConfiguration)) {
            logger.debug("can't find quickbook online configuration for warehouse {},  Fatal error",
                    quickBookOnlineConfiguration.getWarehouseId());
            throw ResourceNotFoundException.raiseException("can't find quickbook configuration by warehouse " +
                    quickBookOnlineConfiguration.getWarehouseId());
        }
        return quickBookOnlineConfiguration.getWebhookVerifierToken();

    }

    /**
     * Encryption key
     *
     * @return
     */
    public String getEncryptionKey() {
        // return env.getProperty(ENCRYPTION_KEY);
        return "0123456789abcdef";
    }

    /**
     * @param plainText
     * @return
     * @throws Exception
     */
    public String encrypt(String plainText) throws Exception {
        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] byteCipherText = aesCipher.doFinal(plainText.getBytes());
        return bytesToHex(byteCipherText);
    }

    /**
     * @param byteCipherText
     * @return
     * @throws Exception
     */
    public String decrypt(String byteCipherText) throws Exception {
        Cipher aesCipher = Cipher.getInstance("AES");
        aesCipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] bytePlainText = aesCipher.doFinal(hexToBytes(byteCipherText));
        return new String(bytePlainText);

    }

    private String bytesToHex(byte[] hash) {
        return DatatypeConverter.printHexBinary(hash);
    }

    private byte[] hexToBytes(String hash) {
        return DatatypeConverter.parseHexBinary(hash);
    }
}
