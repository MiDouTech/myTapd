package com.miduo.cloud.ticket.common.dto.common;

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页响应结果
 *
 * @param <T> 列表数据类型
 */
@Data
public class PageOutput<T> implements Serializable {

    private List<T> records;
    private long total;
    private int pageNum;
    private int pageSize;
    private int totalPages;

    public PageOutput() {
    }

    public PageOutput(List<T> records, long total, int pageNum, int pageSize) {
        this.records = records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
    }

    public static <T> PageOutput<T> empty(int pageNum, int pageSize) {
        return new PageOutput<>(Collections.emptyList(), 0, pageNum, pageSize);
    }

    public static <T> PageOutput<T> of(List<T> records, long total, int pageNum, int pageSize) {
        return new PageOutput<>(records, total, pageNum, pageSize);
    }
}
