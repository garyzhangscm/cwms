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

package com.garyzhangscm.cwms.resources.model;

import com.fasterxml.jackson.annotation.JsonProperty;


import javax.persistence.*;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "menu_group")
public class MenuGroup implements Comparable<MenuGroup>{

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_group_id")
    private Long id;

    @Column(name = "name")
    private String  name;

    @Column(name = "text")
    private String  text;

    @Column(name = "i18n")
    private String i18n;

    @Column(name = "group_flag")
    @JsonProperty("group")
    private Boolean groupFlag;

    @Column(name = "hide_in_breadcrumb")
    private Boolean hideInBreadcrumb;

    @OneToMany(
            mappedBy = "menuGroup",
            cascade = CascadeType.REMOVE,
            fetch = FetchType.LAZY)
    @JsonProperty("children")
    @OrderBy("sequence ASC")
    private Set<MenuSubGroup> menuSubGroups = new TreeSet<>();


    @Column(name = "sequence")
    private Integer sequence;

    @Override
    public int compareTo(MenuGroup anotherMenuGroup) {
        return this.getSequence() - anotherMenuGroup.getSequence();
    }

    @Override
    public String toString() {
        return "MenuGroup{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", text='" + text + '\'' +
                ", i18n='" + i18n + '\'' +
                ", groupFlag=" + groupFlag +
                ", hideInBreadcrumb=" + hideInBreadcrumb +
                ", menuSubGroups=" + menuSubGroups +
                ", sequence=" + sequence +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getI18n() {
        return i18n;
    }

    public void setI18n(String i18n) {
        this.i18n = i18n;
    }


    public Boolean isHideInBreadcrumb() {
        return hideInBreadcrumb;
    }

    public void setHideInBreadcrumb(Boolean hideInBreadcrumb) {
        this.hideInBreadcrumb = hideInBreadcrumb;
    }

    public Boolean getGroupFlag() {
        return groupFlag;
    }

    public void setGroupFlag(Boolean groupFlag) {
        this.groupFlag = groupFlag;
    }

    public Boolean getHideInBreadcrumb() {
        return hideInBreadcrumb;
    }

    public Set<MenuSubGroup> getMenuSubGroups() {
        return menuSubGroups;
    }

    public void setMenuSubGroups(Set<MenuSubGroup> menuSubGroups) {
        this.menuSubGroups = menuSubGroups;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }
}
