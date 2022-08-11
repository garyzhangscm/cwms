package com.garyzhangscm.cwms.quickbook.repository;

import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineToken;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

/**
 * @author dderose
 *
 */
public interface QuickBookOnlineTokenRepository extends CrudRepository<QuickBookOnlineToken, Long> , JpaSpecificationExecutor<QuickBookOnlineToken> {

	QuickBookOnlineToken findByRealmId(String realmId);

	QuickBookOnlineToken findByWarehouseId(Long warehouseId);
}
