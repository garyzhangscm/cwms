package com.garyzhangscm.cwms.resources.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// A place hold of information that we will return to
// the front end of the web site
public class SiteInformation {

    @JsonProperty("app")
    private ApplicationInformation applicationInformation = ApplicationInformation.getApplicationInformation();

    private Boolean singleCompanySite;
    private String defaultCompanyCode;

    // whether we allow the user to load pre-defined testing data
    // this is for test purpose and default to false;
    private Boolean allowDataInitialFlag = false;

    // whether we use server side printing or client side printing
    // Server side printing: print from spring framework
    // client side printing: print by 3rd party tools
    private Boolean serverSidePrinting = true;

    private User user;

    @JsonProperty("menu")
    private List<MenuGroup> menuGroups = new ArrayList<>();

    private WebClientConfiguration webClientConfiguration;


    public static SiteInformation getDefaultSiteInformation(List<MenuGroup> menuGroups) {
        SiteInformation siteInformation = new SiteInformation();

        // Default user
        User user = new User();
        user.setUsername("GZHANG");
        user.setEmail("gzhang1999@gmail.com");
        siteInformation.setUser(user);

        // default menu
        siteInformation.setMenuGroups(menuGroups);

        // An empty configuration normally means enable everything and display everything
        WebClientConfiguration webClientConfiguration = new WebClientConfiguration();
        siteInformation.setWebClientConfiguration(webClientConfiguration);


        return siteInformation;

    }

    public ApplicationInformation getApplicationInformation() {
        return applicationInformation;
    }

    public void setApplicationInformation(ApplicationInformation applicationInformation) {
        this.applicationInformation = applicationInformation;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<MenuGroup> getMenuGroups() {
        return menuGroups;
    }

    public void setMenuGroups(List<MenuGroup> menuGroups) {
        this.menuGroups = menuGroups;
    }


    public Boolean getSingleCompanySite() {
        return singleCompanySite;
    }

    public void setSingleCompanySite(Boolean singleCompanySite) {
        this.singleCompanySite = singleCompanySite;
    }

    public String getDefaultCompanyCode() {
        return defaultCompanyCode;
    }

    public void setDefaultCompanyCode(String defaultCompanyCode) {
        this.defaultCompanyCode = defaultCompanyCode;
    }

    public WebClientConfiguration getWebClientConfiguration() {
        return webClientConfiguration;
    }

    public void setWebClientConfiguration(WebClientConfiguration webClientConfiguration) {
        this.webClientConfiguration = webClientConfiguration;
    }

    public Boolean getAllowDataInitialFlag() {
        return allowDataInitialFlag;
    }

    public void setAllowDataInitialFlag(Boolean allowDataInitialFlag) {
        this.allowDataInitialFlag = allowDataInitialFlag;
    }

    public Boolean getServerSidePrinting() {
        return serverSidePrinting;
    }

    public void setServerSidePrinting(Boolean serverSidePrinting) {
        this.serverSidePrinting = serverSidePrinting;
    }
}
