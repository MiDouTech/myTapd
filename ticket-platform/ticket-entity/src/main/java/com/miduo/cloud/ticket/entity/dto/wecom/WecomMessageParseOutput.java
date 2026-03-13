package com.miduo.cloud.ticket.entity.dto.wecom;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 企微消息自然语言解析出参
 * 接口编号：API000504
 * 产品文档功能：客服信息Tab - 企微消息一键解析赋值
 */
@Data
public class WecomMessageParseOutput implements Serializable {

    /**
     * 商户编号
     */
    private String merchantNo;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 商户账号
     */
    private String merchantAccount;

    /**
     * 场景码
     */
    private String sceneCode;

    /**
     * 问题描述
     */
    private String problemDesc;

    /**
     * 预期结果
     */
    private String expectedResult;

    /**
     * 问题截图URL（逗号分隔）
     */
    private String problemScreenshot;

    /**
     * 成功匹配并赋值的字段名列表（用于前端高亮提示）
     */
    private List<String> matchedFields;

    /**
     * 解析置信度（0-100），越高表示解析越准确
     */
    private Integer confidence;
}
