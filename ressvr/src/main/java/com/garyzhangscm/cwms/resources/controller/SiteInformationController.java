/**
 * Copyright 2019
 *
 * @author gzhang
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.garyzhangscm.cwms.resources.controller;

import com.garyzhangscm.cwms.resources.model.SiteInformation;
import com.garyzhangscm.cwms.resources.service.MenuGroupService;
import com.garyzhangscm.cwms.resources.service.UserService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class SiteInformationController {

    private static final Logger logger = LoggerFactory.getLogger(SiteInformationController.class);
    @Autowired
    private UserService userService;

    @RequestMapping(value = "/site-information")
    public SiteInformation getSiteInformation() {
        logger.debug("Start to get site information for user {}",
                userService.getCurrentUserName());
        if (StringUtils.isBlank(userService.getCurrentUserName())) {
            return SiteInformation.getDefaultSiteInformation();
        }
        else {
            return userService.getSiteInformaiton(userService.getCurrentUserName());
        }
    }

    @RequestMapping(value = "/site-information/default")
    public SiteInformation getDefaultSiteInformation() {
        return SiteInformation.getDefaultSiteInformation();
    }


}
