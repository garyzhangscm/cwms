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

package com.garyzhangscm.cwms.resources.repository;

import com.garyzhangscm.cwms.resources.model.WebPageTableColumnConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface WebPageTableColumnConfigurationRepository
        extends JpaRepository<WebPageTableColumnConfiguration, Long>, JpaSpecificationExecutor<WebPageTableColumnConfiguration> {

    @Query("select cf from WebPageTableColumnConfiguration  cf where cf.companyId = :companyId " +
            " and cf.user.id = :userId and cf.webPageName = :webPageName and cf.tableName = :tableName" +
            "  and cf.columnName = :columnName ")
    WebPageTableColumnConfiguration findByWebPageAndTableAndColumnName(Long companyId,
                                                                       Long userId,
                                                                       String webPageName,
                                                                       String tableName,
                                                                       String columnName);
}
