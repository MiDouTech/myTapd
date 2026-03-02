package com.miduo.cloud.ticket.common.dto.common;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页请求参数
 */
@Data
public class PageInput implements Serializable {

    @Min(value = 1, message = "页码不能小于1")
    private int pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private int pageSize = 20;

    private String orderBy;

    private boolean asc = false;

    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }
}
