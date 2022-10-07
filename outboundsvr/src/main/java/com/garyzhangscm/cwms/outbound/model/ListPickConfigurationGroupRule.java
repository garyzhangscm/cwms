package com.garyzhangscm.cwms.outbound.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

import javax.persistence.*;

@Entity
@Table(name = "list_pick_configuration_group_rule")
public class ListPickConfigurationGroupRule extends AuditibleEntity<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "list_pick_configuration_group_rule_id")
    @JsonProperty(value="id")
    private Long id;


    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "list_pick_configuration_id")
    private ListPickConfiguration listPickConfiguration;


    // Group Rule
    @Column(name = "group_rule_type")
    @Enumerated(EnumType.STRING)
    private ListPickGroupRuleType groupRuleType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ListPickConfiguration getListPickConfiguration() {
        return listPickConfiguration;
    }

    public void setListPickConfiguration(ListPickConfiguration listPickConfiguration) {
        this.listPickConfiguration = listPickConfiguration;
    }

    public ListPickGroupRuleType getGroupRuleType() {
        return groupRuleType;
    }

    public void setGroupRuleType(ListPickGroupRuleType groupRuleType) {
        this.groupRuleType = groupRuleType;
    }
}
