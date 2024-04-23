package com.garyzhangscm.cwms.integration.service.tiktok;

import com.garyzhangscm.cwms.integration.ResponseBodyWrapper;
import com.garyzhangscm.cwms.integration.clients.CommonServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.TikTokAPIRestemplateClient;
import com.garyzhangscm.cwms.integration.clients.WarehouseLayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.integration.exception.GenericException;
import com.garyzhangscm.cwms.integration.exception.MissingInformationException;
import com.garyzhangscm.cwms.integration.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.integration.model.Client;
import com.garyzhangscm.cwms.integration.model.Company;
import com.garyzhangscm.cwms.integration.model.Warehouse;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokSellerShop;
import com.garyzhangscm.cwms.integration.model.tiktok.TikTokSellerShopIntegrationConfiguration;
import com.garyzhangscm.cwms.integration.model.tiktok.TiktokRequestAccessTokenAPICallResponse;
import com.garyzhangscm.cwms.integration.repository.tiktok.TikTokSellerShopIntegrationConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class TikTokSellerShopIntegrationConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(TikTokSellerShopIntegrationConfigurationService.class);


    @Autowired
    private WarehouseLayoutServiceRestemplateClient warehouseLayoutServiceRestemplateClient;
    @Autowired
    private CommonServiceRestemplateClient commonServiceRestemplateClient;
    @Autowired
    private TikTokSellerShopService tikTokSellerShopService;

    @Autowired
    private TikTokSellerShopIntegrationConfigurationRepository tikTokSellerShopIntegrationConfigurationRepository;

    @Autowired
    private TikTokAPIRestemplateClient tikTokAPIRestemplateClient;

    @Value("${tiktok.domain.appAuth:NOT-SET-YET}")
    private String appAuthDomain;
    @Value("${tiktok.serviceId:NOT-SET-YET}")
    private String serviceId;

    public TikTokSellerShopIntegrationConfiguration findById(Long id) {
        return tikTokSellerShopIntegrationConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("TikTok Seller Shop Integration Configuration not found by id: " + id));

    }

    public TikTokSellerShopIntegrationConfiguration save(TikTokSellerShopIntegrationConfiguration tikTokSellerShopIntegrationConfiguration) {
        return tikTokSellerShopIntegrationConfigurationRepository.save(tikTokSellerShopIntegrationConfiguration);
    }

    public TikTokSellerShopIntegrationConfiguration saveOrUpdate(TikTokSellerShopIntegrationConfiguration tikTokSellerShopIntegrationConfiguration) {
        if (Objects.isNull(tikTokSellerShopIntegrationConfiguration.getId()) &&
            Objects.nonNull(findByCompanyIdAndClientIdAndOpenId(
                    tikTokSellerShopIntegrationConfiguration.getCompanyId(),
                    tikTokSellerShopIntegrationConfiguration.getClientId(),
                    tikTokSellerShopIntegrationConfiguration.getOpenId()))) {
            tikTokSellerShopIntegrationConfiguration.setId(
                    findByCompanyIdAndClientIdAndOpenId(
                            tikTokSellerShopIntegrationConfiguration.getCompanyId(),
                            tikTokSellerShopIntegrationConfiguration.getClientId(),
                            tikTokSellerShopIntegrationConfiguration.getOpenId()).getId()
            );
        }
        return save(tikTokSellerShopIntegrationConfiguration);
    }



    /**
     * For each seller, when the seller first approve the APP, we will get an authCode. We will use this code
     * to get the access token and refresh token
     * @param state
     * @param authCode
     */
    public void initTikTokSellerShopIntegrationConfiguration(String authCode, String state) {
        // let's find the right company based on the state
        Pair<Long, Long> companyIdAndClientIdFromState = getCompanyIdAndClientIdFromState(state);
        if (Objects.isNull(companyIdAndClientIdFromState)) {
            logger.debug("fail to find company id and client id  by state {}", state);
        }
        else {

            Long companyId = companyIdAndClientIdFromState.getFirst();
            Long clientId = companyIdAndClientIdFromState.getSecond() >= 0 ?
                    companyIdAndClientIdFromState.getSecond() : null;   // special handling as Pair doesn't allow null value
            logger.debug("find company {} and client {} for this state {}",
                    companyId,
                    Objects.isNull(clientId) ? "N/A" : clientId, state);
            TiktokRequestAccessTokenAPICallResponse tiktokRequestAccessTokenAPICallResponse = tikTokAPIRestemplateClient.requestSellerAccessToken(authCode);
            //logger.debug("tiktokRequestAccessTokenAPICallResponse:\n {}", tiktokRequestAccessTokenAPICallResponse);

            TikTokSellerShopIntegrationConfiguration tikTokSellerShopIntegrationConfiguration =
                    setupTikTokSellerShopIntegrationConfiguration(
                            companyId,
                            clientId,
                            authCode, tiktokRequestAccessTokenAPICallResponse);

            //logger.debug("get seller's toke:\n{}", tikTokSellerShopIntegrationConfiguration);
            //logger.debug("start to save to the DB for company {} / {}",
            //        company.getId(), company.getCode());

            saveOrUpdate(tikTokSellerShopIntegrationConfiguration);

            // setup the shops
            logger.debug("after we get the authorization from the seller, we will setup the shops for the seller");
            setupTiktokSellerShops(tikTokSellerShopIntegrationConfiguration);

        }
    }

    private void setupTiktokSellerShops(TikTokSellerShopIntegrationConfiguration tikTokSellerShopIntegrationConfiguration) {
        tikTokSellerShopService.setupTiktokSellerShops(tikTokSellerShopIntegrationConfiguration);
    }

    private TikTokSellerShopIntegrationConfiguration setupTikTokSellerShopIntegrationConfiguration(Long companyId,
                                                                                                   Long clientId,
                                                                                                   String authCode,
                                                                                                   TiktokRequestAccessTokenAPICallResponse tiktokRequestAccessTokenAPICallResponse) {
        TikTokSellerShopIntegrationConfiguration tikTokSellerShopIntegrationConfiguration = new TikTokSellerShopIntegrationConfiguration();

        tikTokSellerShopIntegrationConfiguration.setCompanyId(companyId);
        tikTokSellerShopIntegrationConfiguration.setClientId(clientId);
        tikTokSellerShopIntegrationConfiguration.setAuthCode(authCode);

        tikTokSellerShopIntegrationConfiguration.setAccessToken(tiktokRequestAccessTokenAPICallResponse.getAccessToken());
        tikTokSellerShopIntegrationConfiguration.setAccessTokenExpireIn(tiktokRequestAccessTokenAPICallResponse.getAccessTokenExpireIn());

        tikTokSellerShopIntegrationConfiguration.setRefreshToken(tiktokRequestAccessTokenAPICallResponse.getRefreshToken());
        tikTokSellerShopIntegrationConfiguration.setRefreshTokenExpireIn(tiktokRequestAccessTokenAPICallResponse.getRefreshTokenExpireIn());

        tikTokSellerShopIntegrationConfiguration.setOpenId(tiktokRequestAccessTokenAPICallResponse.getOpenId());
        tikTokSellerShopIntegrationConfiguration.setSellerName(tiktokRequestAccessTokenAPICallResponse.getSellerName());

        tikTokSellerShopIntegrationConfiguration.setSellerBaseRegion(tiktokRequestAccessTokenAPICallResponse.getSellerBaseRegion());
        tikTokSellerShopIntegrationConfiguration.setUserType(tiktokRequestAccessTokenAPICallResponse.getUserType());

        return tikTokSellerShopIntegrationConfiguration;
    }

    public List<TikTokSellerShopIntegrationConfiguration> getTikTokSellerShopIntegrationConfigurationByCompany(Long companyId, Long clientId) {

            return tikTokSellerShopIntegrationConfigurationRepository.findByCompanyIdAndClientId(companyId, clientId);
    }

    public TikTokSellerShopIntegrationConfiguration findByCompanyIdAndClientIdAndOpenId(
            Long companyId, Long clientId, String openId) {

        return tikTokSellerShopIntegrationConfigurationRepository.findByCompanyIdAndClientIdAndOpenId(companyId, clientId, openId);
    }
    /**
     * Get the company from the state value. For state value, see the function  getStateCode
     * @param state
     * @return
     */
    private Pair<Long, Long> getCompanyIdAndClientIdFromState(String state) {
        List<Company> companies = warehouseLayoutServiceRestemplateClient.getAllCompanies();

        for (Company company : companies) {
            // if the seller is assigned to the company, not 3pl client
            if (getStateCode(company, null).equalsIgnoreCase(state)) {
                return Pair.of(company.getId(), -1l);
            }

            try {

                // get the client ids from all warehouses of this company
                List<Warehouse> warehouses = warehouseLayoutServiceRestemplateClient.getWarehouseByCompany(company.getId());
                Set<Long> clientIdSet = new HashSet<>();
                warehouses.forEach(
                        warehouse -> {
                            List<Client> clients = commonServiceRestemplateClient.getAllClients(warehouse.getId());
                            clients.forEach(
                                    client -> clientIdSet.add(client.getId())
                            );
                        }
                );

                for (Long clientId : clientIdSet) {

                    if (getStateCode(company, clientId).equalsIgnoreCase(state)) {
                        return Pair.of(company.getId(), clientId);
                    }
                }
            }
            catch(GenericException ex) {
                ex.printStackTrace();
                logger.debug("can't load client for the company {}, we will simply ignore the error for now",
                        company.getCode());
            }
        }
        return null;
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
    public String getStateCode(Company company, Long clientId) {
        return String.valueOf((company.getCode() +
                (Objects.isNull(clientId) ? "_" : clientId) +
                company.getApiSecret()).hashCode());
    }

    public String getStateCode(Long companyId, Long clientId) {
        return getStateCode(
                warehouseLayoutServiceRestemplateClient.getCompanyById(companyId),
                clientId
        );
    }

    public String getTikTokSellerShopIntegrationSellerAuthUrl(Long companyId, Long clientId) {
        Company company = warehouseLayoutServiceRestemplateClient.getCompanyById(companyId);
        if (Objects.isNull(company)) {
            throw MissingInformationException.raiseException("Can't find company by id " + companyId);
        }

        //https://services.us.tiktokshop.com/open/authorize?service_id=7172**********70150&state=xaoegsefowuf
        return "https://services.us.tiktokshop.com/open/authorize?service_id=" + serviceId +"&state=" + getStateCode(company, clientId);
    }

    public void removeTikTokSellerShopIntegrationConfigurationByCompany(Long id) {
        tikTokSellerShopIntegrationConfigurationRepository.deleteById(id);
    }

    /**
     * Change the configuration . Right now we are only allow to change the value autoRefreshOrderTimeWindowInMinute when changing
     * from the web client
     * @param id
     * @param tikTokSellerShopIntegrationConfiguration
     * @return
     */
    public TikTokSellerShopIntegrationConfiguration changeTikTokSellerShopIntegrationConfigurationByCompany(Long id,
                                                                                                            TikTokSellerShopIntegrationConfiguration tikTokSellerShopIntegrationConfiguration) {
         TikTokSellerShopIntegrationConfiguration existingTikTokSellerShopIntegrationConfiguration = findById(id);

         existingTikTokSellerShopIntegrationConfiguration.setAutoRefreshOrderTimeWindowInMinute(tikTokSellerShopIntegrationConfiguration.getAutoRefreshOrderTimeWindowInMinute());

        return saveOrUpdate(existingTikTokSellerShopIntegrationConfiguration);
    }
}
