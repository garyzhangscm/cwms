package com.garyzhangscm.cwms.resources.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// A place hold of information that we will return to
// the front end of the web site
public class SiteInformation {

    @JsonProperty("app")
    private ApplicationInformation applicationInformation = new ApplicationInformation();

    private User user;

    @JsonProperty("menu")
    private List<MenuGroup> menuGroups = new ArrayList<>();


    public static SiteInformation getDefaultSiteInformation() {
        SiteInformation siteInformation = new SiteInformation();

        // Default user
        User user = new User();
        user.setUsername("GZHANG");
        user.setEmail("gzhang1999@gmail.com");
        siteInformation.setUser(user);

        // default menu
        MenuGroup menuGroup = new MenuGroup();
        siteInformation.setMenuGroups(Arrays.asList(new MenuGroup[]{menuGroup}));

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
}
