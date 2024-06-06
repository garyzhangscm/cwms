package com.garyzhangscm.cwms.integration.service.shopify;

import com.garyzhangscm.cwms.integration.model.shopify.AppConfiguration;
import com.garyzhangscm.cwms.integration.repository.shopify.AppConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppConfigurationService {

    @Autowired
    private AppConfigurationRepository appConfigurationRepository;


    @Value("${shopify.redirectUrlAfterShopOAuth:https://staging.claytechsuite.com/api/integration/shopify/shop-oauth/shop-authorization-callback}")
    private String redirectUrlAfterShopOAuth;


    /**
     * There should be only one app in the system
     * @return
     */
    public AppConfiguration getSingletonAppConfiguration() {
        List<AppConfiguration> appConfigurations = appConfigurationRepository.findAll();
        if (appConfigurations.isEmpty()) {
            return null;
        }
        else {
            return appConfigurations.get(0);
        }
    }


    public boolean validateShopOAuthRequest(String hmac, String shop, String timestamp) {
        return true;
    }

    /**
     *
     * redirect to the URL for user's authorization
     *   https://{shop}.myshopify.com/admin/oauth/authorize?
     *     client_id={client_id}
     *     &scope={scopes}
     *     &redirect_uri={redirect_uri}
     *     &state={nonce}
     *     &grant_options[]={access_mode}
     * @param shop example: quickstart-0933fc70.myshopify.com
     * @return
     */
    public String getRedirectUrlForUserAuthorization(String shop) {
        String url = "https://" + shop + "/admin/oauth/authorize?";
        // find the APP's client id from partner dashboard, the app's configuration
        url = url + "client_id=7e0b33dd596b5e74e611d86e50c3d5bd";

        // https://shopify.dev/docs/api/usage/access-scopes
        url = url + "&scope=write_orders,write_assigned_fulfillment_orders,read_customers,write_inventory";
        url = url + "&redirect_uri=" + redirectUrlAfterShopOAuth;
        url = url + "&nonce=" + getNouce(shop);

        //{access_mode}	Sets the access mode.
        // For an online access token, set to per-user. For an offline access token omit this parameter.

        return url;
    }

    private String getNouce(String shop) {
        return String.valueOf(shop.hashCode());
    }

}
