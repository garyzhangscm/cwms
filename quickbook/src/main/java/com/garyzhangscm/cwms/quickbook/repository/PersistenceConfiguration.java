package com.garyzhangscm.cwms.quickbook.repository;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author dderose
 *
 */
@Configuration
@EnableAutoConfiguration
@EntityScan(basePackages = {"com.garyzhangscm.cwms.quickbook.model"})
@EnableJpaRepositories(basePackages = {"com.garyzhangscm.cwms.quickbook.repository"})
@EnableTransactionManagement
public class PersistenceConfiguration {

}
