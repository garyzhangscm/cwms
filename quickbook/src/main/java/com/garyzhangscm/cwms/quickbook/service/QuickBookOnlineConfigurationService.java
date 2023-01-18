package com.garyzhangscm.cwms.quickbook.service;

import com.garyzhangscm.cwms.quickbook.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineConfiguration;
import com.garyzhangscm.cwms.quickbook.repository.QuickBookOnlineConfigurationRepository;
import com.intuit.ipp.util.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

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
