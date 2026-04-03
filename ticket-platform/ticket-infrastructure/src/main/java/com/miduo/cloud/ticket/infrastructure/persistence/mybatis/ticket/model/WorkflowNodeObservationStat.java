package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 工作流节点运行观察聚合统计
 */
@Data
public class WorkflowNodeObservationStat implements Serializable {

    private String nodeName;

    private Long enterCount;

    private Long avgWaitDurationSec;

    private Long avgProcessDurationSec;

    private Long avgTotalDurationSec;

    private Long maxTotalDurationSec;
}
