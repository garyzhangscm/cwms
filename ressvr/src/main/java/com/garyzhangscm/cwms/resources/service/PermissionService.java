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
import com.garyzhangscm.cwms.resources.model.*;
import com.garyzhangscm.cwms.resources.repository.PermissionRepository;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class PermissionService {
    private static final Logger logger = LoggerFactory.getLogger(PermissionService.class);
    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private MenuService menuService;
    @Autowired
    private FileService fileService;

    public Permission findById(Long id) {
        Permission permission =  permissionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.raiseException("permission not found by id: " + id));
        return permission;
    }

    public Permission findByMenuAndName(Menu menu, String name) {
        return permissionRepository.findByMenuAndName(menu, name);
    }

    public List<Permission> findAll(Menu menu, String name, String menuIds) {

        return permissionRepository.findAll(
                (Root<Permission> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<Predicate>();

                    if (Objects.nonNull(menu)) {
                        Join<Permission, Menu> joinMenu = root.join("menu", JoinType.INNER);
                        predicates.add(criteriaBuilder.equal(
                                joinMenu.get("id"), menu.getId()));
                    }
                    if (Strings.isNotBlank(name)) {
                        predicates.add(criteriaBuilder.equal(root.get("name"), name));
                    }
                    if (Strings.isNotBlank(menuIds)) {

                        Join<Permission, Menu> joinMenu = root.join("menu", JoinType.INNER);
                        CriteriaBuilder.In<Long> inMenuIds = criteriaBuilder.in(joinMenu.get("id"));
                        for(String id : menuIds.split(",")) {
                            inMenuIds.value(Long.parseLong(id));
                        }
                        predicates.add(criteriaBuilder.and(inMenuIds));
                    }
                    Predicate[] p = new Predicate[predicates.size()];
                    return criteriaBuilder.and(predicates.toArray(p));
                }
        );



    }

    public Permission save(Permission permission) {
        return permissionRepository.save(permission);
    }

    public Permission saveOrUpdate(Permission permission) {
        if (Objects.isNull(permission.getId()) &&
                !Objects.isNull(findByMenuAndName(permission.getMenu(), permission.getName()))) {
            permission.setId(findByMenuAndName(permission.getMenu(), permission.getName()).getId());
        }
        return save(permission);
    }
    public Permission addPermission(Permission permission) {
        return saveOrUpdate(permission);


    }

    public void delete(Long id) {
        permissionRepository.deleteById(id);
    }


    private Permission convertFromWrapper(PermissionCSVWrapper permissionCSVWrapper) {

        Permission permission = new Permission();

        Menu menu = menuService.findByName(permissionCSVWrapper.getMenu());
        if (Objects.isNull(menu)) {
            throw ResourceNotFoundException.raiseException("Can't find menu by name " +
                    permissionCSVWrapper.getMenu());
        }
        permission.setMenu(menu);
        permission.setName(permissionCSVWrapper.getName());
        permission.setDescription(permissionCSVWrapper.getDescription());

        return permission;
    }

    public void initData(String fileName) {
        try {
            InputStream inputStream = new ClassPathResource(fileName).getInputStream();
            List<PermissionCSVWrapper> permissionCSVWrappers = fileService.loadData(inputStream,  PermissionCSVWrapper.class);
            permissionCSVWrappers.stream().forEach(permissionCSVWrapper -> saveOrUpdate(convertFromWrapper(permissionCSVWrapper)));
        } catch (IOException ex) {
            logger.debug("Exception while init permission data: {}", ex.getMessage());
        }
    }
}
