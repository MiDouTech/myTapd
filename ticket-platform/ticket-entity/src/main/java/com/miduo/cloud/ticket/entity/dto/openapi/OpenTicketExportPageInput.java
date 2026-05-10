package com.miduo.cloud.ticket.entity.dto.openapi;

import com.miduo.cloud.ticket.common.dto.common.PageInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 外部系统工单数据拉取分页入参
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OpenTicketExportPageInput extends PageInput {

    /**
     * 创建时间范围起始（yyyy-MM-dd HH:mm:ss）
     */
    private String createTimeStart;

    /**
     * 创建时间范围结束（yyyy-MM-dd HH:mm:ss）
     */
    private String createTimeEnd;

    /**
     * 完成时间范围起始（yyyy-MM-dd HH:mm:ss）
     */
    private String completeTimeStart;

    /**
     * 完成时间范围结束（yyyy-MM-dd HH:mm:ss）
     */
    private String completeTimeEnd;

    /**
     * 工单状态筛选
     */
    private List<String> statuses;

    /**
     * 业务类型ID（对应工单分类ID）
     */
    private Long businessTypeId;

    /**
     * 业务类型名称（对应工单分类名，模糊匹配）
     */
    private String businessTypeName;
}
