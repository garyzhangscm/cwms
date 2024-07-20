package com.garyzhangscm.cwms.resources.model;


import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "web_page_table_column_configuration")
public class WebPageTableColumnConfiguration extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "web_page_table_column_configuration_id")
    private Long id;


    @Column(name = "company_id")
    private Long companyId;


    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;


    @Column(name = "web_page_name")
    private String webPageName;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "column_name")
    private String columnName;
    @Column(name = "column_width")
    private Integer columnWidth;
    @Column(name = "column_sequence")
    private Integer columnSequence;

    @Column(name = "display")
    private Boolean display;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getWebPageName() {
        return webPageName;
    }

    public void setWebPageName(String webPageName) {
        this.webPageName = webPageName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Integer getColumnWidth() {
        return columnWidth;
    }

    public void setColumnWidth(Integer columnWidth) {
        this.columnWidth = columnWidth;
    }

    public Integer getColumnSequence() {
        return columnSequence;
    }

    public void setColumnSequence(Integer columnSequence) {
        this.columnSequence = columnSequence;
    }

    public Boolean getDisplay() {
        return display;
    }

    public void setDisplay(Boolean display) {
        this.display = display;
    }
}
