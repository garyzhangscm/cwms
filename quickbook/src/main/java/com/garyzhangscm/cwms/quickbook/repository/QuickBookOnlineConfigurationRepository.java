package com.garyzhangscm.cwms.quickbook.repository;

import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineConfiguration;
import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineToken;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

/**
 * @author dderose
 *
 */
public interface QuickBookOnlineConfigurationRepository extends CrudRepository<QuickBookOnlineConfiguration, Long> , JpaSpecificationExecutor<QuickBookOnlineConfiguration> {


    QuickBookOnlineConfiguration findByWarehouseId(Long warehouseId);
}
