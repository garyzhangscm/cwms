package com.garyzhangscm.cwms.integration.service.tiktok;

import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.model.Company;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;


@Service
public class TikTokService {

    private static final Logger logger = LoggerFactory.getLogger(TikTokService.class);

    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;

    @Autowired
    private TikTokSellerShopIntegrationConfigurationService tikTokSellerShopIntegrationConfigurationService;

    /**
     * When the seller authorizes our APP, we will save the auth code, which we will use
     * later on to get the access code.
     * Auth code: permanent value, as long as the seller auth the APP
     * access code(refresh code): temporary code, we will need the auth code to initial a
     *               access code and use the refresh code to get new access code, when it is
     *               expired. Access code is used to get the information from the seller via API
     * @param authCode auth code
     * @param state identifier used to identify the compay information for this seller
     */
    public void processSellerAuthCallback(String authCode, String state) {
        Company company = getCompanyFromState(state);
        if (Objects.nonNull(company)) {
            // we found the company based on the state, let's save the result
            tikTokSellerShopIntegrationConfigurationService.initTikTokSellerShopIntegrationConfiguration(
                    company, authCode
            );
        }
        else {

            logger.debug("can't get company from state: {}", state);
        }
    }

    /**
     * Get the company from the state value. For state value, see the function  getStateCode
     * @param state
     * @return
     */
    private Company getCompanyFromState(String state) {
        List<Company> companies = warehouseLayoutServiceRestemplateClient.getAllCompanies();

        return companies.stream().filter(
                company -> getStateCode(company).equalsIgnoreCase(state)
        ).findFirst().orElse(null);
    }

    /**
     * Generate a Tiktok state code for the company. State code is only used by the auth URL.
     * When we want to access the seller's information via API, the first step is to get the seller's
     * authorization. We will send the seller a URL, which the seller can click and auth our APP
     * We will include the 'state' value in the URL so that we can contain the company's information
     * and when the seller auth the APP, we can tie the seller to the company
     * @param company
     * @return
     */
    private String getStateCode(Company company) {
        return String.valueOf((company.getCode() + company.getApiSecret()).hashCode());
    }
}
