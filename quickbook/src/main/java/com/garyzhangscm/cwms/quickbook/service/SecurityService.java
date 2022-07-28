package com.garyzhangscm.cwms.quickbook.service;


import com.garyzhangscm.cwms.quickbook.controller.QuickBookOnlineTokenController;
import com.garyzhangscm.cwms.quickbook.service.qbo.WebhooksServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.xml.bind.DatatypeConverter;

import org.springframework.core.env.Environment;
import javax.annotation.PostConstruct;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import com.intuit.ipp.services.WebhooksService;
import com.intuit.ipp.util.Config;

@Service
@Configuration
public class SecurityService {

    private static final Logger logger = LoggerFactory.getLogger(QuickBookOnlineTokenController.class);

    private static final String VERIFIER_KEY = "webhooks.verifier.token";
    private static final String ENCRYPTION_KEY = "encryption.key";

    @Autowired
    Environment env;
    @Autowired
    WebhooksServiceFactory webhooksServiceFactory;

    private SecretKeySpec secretKey;

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
    public boolean isRequestValid(String signature, String payload) {

        // set custom config
        Config.setProperty(Config.WEBHOOKS_VERIFIER_TOKEN, getVerifierKey());

        // create webhooks service
        WebhooksService service = webhooksServiceFactory.getWebhooksService();
        return service.verifyPayload(signature, payload);
    }

    /**
     * Verified key to validate webhooks payload
     * @return
     */
    public String getVerifierKey() {

        //return env.getProperty(VERIFIER_KEY);
        return "3c9ea2d4-bdc2-464d-ad44-aa12e412f694";
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
