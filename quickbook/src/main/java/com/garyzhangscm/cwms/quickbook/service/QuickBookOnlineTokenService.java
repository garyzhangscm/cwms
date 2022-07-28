package com.garyzhangscm.cwms.quickbook.service;

import com.garyzhangscm.cwms.quickbook.exception.MissingInformationException;
import com.garyzhangscm.cwms.quickbook.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineToken;
import com.garyzhangscm.cwms.quickbook.repository.QuickBookOnlineTokenRepository;
import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.config.Environment;
import com.intuit.oauth2.config.OAuth2Config;
import com.intuit.oauth2.data.BearerTokenResponse;
import com.intuit.oauth2.exception.OAuthException;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.intuit.ipp.util.Logger;

import javax.annotation.PostConstruct;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QuickBookOnlineTokenService  {
	
	private static final org.slf4j.Logger logger = Logger.getLogger();
	
	@Autowired
	private QuickBookOnlineTokenRepository quickBookOnlineTokenRepository;

	OAuth2PlatformClient client;
	OAuth2Config oauth2Config;

	// key: realmId
	// concurrent hash map as locks so as to lock the web hook call or query call
	// when we are updating the token
	private ConcurrentHashMap<String, QuickBookOnlineToken> quickBookOnlineTokenConcurrentHashMap = new ConcurrentHashMap<>();

	
	@Autowired
	private SecurityService securityService;

	private String clientId = "ABDFP7IYKkABCJ3L29pfNGwYfCgHtddfXswzNq3NeTCvQI6Dfz";
	private String clientSecret = "GjGWB4tqIy490VSUqt4YL9WWRVmrdHKofr0f8uOz";

	@PostConstruct
	public void init() {
		// intitialize a single thread executor, this will ensure only one thread processes the queue
		oauth2Config = new OAuth2Config.OAuth2ConfigBuilder(
				clientId,
				clientSecret) //set client id, secret
				.callDiscoveryAPI(Environment.SANDBOX) // call discovery API to populate urls
				.buildConfig();
		client  = new OAuth2PlatformClient(oauth2Config);
	}


	public QuickBookOnlineToken findById(Long id) {
		QuickBookOnlineToken quickBookOnlineToken = quickBookOnlineTokenRepository.findById(id)
				.orElseThrow(() -> ResourceNotFoundException.raiseException("quick book online token not found by id: " + id));
		return quickBookOnlineToken;
	}


	public List<QuickBookOnlineToken> findAll(Long warehouseId,
											  String realmId) {

		return quickBookOnlineTokenRepository.findAll(
				(Root<QuickBookOnlineToken> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
					List<Predicate> predicates = new ArrayList<Predicate>();

					if (Objects.nonNull(warehouseId)) {

						predicates.add(criteriaBuilder.equal(root.get("warehouseId"), warehouseId));
					}


					if (Strings.isNotBlank(realmId)) {

						predicates.add(criteriaBuilder.equal(root.get("realmId"), realmId));
					}


					Predicate[] p = new Predicate[predicates.size()];
					return criteriaBuilder.and(predicates.toArray(p));
				});
	}

	public QuickBookOnlineToken getByRealmId(String realmId) {

		if (quickBookOnlineTokenConcurrentHashMap.contains(realmId)) {
			return quickBookOnlineTokenConcurrentHashMap.get(realmId);
		}
		QuickBookOnlineToken quickBookOnlineToken =
				quickBookOnlineTokenRepository.findByRealmId(realmId);
		quickBookOnlineTokenConcurrentHashMap.put(
				quickBookOnlineToken.getRealmId(),
				quickBookOnlineToken
		);
		return quickBookOnlineToken;
		/*
		try {
			quickBookOnlineToken.set(decrypt(quickBookOnlineToken.getAccessToken()));
			quickBookOnlineToken.setAccessTokenSecret(decrypt(quickBookOnlineToken.getAccessTokenSecret()));
			return companyConfig;
		} catch (Exception ex) {
			LOG.error("Error loading company config" , ex.getCause());
			return null;
		}*/
		
	}

	public QuickBookOnlineToken save(QuickBookOnlineToken quickBookOnlineToken) {
		QuickBookOnlineToken newQuickBookOnlineToken =
				quickBookOnlineTokenRepository.save(quickBookOnlineToken);
		quickBookOnlineTokenConcurrentHashMap.put(
				newQuickBookOnlineToken.getRealmId(), newQuickBookOnlineToken
		);
		return newQuickBookOnlineToken;
	}

	public QuickBookOnlineToken saveOrUpdate(QuickBookOnlineToken quickBookOnlineToken) {
		if (Objects.isNull(quickBookOnlineToken.getId()) &&
		        Objects.nonNull(getByRealmId(quickBookOnlineToken.getRealmId()))) {
			quickBookOnlineToken.setId(
					getByRealmId(quickBookOnlineToken.getRealmId()).getId()
			);
		}
		return save(quickBookOnlineToken);
	}
	
	public String decrypt(String string) {
		try {
			return securityService.decrypt(string);
		} catch (Exception ex) {
			logger.error("Error decrypting" , ex.getCause());
			return null;
		}
	}
	
	public String encrypt(String string) {
		try {
			return securityService.encrypt(string);
		} catch (Exception ex) {
			logger.error("Error encrypting" , ex.getCause());
			return null;
		}
	}


	public OAuth2PlatformClient getOAuth2PlatformClient()  {
		return client;
	}

	public OAuth2Config getOAuth2Config()  {
		return oauth2Config;
	}

    public String requestToken(Long companyId,
							   Long warehouseId,
							   String authCode, String realmId) throws OAuthException {
		logger.info("authQuickBook with auto code {} , realmId {}",
				authCode, realmId);
		OAuth2Config oauth2Config = getOAuth2Config();

		OAuth2PlatformClient client  = new OAuth2PlatformClient(oauth2Config);

		//Get the bearer token (OAuth2 tokens)
		BearerTokenResponse bearerTokenResponse = client.retrieveBearerTokens(
				authCode,
				"https://developer.intuit.com/v2/OAuth2Playground/RedirectUrl");

		//retrieve the token using the variables below
		logger.debug("access token: {}", bearerTokenResponse.getAccessToken());
		logger.debug("refresh token: {}", bearerTokenResponse.getRefreshToken());
		logger.debug("token expired in : {}", bearerTokenResponse.getExpiresIn());
		logger.debug("refresh token expired in : {}", bearerTokenResponse.getXRefreshTokenExpiresIn());

		// see if we already have the token information saved for this realmId
		QuickBookOnlineToken quickBookOnlineToken = getByRealmId(realmId);
		if (Objects.isNull(quickBookOnlineToken)) {
			quickBookOnlineToken = new QuickBookOnlineToken();
			quickBookOnlineToken.setCompanyId(companyId);
			quickBookOnlineToken.setWarehouseId(warehouseId);
			quickBookOnlineToken.setRealmId(realmId);
			quickBookOnlineToken.setAuthorizationCode(authCode);
			quickBookOnlineToken.setToken(bearerTokenResponse.getAccessToken());
			quickBookOnlineToken.setRefreshToken(bearerTokenResponse.getRefreshToken());
			quickBookOnlineToken.setLastTokenRequestTime(LocalDateTime.now());
		}
		else {
			quickBookOnlineToken.setAuthorizationCode(authCode);
			quickBookOnlineToken.setToken(bearerTokenResponse.getAccessToken());
			quickBookOnlineToken.setRefreshToken(bearerTokenResponse.getRefreshToken());
			quickBookOnlineToken.setLastTokenRequestTime(LocalDateTime.now());
		}
		saveOrUpdate(quickBookOnlineToken);

		String jsonString = new JSONObject()
				.put("access_token", bearerTokenResponse.getAccessToken())
				.put("refresh_token", bearerTokenResponse.getRefreshToken()).toString();

		return jsonString;
    }

	public String requestRefreshToken(Long companyId,
									  Long warehouseId,
									  String realmId) throws OAuthException {



		QuickBookOnlineToken quickBookOnlineToken = getByRealmId(realmId);
		if (Objects.isNull(quickBookOnlineToken)) {
			throw MissingInformationException.raiseException("Can't find token by realm ID " +
					realmId + ", please initiate the token first");
		}
		OAuth2Config oauth2Config = getOAuth2Config();

		OAuth2PlatformClient client  = new OAuth2PlatformClient(oauth2Config);

		BearerTokenResponse bearerTokenResponse = client.refreshToken(
				quickBookOnlineToken.getRefreshToken()
		);

		logger.debug("access token: {}", bearerTokenResponse.getAccessToken());
		logger.debug("refresh token: {}", bearerTokenResponse.getRefreshToken());
		logger.debug("token expired in : {}", bearerTokenResponse.getExpiresIn());
		logger.debug("refresh token expired in : {}", bearerTokenResponse.getXRefreshTokenExpiresIn());

		quickBookOnlineToken.setToken(bearerTokenResponse.getAccessToken());
		quickBookOnlineToken.setRefreshToken(bearerTokenResponse.getRefreshToken());
		quickBookOnlineToken.setLastTokenRequestTime(LocalDateTime.now());
	    saveOrUpdate(quickBookOnlineToken);

		String jsonString = new JSONObject()
				.put("access_token", bearerTokenResponse.getAccessToken())
				.put("refresh_token", bearerTokenResponse.getRefreshToken()).toString();
		return jsonString;

	}
}
