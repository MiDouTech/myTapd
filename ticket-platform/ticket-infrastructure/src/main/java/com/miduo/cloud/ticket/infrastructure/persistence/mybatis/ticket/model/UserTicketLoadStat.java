package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户工单负载统计（用于负载均衡分派）
 */
@Data
public class UserTicketLoadStat implements Serializable {

    /** 用户ID */
    private Long userId;

    /** 未完成工单数量 */
    private Long activeCount;
}
