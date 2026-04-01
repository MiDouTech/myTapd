package com.miduo.cloud.ticket.entity.dto.alert;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 夜莺（Nightingale）告警事件 Webhook 请求体
 * 对应 AlertCurEvent 结构体
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NightingaleAlertEvent implements Serializable {

    private Long id;

    private String cate;

    private String cluster;

    @JsonProperty("datasource_id")
    private Long datasourceId;

    @JsonProperty("group_id")
    private Long groupId;

    @JsonProperty("group_name")
    private String groupName;

    private String hash;

    @JsonProperty("rule_id")
    private Long ruleId;

    @JsonProperty("rule_name")
    private String ruleName;

    @JsonProperty("rule_note")
    private String ruleNote;

    @JsonProperty("rule_prod")
    private String ruleProd;

    private Integer severity;

    @JsonProperty("prom_for_duration")
    private Integer promForDuration;

    @JsonProperty("prom_ql")
    private String promQl;

    @JsonProperty("rule_config")
    private Object ruleConfig;

    @JsonProperty("prom_eval_interval")
    private Integer promEvalInterval;

    private List<String> callbacks;

    @JsonProperty("notify_recovered")
    private Integer notifyRecovered;

    @JsonProperty("notify_channels")
    private List<String> notifyChannels;

    @JsonProperty("notify_groups")
    private List<String> notifyGroups;

    @JsonProperty("notify_groups_obj")
    private List<Object> notifyGroupsObj;

    @JsonProperty("target_ident")
    private String targetIdent;

    @JsonProperty("target_note")
    private String targetNote;

    @JsonProperty("trigger_time")
    private Long triggerTime;

    @JsonProperty("trigger_value")
    private String triggerValue;

    private List<String> tags;

    @JsonProperty("tags_map")
    private Map<String, String> tagsMap;

    private Map<String, String> annotations;

    @JsonProperty("is_recovered")
    private Boolean isRecovered;

    @JsonProperty("notify_users_obj")
    private List<Object> notifyUsersObj;

    @JsonProperty("last_eval_time")
    private Long lastEvalTime;

    @JsonProperty("last_sent_time")
    private Long lastSentTime;

    @JsonProperty("notify_cur_number")
    private Integer notifyCurNumber;

    @JsonProperty("first_trigger_time")
    private Long firstTriggerTime;

    @JsonProperty("extra_config")
    private Object extraConfig;

    private String claimant;

    @JsonProperty("sub_rule_id")
    private Long subRuleId;
}
