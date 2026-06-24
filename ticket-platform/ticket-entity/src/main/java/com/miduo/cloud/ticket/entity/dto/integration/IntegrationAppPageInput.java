package com.miduo.cloud.ticket.entity.dto.integration;

import com.miduo.cloud.ticket.common.dto.common.PageInput;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 接入应用分页查询
 * 接口编号：API000527
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class IntegrationAppPageInput extends PageInput {

    private String keyword;

    private Integer status;
}
