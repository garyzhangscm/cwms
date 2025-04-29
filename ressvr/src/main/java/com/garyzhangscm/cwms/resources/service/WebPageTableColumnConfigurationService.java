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

import com.garyzhangscm.cwms.resources.exception.ResourceNotFoundException;
import com.garyzhangscm.cwms.resources.model.AlertType;
import com.garyzhangscm.cwms.resources.model.User;
import com.garyzhangscm.cwms.resources.model.WebPageTableColumnConfiguration;
import com.garyzhangscm.cwms.resources.repository.WebPageTableColumnConfigurationRepository;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class WebPageTableColumnConfigurationService {
    private static final Logger logger = LoggerFactory.getLogger(WebPageTableColumnConfigurationService.class);
    @Autowired
    private WebPageTableColumnConfigurationRepository webPageTableColumnConfigurationRepository;
    @Autowired
    private UserService userService;


    public WebPageTableColumnConfiguration findById(Long id) {
        return  webPageTableColumnConfigurationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("Web Page table column configuration not found by id: " + id));
    }


    public List<WebPageTableColumnConfiguration> findAll(Long companyId,
                                                         Long userId,
                                                         String webPageName,
                                                         String tableName,
                                                         String columnName) {

        if (Objects.isNull(userId)) {
            userId = userService.getCurrentUser(companyId).getId();
        }

        Long finalUserId = userId;

        return webPageTableColumnConfigurationRepository.findAll(
                (Root<WebPageTableColumnConfiguration> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    predicates.add(criteriaBuilder.equal(root.get("companyId"), companyId));

                    Join<WebPageTableColumnConfiguration, User> joinUser = root.join("user", JoinType.INNER);
                    predicates.add(criteriaBuilder.equal(joinUser.get("id"), finalUserId));

                    if (Strings.isNotBlank(webPageName)) {

                        predicates.add(criteriaBuilder.equal(root.get("webPageName"), webPageName));
                    }
                    if (Strings.isNotBlank(tableName)) {

                        predicates.add(criteriaBuilder.equal(root.get("tableName"), tableName));
                    }
                    if (Strings.isNotBlank(columnName)) {

                        predicates.add(criteriaBuilder.equal(root.get("columnName"), columnName));
                    }


                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                },
                Sort.by(Sort.Direction.ASC, "webPageName", "tableName", "columnSequence")

        );
    }



    public WebPageTableColumnConfiguration save(WebPageTableColumnConfiguration webPageTableColumnConfiguration) {
        return webPageTableColumnConfigurationRepository.save(webPageTableColumnConfiguration);
    }



    public WebPageTableColumnConfiguration saveOrUpdate(WebPageTableColumnConfiguration webPageTableColumnConfiguration) {
        if (Objects.isNull(webPageTableColumnConfiguration.getId()) &&
                !Objects.isNull(findByWebPageAndTableAndColumnName(
                        webPageTableColumnConfiguration.getCompanyId(),
                        webPageTableColumnConfiguration.getUser().getId(),
                        webPageTableColumnConfiguration.getWebPageName(),
                        webPageTableColumnConfiguration.getTableName(),
                        webPageTableColumnConfiguration.getColumnName()))) {
            webPageTableColumnConfiguration.setId(
                    findByWebPageAndTableAndColumnName(
                            webPageTableColumnConfiguration.getCompanyId(),
                            webPageTableColumnConfiguration.getUser().getId(),
                            webPageTableColumnConfiguration.getWebPageName(),
                            webPageTableColumnConfiguration.getTableName(),
                            webPageTableColumnConfiguration.getColumnName()).getId());
        }
        return save(webPageTableColumnConfiguration);
    }

    public WebPageTableColumnConfiguration findByWebPageAndTableAndColumnName(Long companyId,
                                                                              Long userId,
                                                                              String webPageName,
                                                                              String tableName,
                                                                              String columnName) {
        return webPageTableColumnConfigurationRepository.findByWebPageAndTableAndColumnName(
                companyId, userId, webPageName, tableName, columnName
        );
    }


    public WebPageTableColumnConfiguration addWebPageTableColumnConfiguration(WebPageTableColumnConfiguration webPageTableColumnConfiguration) {
        if (Objects.isNull(webPageTableColumnConfiguration.getUser())) {
            // get the current user
            webPageTableColumnConfiguration.setUser(userService.getCurrentUser(
                    webPageTableColumnConfiguration.getCompanyId()
            ));
        }
        return saveOrUpdate(webPageTableColumnConfiguration);

    }

    public WebPageTableColumnConfiguration changeWebPageTableColumnConfiguration(Long id,
                                                                                 WebPageTableColumnConfiguration webPageTableColumnConfiguration) {
        if (Objects.isNull(webPageTableColumnConfiguration.getUser())) {
            // get the current user
            webPageTableColumnConfiguration.setUser(userService.getCurrentUser(
                    webPageTableColumnConfiguration.getCompanyId()
            ));
        }

        webPageTableColumnConfiguration.setId(id);
        return saveOrUpdate(webPageTableColumnConfiguration);

    }

    public void delete(Long id) {
        webPageTableColumnConfigurationRepository.deleteById(id);

    }

    public List<WebPageTableColumnConfiguration> addWebPageTableColumnConfigurations(
             List<WebPageTableColumnConfiguration> webPageTableColumnConfigurations) {
        return webPageTableColumnConfigurations.stream().map(
                webPageTableColumnConfiguration -> addWebPageTableColumnConfiguration(webPageTableColumnConfiguration)
        ).collect(Collectors.toList());

    }

    public List<WebPageTableColumnConfiguration> changeWebPageTableColumnConfigurations(
            List<WebPageTableColumnConfiguration> webPageTableColumnConfigurations) {
        return webPageTableColumnConfigurations.stream().map(
                webPageTableColumnConfiguration -> changeWebPageTableColumnConfiguration(
                        webPageTableColumnConfiguration.getId(), webPageTableColumnConfiguration)
        ).collect(Collectors.toList());

    }

}
