package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.wecom.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 企微自然语言解析关键词配置持久化对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wecom_nlp_keyword")
public class WecomNlpKeywordPO extends BaseEntity {

    /**
     * 关键词
     */
    @TableField("keyword")
    private String keyword;

    /**
     * 匹配类型：1=分类 2=优先级 3=实体
     */
    @TableField("match_type")
    private Integer matchType;

    /**
     * 映射目标值（分类路径/优先级枚举/实体类型）
     */
    @TableField("target_value")
    private String targetValue;

    /**
     * 置信度(0-100)
     */
    @TableField("confidence")
    private Integer confidence;

    /**
     * 排序，数值越大优先级越高
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 是否启用：0否 1是
     */
    @TableField("is_active")
    private Integer isActive;
}
