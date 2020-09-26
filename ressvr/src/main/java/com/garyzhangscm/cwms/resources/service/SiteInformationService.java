package com.garyzhangscm.cwms.resources.service;

import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.model.Company;
import com.garyzhangscm.cwms.resources.model.SiteInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class SiteInformationService {

    private static final Logger logger = LoggerFactory.getLogger(SiteInformationService.class);

    @Value("${site.company.singleCompany}")
    private Boolean singleCompanySite;

    @Value("${site.company.defaultCompanyCode}")
    private String defaultCompanyCode;




    public SiteInformation getDefaultSiteInformation() {
        SiteInformation siteInformation = SiteInformation.getDefaultSiteInformation();

        logger.debug("Objects.isNull(singleCompanySite) ? : {}", Objects.isNull(singleCompanySite)  );
        logger.debug("singleCompanySite ? : {}", singleCompanySite );

        siteInformation.setSingleCompanySite(
                Objects.isNull(singleCompanySite) ? false : singleCompanySite
        );
        if (siteInformation.getSingleCompanySite() == true) {
            // If this is a single company site, then get the only one
            // company and return it as the default company
            siteInformation.setDefaultCompanyCode(defaultCompanyCode);

        }
        return siteInformation;

    }
}
