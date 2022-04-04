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

package com.garyzhangscm.cwms.resources.service;

import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.garyzhangscm.cwms.resources.clients.LayoutServiceRestemplateClient;
import com.garyzhangscm.cwms.resources.model.Menu;
import com.garyzhangscm.cwms.resources.model.Role;
import com.garyzhangscm.cwms.resources.model.RoleClientAccess;
import com.garyzhangscm.cwms.resources.model.RoleMenu;
import com.garyzhangscm.cwms.resources.repository.RoleClientAccessRepository;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class RoleClientAccessService{
    private static final Logger logger = LoggerFactory.getLogger(RoleClientAccessService.class);


    @Autowired
    private RoleClientAccessRepository roleClientAccessRepository;

    public RoleClientAccess findByRoleAndClient(Long roleId, Long clientId) {
        return roleClientAccessRepository.findByRoleAndClient(roleId, clientId);
    }

    public RoleClientAccess findByRoleAndClient(Role role, Long clientId) {
        return roleClientAccessRepository.findByRoleAndClient(role.getId(), clientId);
    }

}
