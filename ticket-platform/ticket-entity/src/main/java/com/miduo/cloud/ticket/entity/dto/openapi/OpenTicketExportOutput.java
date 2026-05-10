package com.miduo.cloud.ticket.entity.dto.openapi;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 外部系统工单数据拉取出参
 */
@Data
public class OpenTicketExportOutput implements Serializable {

    private Long id;
    private String ticketNo;
    private String title;
    private String status;
    private String statusLabel;
    private Date createTime;
    private Date completeTime;
    private String briefDescription;
    private Long businessTypeId;
    private String businessTypeName;
    private List<ProcessNode> processNodes;

    @Data
    public static class ProcessNode implements Serializable {
        private String nodeName;
        private String nodeLabel;
        private Date enterTime;
        private Date leaveTime;
        private Long processDurationSec;
        private String handlerName;
        private String remark;
    }
}
