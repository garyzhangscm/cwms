package com.garyzhangscm.cwms.integration.service.shopify;

import org.springframework.stereotype.Service;

@Service
public class ShopifyIntegrationService {


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
        url = url + "&redirect_uri=https://prod.claytechsuite.com/api/integration/shopify/user-authorization-callback";
        url = url + "&nonce=" + getNouce(shop);

        //{access_mode}	Sets the access mode.
        // For an online access token, set to per-user. For an offline access token omit this parameter.

        return url;
    }

    private String getNouce(String shop) {
        return String.valueOf(shop.hashCode());
    }
}
