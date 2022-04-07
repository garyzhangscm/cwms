package com.garyzhangscm.cwms.resources.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.codehaus.jackson.annotate.JsonIgnore;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "role")
public class Role extends AuditibleEntity<String>  {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "enabled")
    private Boolean enabled;

    @ManyToMany(cascade = {
            CascadeType.ALL
    })
    @JoinTable(name = "role_menu",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "menu_id")
    )
    private List<Menu> menus = new ArrayList<>();


    @OneToMany(
            mappedBy = "role",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<RoleClientAccess> clientAccesses = new ArrayList<>();


    // whether the role has access to the non client data
    // by default, the role has access to any non client data
    @Column(name = "non_client_data_accessible")
    private Boolean nonClientDataAccessible;

    // Place holder to accept the JSON object from the web client
    // when creating a new role with assigned menu
    // and users
    @Transient
    private List<MenuGroup> menuGroups = new ArrayList<>();
    @Transient
    private List<User> users = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, enabled, menus);
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<Menu> getMenus() {

        return menus;
    }

    public void setMenus(List<Menu> menus) {
        this.menus = menus;
    }

    public void assignMenu(Menu menu) {
        if (!getMenus().contains(menu)) {
            getMenus().add(menu);
        }
    }
    public void deassignMenu(Menu menu) {
        if (getMenus().contains(menu)) {
            getMenus().remove(menu);
        }
    }

    public List<MenuGroup> getMenuGroups() {
        return menuGroups;
    }

    public void setMenuGroups(List<MenuGroup> menuGroups) {
        this.menuGroups = menuGroups;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public List<RoleClientAccess> getClientAccesses() {
        return clientAccesses;
    }

    public void setClientAccesses(List<RoleClientAccess> clientAccesses) {
        this.clientAccesses = clientAccesses;
    }
    public void addClientAccess(RoleClientAccess clientAccess) {
        this.clientAccesses.add(clientAccess);
    }

    public Boolean getNonClientDataAccessible() {
        return nonClientDataAccessible;
    }

    public void setNonClientDataAccessible(Boolean nonClientDataAccessible) {
        this.nonClientDataAccessible = nonClientDataAccessible;
    }
}
