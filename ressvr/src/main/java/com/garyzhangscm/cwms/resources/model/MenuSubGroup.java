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
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "menu_sub_group")
public class MenuSubGroup implements Comparable<MenuSubGroup>{

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_sub_group_id")
    private Long id;

    @Column(name = "name")
    private String  name;

    @Column(name = "text")
    private String  text;

    @Column(name = "i18n")
    private String i18n;

    @Column(name = "icon")
    private String icon;

    @Column(name = "shortcut_root")
    private Boolean shortcutRoot;

    @ManyToOne
    @JoinColumn(name = "menu_group_id")
    @JsonIgnore
    private MenuGroup menuGroup;

    @OneToMany(mappedBy = "menuSubGroup", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonProperty("children")
    @OrderBy("sequence ASC")
    private Set<Menu> menus = new TreeSet<>();


    @Column(name = "sequence")
    private Integer sequence;

    @Column(name = "link")
    private String link;

    @Column(name = "badge")
    private Integer badge;

    @Override
    public int compareTo(MenuSubGroup anotherMenuSubGroup) {
        return this.getSequence().compareTo(anotherMenuSubGroup.getSequence());
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Boolean getShortcutRoot() {
        return shortcutRoot;
    }

    public Set<Menu> getMenus() {
        return menus;
    }

    public void setMenus(Set<Menu> menus) {
        this.menus = menus;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public Boolean isShortcutRoot() {
        return shortcutRoot;
    }

    public void setShortcutRoot(Boolean shortcutRoot) {
        this.shortcutRoot = shortcutRoot;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Integer getBadge() {
        return badge;
    }

    public void setBadge(Integer badge) {
        this.badge = badge;
    }

    public MenuGroup getMenuGroup() {
        return menuGroup;
    }

    public void setMenuGroup(MenuGroup menuGroup) {
        this.menuGroup = menuGroup;
    }
}
