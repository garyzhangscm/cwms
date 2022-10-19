package com.garyzhangscm.cwms.quickbook.repository;

import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineToken;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * @author dderose
 *
 */
public interface QuickBookOnlineTokenRepository extends CrudRepository<QuickBookOnlineToken, Long> , JpaSpecificationExecutor<QuickBookOnlineToken> {

	List<QuickBookOnlineToken> findByRealmId(String realmId);

	QuickBookOnlineToken findByWarehouseId(Long warehouseId);
}
