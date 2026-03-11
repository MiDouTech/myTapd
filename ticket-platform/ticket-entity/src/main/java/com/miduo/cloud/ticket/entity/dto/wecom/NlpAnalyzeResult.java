package com.miduo.cloud.ticket.entity.dto.wecom;

import lombok.Data;

import java.util.Map;

/**
 * NLP自然语言解析结果（内部传递，非对外接口DTO）
 * Task023：企微自然语言建单 - 内部解析结果封装
 */
@Data
public class NlpAnalyzeResult {

    /**
     * 解析出的分类路径（置信度不足时为空）
     */
    private String categoryPath;

    /**
     * 解析出的优先级（urgent/high/medium/low）
     */
    private String priority;

    /**
     * 整体置信度(0-100)
     */
    private Integer confidence;

    /**
     * 生成的工单标题（截取前50字符）
     */
    private String title;

    /**
     * 原始文本内容
     */
    private String rawText;

    /**
     * 提取到的实体（商户编号、手机号等）
     */
    private Map<String, String> entities;
}
