package com.miduo.cloud.ticket.entity.dto.wecom;

import lombok.Data;

import java.util.Date;

/**
 * NLP关键词配置列表响应
 * Task023：企微自然语言建单 - 关键词管理
 */
@Data
public class NlpKeywordListOutput {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 关键词
     */
    private String keyword;

    /**
     * 匹配类型：1=分类 2=优先级 3=实体
     */
    private Integer matchType;

    /**
     * 匹配类型描述
     */
    private String matchTypeLabel;

    /**
     * 映射目标值
     */
    private String targetValue;

    /**
     * 置信度(0-100)
     */
    private Integer confidence;

    /**
     * 排序，数值越大优先级越高
     */
    private Integer sortOrder;

    /**
     * 是否启用：0否 1是
     */
    private Integer isActive;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
