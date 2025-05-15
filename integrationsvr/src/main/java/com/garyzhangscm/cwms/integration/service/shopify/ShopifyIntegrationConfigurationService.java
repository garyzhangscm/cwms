package com.garyzhangscm.cwms.integration.service.shopify;

import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.ClientRestriction;
import com.garyzhangscm.cwms.integration.model.shopify.ShopifyIntegrationConfiguration;
import com.garyzhangscm.cwms.integration.repository.shopify.ShopifyIntegrationConfigurationRepository;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ShopifyIntegrationConfigurationService {

    @Autowired
    private ShopifyIntegrationConfigurationRepository shopifyIntegrationConfigurationRepository;

    public ShopifyIntegrationConfiguration findById(Long id) {
        return shopifyIntegrationConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Shopify Integration Configuration URL not found by id: " + id));

    }

    public ShopifyIntegrationConfiguration save(ShopifyIntegrationConfiguration shopifyIntegrationConfiguration) {
        return shopifyIntegrationConfigurationRepository.save(shopifyIntegrationConfiguration);
    }

    public ShopifyIntegrationConfiguration saveOrUpdate(ShopifyIntegrationConfiguration shopifyIntegrationConfiguration) {
        if (Objects.isNull(shopifyIntegrationConfiguration.getId()) &&
                Objects.nonNull(findByCompanyIdAndClientIdAndShop(
                        shopifyIntegrationConfiguration.getCompanyId(),
                        shopifyIntegrationConfiguration.getClientId(),
                        shopifyIntegrationConfiguration.getShop()))) {
            shopifyIntegrationConfiguration.setId(
                    findByCompanyIdAndClientIdAndShop(
                            shopifyIntegrationConfiguration.getCompanyId(),
                            shopifyIntegrationConfiguration.getClientId(),
                            shopifyIntegrationConfiguration.getShop()).getId()
            );
        }
        return save(shopifyIntegrationConfiguration);
    }

    public ShopifyIntegrationConfiguration findByCompanyIdAndClientIdAndShop(Long companyId, Long clientId,
                                                                             String shop) {
        List<ShopifyIntegrationConfiguration> shopifyIntegrationConfigurations
                = findByCompanyIdAndShop(companyId, shop);
        return shopifyIntegrationConfigurations.stream().filter(
                shopifyIntegrationConfiguration -> {
                    if (Objects.isNull(clientId)) {
                        return Objects.isNull(shopifyIntegrationConfiguration.getClientId());
                    }
                    return clientId.equals(shopifyIntegrationConfiguration.getClientId());
                }
        ).findFirst().orElse(null);
    }


    public List<ShopifyIntegrationConfiguration> findByCompanyIdAndShop(
            Long companyId, String shop) {

        return shopifyIntegrationConfigurationRepository.findByCompanyIdAndShop(companyId, shop);
    }


    public List<ShopifyIntegrationConfiguration> findAll(Long companyId, Long clientId, String shop,
                                                                ClientRestriction clientRestriction) {
        return
                shopifyIntegrationConfigurationRepository.findAll(
                        (Root<ShopifyIntegrationConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                            List<Predicate> predicates = new ArrayList<Predicate>();

                            predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));

                            if (Objects.nonNull(clientId)) {

                                predicates.add(criteriaBuilder.equal(root.get("clientId"), clientId));
                            }

                            if (Strings.isNotBlank(shop)) {
                                predicates.add(criteriaBuilder.equal(root.get("shop"), shop));

                            }
                            Predicate[] p = new Predicate[predicates.size()];

                            // special handling for 3pl
                            Predicate predicate = criteriaBuilder.and(predicates.toArray(p));

                            return Objects.isNull(clientRestriction) ?
                                    predicate :
                                    clientRestriction.addClientRestriction(predicate,
                                            root, criteriaBuilder);
                        }
                );

    }
}
