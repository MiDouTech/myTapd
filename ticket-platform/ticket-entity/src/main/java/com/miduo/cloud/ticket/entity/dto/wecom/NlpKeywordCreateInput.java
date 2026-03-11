package com.miduo.cloud.ticket.entity.dto.wecom;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 创建NLP关键词配置请求
 * Task023：企微自然语言建单 - 关键词管理
 */
@Data
public class NlpKeywordCreateInput {

    /**
     * 关键词
     */
    @NotBlank(message = "关键词不能为空")
    @Size(max = 50, message = "关键词长度不能超过50个字符")
    private String keyword;

    /**
     * 匹配类型：1=分类 2=优先级 3=实体
     */
    @NotNull(message = "匹配类型不能为空")
    private Integer matchType;

    /**
     * 映射目标值
     */
    @NotBlank(message = "映射目标值不能为空")
    @Size(max = 100, message = "映射目标值长度不能超过100个字符")
    private String targetValue;

    /**
     * 置信度(0-100)
     */
    @NotNull(message = "置信度不能为空")
    @Min(value = 0, message = "置信度最小值为0")
    @Max(value = 100, message = "置信度最大值为100")
    private Integer confidence;

    /**
     * 排序，数值越大优先级越高
     */
    @NotNull(message = "排序值不能为空")
    private Integer sortOrder;

    /**
     * 是否启用：0否 1是
     */
    @NotNull(message = "启用状态不能为空")
    private Integer isActive;
}
