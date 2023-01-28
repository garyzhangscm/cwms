package com.garyzhangscm.cwms.quickbook.service;

import com.garyzhangscm.cwms.quickbook.exception.MissingInformationException;
import com.garyzhangscm.cwms.quickbook.exception.OAuthFailException;
import com.garyzhangscm.cwms.quickbook.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineConfiguration;
import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineToken;
import com.garyzhangscm.cwms.quickbook.repository.QuickBookOnlineTokenRepository;
import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.config.Environment;
import com.intuit.oauth2.config.OAuth2Config;
import com.intuit.oauth2.data.BearerTokenResponse;
import com.intuit.oauth2.exception.OAuthException;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class QuickBookOnlineTokenService  {

	private static final Logger logger = LoggerFactory.getLogger(QuickBookOnlineTokenService.class);
	
	@Autowired
	private QuickBookOnlineTokenRepository quickBookOnlineTokenRepository;


	// key: realmId
	// concurrent hash map as locks so as to lock the web hook call or query call
	// when we are updating the token
	private ConcurrentHashMap<String, List<QuickBookOnlineToken>> quickBookOnlineTokenConcurrentHashMap = new ConcurrentHashMap<>();

	// key: warehouse id
	// value: OAuth2Config
	private ConcurrentHashMap<Long, OAuth2Config> oAuth2ConfigConcurrentHashMap = new ConcurrentHashMap<>();


	@Autowired
	private QuickBookOnlineConfigurationService quickBookOnlineConfigurationService;
	
	@Autowired
	private SecurityService securityService;



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

	public QuickBookOnlineToken getByWarehouseId(Long warehouseId) {

		return quickBookOnlineTokenRepository.findByWarehouseId(warehouseId);
	}
	public List<QuickBookOnlineToken> getByRealmId(String realmId) {

		if (quickBookOnlineTokenConcurrentHashMap.contains(realmId)) {
			return quickBookOnlineTokenConcurrentHashMap.get(realmId);
		}
		List<QuickBookOnlineToken> quickBookOnlineTokens =
				quickBookOnlineTokenRepository.findByRealmId(realmId);
		if (Objects.nonNull(quickBookOnlineTokens)) {

			quickBookOnlineTokenConcurrentHashMap.put(
					realmId,
					quickBookOnlineTokens
			);
		}
		return quickBookOnlineTokens;
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

		// refresh the map to update the cached token
		List<QuickBookOnlineToken> quickBookOnlineTokens =
				quickBookOnlineTokenConcurrentHashMap.get(
						newQuickBookOnlineToken.getRealmId()
				).stream().map(existingQuickBookOnlineToken -> {
					if (Objects.equals(newQuickBookOnlineToken.getCompanyId(), existingQuickBookOnlineToken.getCompanyId()) &&
							Objects.equals(newQuickBookOnlineToken.getWarehouseId(), existingQuickBookOnlineToken.getWarehouseId()) &&
							Objects.equals(newQuickBookOnlineToken.getRealmId(), existingQuickBookOnlineToken.getRealmId())) {
						return newQuickBookOnlineToken;
					}
					// for anything that not in saved in current transaction
					return existingQuickBookOnlineToken;
				}).collect(Collectors.toList());
		quickBookOnlineTokenConcurrentHashMap.put(
				newQuickBookOnlineToken.getRealmId(), quickBookOnlineTokens
		);
		return newQuickBookOnlineToken;
	}

	public QuickBookOnlineToken saveOrUpdate(QuickBookOnlineToken quickBookOnlineToken) {
		if (Objects.isNull(quickBookOnlineToken.getId()) &&
		        Objects.nonNull(getByWarehouseId(quickBookOnlineToken.getWarehouseId()))) {
			quickBookOnlineToken.setId(
					getByWarehouseId(quickBookOnlineToken.getWarehouseId()).getId()
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


	public OAuth2PlatformClient getOAuth2PlatformClient(Long warehouseId)  {

		return new OAuth2PlatformClient(getOAuth2Config(warehouseId));
	}

	public OAuth2Config getOAuth2Config(Long warehouseId)  {

		if (oAuth2ConfigConcurrentHashMap.contains(warehouseId)) {
			return oAuth2ConfigConcurrentHashMap.get(warehouseId);
		}
		QuickBookOnlineConfiguration quickBookOnlineConfiguration =
				quickBookOnlineConfigurationService.findByWarehouseId(warehouseId);
		if (Objects.isNull(quickBookOnlineConfiguration)) {
			throw ResourceNotFoundException.raiseException("quickbook is not setup yet." +
					"please setup the client id and client secret first");
		}

		logger.debug("setup oauth2 configuration for warehouse {}, by client id {}, client secret {}",
				warehouseId,
				quickBookOnlineConfiguration.getClientId(),
				quickBookOnlineConfiguration.getClientSecret());
		OAuth2Config oauth2Config = new OAuth2Config.OAuth2ConfigBuilder(
				quickBookOnlineConfiguration.getClientId(),
				quickBookOnlineConfiguration.getClientSecret()) //set client id, secret
				.callDiscoveryAPI(Environment.PRODUCTION) // call discovery API to populate urls
				.buildConfig();
		oAuth2ConfigConcurrentHashMap.put(warehouseId, oauth2Config);

		return oauth2Config;
	}

    public QuickBookOnlineToken requestToken(Long companyId,
							   Long warehouseId,
							   String authCode, String realmId) throws OAuthException {
		logger.info("authQuickBook with auto code {} , realmId {}",
				authCode, realmId);

		// see if we already have the token information saved for this realmId
		List<QuickBookOnlineToken> quickBookOnlineTokens = getByRealmId(realmId);

		OAuth2Config oauth2Config = getOAuth2Config(warehouseId);

		OAuth2PlatformClient client  = new OAuth2PlatformClient(oauth2Config);

		//Get the bearer token (OAuth2 tokens)
		try {
			BearerTokenResponse bearerTokenResponse;
			if (quickBookOnlineTokens.size() > 0) {
				// ok, we already have existing token record for this realmid.
				// let's just refresh the token for every record and
				// save / update for this warehouse as well

				bearerTokenResponse = client.refreshToken(
						quickBookOnlineTokens.get(0).getRefreshToken()
				);

			}
			else {

				bearerTokenResponse = client.retrieveBearerTokens(
						authCode,
						"https://developer.intuit.com/v2/OAuth2Playground/RedirectUrl");
			}

			//retrieve the token using the variables below
			logger.debug("access token: {}", bearerTokenResponse.getAccessToken());
			logger.debug("refresh token: {}", bearerTokenResponse.getRefreshToken());
			logger.debug("token expired in : {}", bearerTokenResponse.getExpiresIn());
			logger.debug("refresh token expired in : {}", bearerTokenResponse.getXRefreshTokenExpiresIn());

			// see if the token for current warehouse already exists, if not, we
			// will create one

			QuickBookOnlineToken existingQuickBookOnlineToken = null;
			// loop through the existing token record with the same realmid and update
			// them accordingly, regardless of whether the record belongs to the same
			//  compamny or warehouse
			// the token may be for other company or warehouse
			// 1. a test environment for the same company but using a different company code
			// 2. another warehouse that connect to the same quickbook account
			// then we will update the token as well as long as the 2 share the same
			// realmid
			for (QuickBookOnlineToken quickBookOnlineToken : quickBookOnlineTokens) {

				quickBookOnlineToken.setAuthorizationCode(authCode);
				quickBookOnlineToken.setToken(bearerTokenResponse.getAccessToken());
				quickBookOnlineToken.setRefreshToken(bearerTokenResponse.getRefreshToken());
				quickBookOnlineToken.setLastTokenRequestTime(ZonedDateTime.now(ZoneOffset.UTC));

				if (Objects.equals(companyId, quickBookOnlineToken.getCompanyId()) &&
								Objects.equals(warehouseId, quickBookOnlineToken.getWarehouseId()) &&
								Objects.equals(realmId, quickBookOnlineToken.getRealmId())) {
					// the token for current warehouse already exists, we will
					// update it and return
					existingQuickBookOnlineToken = saveOrUpdate(quickBookOnlineToken);
				}
				else {
					saveOrUpdate(quickBookOnlineToken);
				}
			}

			if (Objects.isNull(existingQuickBookOnlineToken)) {
				// if we are here, then this is the first time we request a token for this
				// warehouse / company, let's save it
				existingQuickBookOnlineToken =
				   saveOrUpdate(new QuickBookOnlineToken(companyId, warehouseId,
						  realmId, authCode, bearerTokenResponse.getAccessToken(),
						  bearerTokenResponse.getRefreshToken(), ZonedDateTime.now(ZoneOffset.UTC))
				   );
			}
			return existingQuickBookOnlineToken;
/**
			String jsonString = new JSONObject()
					.put("access_token", bearerTokenResponse.getAccessToken())
					.put("refresh_token", bearerTokenResponse.getRefreshToken()).toString();

			return jsonString;
 **/
		}
		catch (OAuthException exception) {
			exception.printStackTrace();
			throw OAuthFailException.raiseException(exception.getMessage());
		}
    }

	public QuickBookOnlineToken requestRefreshToken(Long companyId,
									  Long warehouseId,
									  String realmId) throws OAuthException {



		List<QuickBookOnlineToken> quickBookOnlineTokens = getByRealmId(realmId);
		if (Objects.isNull(quickBookOnlineTokens) || quickBookOnlineTokens.size() == 0) {
			throw MissingInformationException.raiseException("Can't find token by realm ID " +
					realmId + ", please initiate the token first");
		}
		OAuth2Config oauth2Config = getOAuth2Config(warehouseId);

		OAuth2PlatformClient client  = new OAuth2PlatformClient(oauth2Config);

		// we will only refresh the token once but update all the
		// record that has the same realmid
		QuickBookOnlineToken quickBookOnlineToken = quickBookOnlineTokens.get(0);

		BearerTokenResponse bearerTokenResponse = client.refreshToken(
				quickBookOnlineToken.getRefreshToken()
		);

		logger.debug("access token: {}", bearerTokenResponse.getAccessToken());
		logger.debug("refresh token: {}", bearerTokenResponse.getRefreshToken());
		logger.debug("token expired in : {}", bearerTokenResponse.getExpiresIn());
		logger.debug("refresh token expired in : {}", bearerTokenResponse.getXRefreshTokenExpiresIn());

		quickBookOnlineTokens.forEach(
				existingQuickBookOnlineToken -> {

					existingQuickBookOnlineToken.setToken(bearerTokenResponse.getAccessToken());
					existingQuickBookOnlineToken.setRefreshToken(bearerTokenResponse.getRefreshToken());
					existingQuickBookOnlineToken.setLastTokenRequestTime(ZonedDateTime.now(ZoneOffset.UTC));
					saveOrUpdate(existingQuickBookOnlineToken);
				}
		);


	    return getByWarehouseId(warehouseId);
/*
		String jsonString = new JSONObject()
				.put("access_token", bearerTokenResponse.getAccessToken())
				.put("refresh_token", bearerTokenResponse.getRefreshToken()).toString();
		return jsonString;
*/
	}
}
