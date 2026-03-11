package com.miduo.cloud.ticket.entity.dto.wecom;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * NLP解析日志分页查询请求
 * Task023：企微自然语言建单 - 解析日志
 */
@Data
public class NlpLogPageInput {

    /**
     * 页码，从1开始
     */
    @Min(value = 1, message = "页码最小值为1")
    private int pageNum = 1;

    /**
     * 每页条数
     */
    @Min(value = 1, message = "每页条数最小值为1")
    @Max(value = 100, message = "每页条数最大值为100")
    private int pageSize = 20;

    /**
     * 解析类型：template=格式模板 natural_language=自然语言
     */
    private String parseType;

    /**
     * 群ChatID
     */
    private String chatId;

    /**
     * 发送人企微UserID
     */
    private String fromWecomUserid;

    /**
     * 最低置信度过滤
     */
    @Min(value = 0, message = "置信度最小值为0")
    @Max(value = 100, message = "置信度最大值为100")
    private Integer minConfidence;

    /**
     * 开始时间（yyyy-MM-dd HH:mm:ss）
     */
    private String startTime;

    /**
     * 结束时间（yyyy-MM-dd HH:mm:ss）
     */
    private String endTime;
}
