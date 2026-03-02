package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 看板拖拽移动请求
 */
@Data
public class KanbanMoveInput implements Serializable {

    @NotNull(message = "工单ID不能为空")
    private Long ticketId;

    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;

    private String remark;
}
