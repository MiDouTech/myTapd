package com.miduo.cloud.ticket.entity.dto.bugreport;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 逻辑归因树节点
 */
@Data
public class LogicCauseTreeOutput implements Serializable {

    private Long id;

    private String name;

    private List<LogicCauseTreeOutput> children;
}
