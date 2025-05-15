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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.springframework.beans.BeanUtils;

import javax.persistence.*;
import java.sql.Timestamp;

import java.time.ZonedDateTime;
import java.util.*;

@Entity
@Table(name = "user_info")
public class User extends AuditibleEntity<String>  {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "username")
    private String username;
    @Column(name = "first_name")
    private String firstname;
    @Column(name = "last_name")
    private String lastname;
    @Column(name = "email")
    private String email;


    // system admin. the admin will have full access
    // to any company and warehouse in the system
    @Column(name = "is_system_admin")
    private Boolean isSystemAdmin = false;

    // Company admin, who will have full access to
    // specific company
    @Column(name = "is_admin")
    private Boolean isAdmin = false;

    @Column(name = "change_password_at_next_logon")
    private Boolean changePasswordAtNextLogon = false;


    @Column(name = "last_login_company_id")
    private Long lastLoginCompanyId;
    @Column(name = "last_login_warehouse_id")
    private Long lastLoginWarehouseId;
    @Column(name = "last_login_token")
    private String lastLoginToken;


    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private UserType type;

    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Transient
    private boolean enabled;
    @Transient
    private boolean locked;


    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private List<Role> roles = new ArrayList<>();

    @ManyToMany(cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    @JoinTable(name = "working_team_user",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "working_team_id"))
    private List<WorkingTeam> workingTeams = new ArrayList<>();



    @ManyToOne
    @JoinColumn(name="department_id")
    private Department department;

    @Column(name = "position")
    private String position;

    @Column(name = "on_board_time")
    @JsonDeserialize(using = CustomZonedDateTimeDeserializer.class)
    @JsonSerialize(using = CustomZonedDateTimeSerializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private ZonedDateTime onBoardTime;
    // @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    // @JsonSerialize(using = LocalDateTimeSerializer.class)
    // @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    // private LocalDateTime onBoardTime;


    @Column(name = "worker_type")
    @Enumerated(EnumType.STRING)
    private WorkerType workerType;



    @Transient
    private String token;
    @Transient
    private String refreshToken;
    @Transient
    private String name;
    @Transient
    private Timestamp time;
    @Transient
    private int refreshIn;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        if(Objects.nonNull(id) &&
           Objects.nonNull(user.getId())) {
            return Objects.equals(id, user.id);
        }

        return companyId.equals(user.getCompanyId()) &&
                username.equals(user.getUsername());
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

    @JsonIgnore
    public UserAuth getUserAuth() {
        UserAuth userAuth = new UserAuth();
        userAuth.setCompanyId(companyId);
        userAuth.setUsername(username);
        userAuth.setPassword(password);
        userAuth.setEmail(email);
        userAuth.setEnabled(enabled);
        userAuth.setLocked(locked);
        return userAuth;
    }

    /**
     * Create a copy of current user
     * @return
     */
    public User copy(String username, String firstname, String lastname) {
        User user = new User();
        BeanUtils.copyProperties(this, user, "id",
                "username","password", "firstname", "lastname", "isSystemAdmin",
                "email", "workingTeams", "roles",
                "lastLoginCompanyId", "lastLoginWarehouseId", "lastLoginToken",
                "onBoardTime");
        // copy the roles into a new list
        user.setRoles(new ArrayList<>(getRoles()));
        user.setWorkingTeams(new ArrayList<>(getWorkingTeams()));
        // default the values for the new user
        user.setUsername(username);
        user.setPassword(username);
        user.setFirstname(firstname);
        user.setLastname(lastname);
        user.setSystemAdmin(false);
        user.setEmail("");
        user.setChangePasswordAtNextLogon(true);
        return user;


    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getAdmin() {
        return isAdmin;
    }

    public void setAdmin(Boolean admin) {
        isAdmin = admin;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }
    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {return username;}

    public Long getLastLoginCompanyId() {
        return lastLoginCompanyId;
    }

    public void setLastLoginCompanyId(Long lastLoginCompanyId) {
        this.lastLoginCompanyId = lastLoginCompanyId;
    }

    public Long getLastLoginWarehouseId() {
        return lastLoginWarehouseId;
    }

    public void setLastLoginWarehouseId(Long lastLoginWarehouseId) {
        this.lastLoginWarehouseId = lastLoginWarehouseId;
    }

    public Boolean getChangePasswordAtNextLogon() {
        return changePasswordAtNextLogon;
    }

    public void setChangePasswordAtNextLogon(Boolean changePasswordAtNextLogon) {
        this.changePasswordAtNextLogon = changePasswordAtNextLogon;
    }

    public void assignRole(Role role) {

        if (!getRoles().contains(role)) {
            getRoles().add(role);
        }
    }


    public void deassignRole(Role role) {

        if (getRoles().contains(role)) {
            getRoles().remove(role);
        }
    }

    public void assignWorkingTeam(WorkingTeam workingTeam) {

        if (!getWorkingTeams().contains(workingTeam)) {
            getWorkingTeams().add(workingTeam);
        }
    }


    public void deassignWorkingTeam(WorkingTeam workingTeam) {

        if (getWorkingTeams().contains(workingTeam)) {
            getWorkingTeams().remove(workingTeam);
        }
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<WorkingTeam> getWorkingTeams() {
        return workingTeams;
    }

    public void setWorkingTeams(List<WorkingTeam> workingTeams) {
        this.workingTeams = workingTeams;
    }

    public Boolean getSystemAdmin() {
        return isSystemAdmin;
    }

    public void setSystemAdmin(Boolean systemAdmin) {
        isSystemAdmin = systemAdmin;
    }

    public String getLastLoginToken() {
        return lastLoginToken;
    }

    public void setLastLoginToken(String lastLoginToken) {
        this.lastLoginToken = lastLoginToken;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public ZonedDateTime getOnBoardTime() {
        return onBoardTime;
    }

    public void setOnBoardTime(ZonedDateTime onBoardTime) {
        this.onBoardTime = onBoardTime;
    }

    public WorkerType getWorkerType() {
        return workerType;
    }

    public void setWorkerType(WorkerType workerType) {
        this.workerType = workerType;
    }

    public UserType getType() {
        return type;
    }

    public void setType(UserType type) {
        this.type = type;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }

    public int getRefreshIn() {
        return refreshIn;
    }

    public void setRefreshIn(int refreshIn) {
        this.refreshIn = refreshIn;
    }
}
