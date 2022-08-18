package com.garyzhangscm.cwms.quickbook.repository;

import com.garyzhangscm.cwms.quickbook.model.QuickBookOnlineConfiguration;
import com.garyzhangscm.cwms.quickbook.model.QuickBookWebhookHistory;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

/**
 * @author dderose
 *
 */
public interface QuickbookWebhookRepository extends CrudRepository<QuickBookWebhookHistory, Long> , JpaSpecificationExecutor<QuickBookWebhookHistory> {


}
