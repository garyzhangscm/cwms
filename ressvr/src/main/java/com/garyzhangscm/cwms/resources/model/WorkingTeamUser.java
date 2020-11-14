package com.garyzhangscm.cwms.resources.model;



public class WorkingTeamUser {


    private Long companyId;

    private String workingTeamName;

    private String username;

    public String getWorkingTeamName() {
        return workingTeamName;
    }

    public void setWorkingTeamName(String workingTeamName) {
        this.workingTeamName = workingTeamName;
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
}
