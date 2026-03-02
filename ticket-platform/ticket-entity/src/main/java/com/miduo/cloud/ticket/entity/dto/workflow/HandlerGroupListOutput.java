package com.miduo.cloud.ticket.entity.dto.workflow;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 处理组列表输出
 */
@Data
public class HandlerGroupListOutput implements Serializable {

    private Long id;
    private String name;
    private String description;
    private String skillTags;
    private Integer isActive;
    private Long leaderId;
    private String leaderName;
    private Integer memberCount;
    private List<MemberItem> members;
    private Date createTime;

    @Data
    public static class MemberItem implements Serializable {
        private Long userId;
        private String userName;
    }
}
