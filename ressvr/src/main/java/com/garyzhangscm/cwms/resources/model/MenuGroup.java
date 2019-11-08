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
    private Integer id;

    @Column(name = "text")
    private String  text;

    @Column(name = "i18n")
    private String i18n;

    @Column(name = "group_flag")
    private Boolean group;

    @Column(name = "hide_in_breadcrumb")
    private Boolean hideInBreadcrumb;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name="menu_group_id")
//    @Transient
    private Set<MenuSubGroup> children = new TreeSet<>();


    @Column(name = "sequence")
    private Integer sequence;

    @Override
    public int compareTo(MenuGroup anotherMenuGroup) {
        return this.getSequence() - anotherMenuGroup.getSequence();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public Boolean isGroup() {
        return group;
    }

    public void setGroup(Boolean group) {
        this.group = group;
    }

    public Boolean isHideInBreadcrumb() {
        return hideInBreadcrumb;
    }

    public void setHideInBreadcrumb(Boolean hideInBreadcrumb) {
        this.hideInBreadcrumb = hideInBreadcrumb;
    }

    public Set<MenuSubGroup> getChildren() {
        return children;
    }

    public void setChildren(Set<MenuSubGroup> children) {
        this.children = children;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }
}
