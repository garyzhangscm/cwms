package com.garyzhangscm.cwms.quickbook.service;

import com.garyzhangscm.cwms.quickbook.exception.MissingInformationException;
import com.garyzhangscm.cwms.quickbook.exception.OAuthFailException;
import com.garyzhangscm.cwms.quickbook.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineConfiguration;
import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineToken;
import com.garyzhangscm.cwms.quickbook.repository.QuickBookOnlineConfigurationRepository;
import com.garyzhangscm.cwms.quickbook.repository.QuickBookOnlineTokenRepository;
import com.intuit.ipp.util.Logger;
import com.intuit.oauth2.client.OAuth2PlatformClient;
import com.intuit.oauth2.config.Environment;
import com.intuit.oauth2.config.OAuth2Config;
import com.intuit.oauth2.data.BearerTokenResponse;
import com.intuit.oauth2.exception.OAuthException;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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
public class QuickBookOnlineConfigurationService {
	
	private static final org.slf4j.Logger logger = Logger.getLogger();
	
	@Autowired
	private QuickBookOnlineConfigurationRepository quickBookOnlineConfigurationRepository;


	public QuickBookOnlineConfiguration findById(Long id) {
		return quickBookOnlineConfigurationRepository.findById(id)
				.orElseThrow(() -> ResourceNotFoundException.raiseException("quick book online configuration not found by id: " + id));
	}

	public QuickBookOnlineConfiguration findByWarehouseId(Long warehouseId) {
		return quickBookOnlineConfigurationRepository.findByWarehouseId(warehouseId);
	}

	public QuickBookOnlineConfiguration save(QuickBookOnlineConfiguration quickBookOnlineConfiguration) {


		return quickBookOnlineConfigurationRepository.save(quickBookOnlineConfiguration);
	}

	public QuickBookOnlineConfiguration saveOrUpdate(QuickBookOnlineConfiguration quickBookOnlineConfiguration) {
		if (Objects.isNull(quickBookOnlineConfiguration.getId()) &&
				Objects.nonNull(findByWarehouseId(quickBookOnlineConfiguration.getWarehouseId()))) {
			quickBookOnlineConfiguration.setId(
					findByWarehouseId(quickBookOnlineConfiguration.getWarehouseId()).getId()
			);
		}
		return save(quickBookOnlineConfiguration);
	}


	public QuickBookOnlineConfiguration saveConfiguration(Long companyId, Long warehouseId, QuickBookOnlineConfiguration quickBookOnlineConfiguration) {

		return saveOrUpdate(quickBookOnlineConfiguration);
    }
}
