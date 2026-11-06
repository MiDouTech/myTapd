package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 新增工单评论输入
 * 接口编号：API000508
 */
@Data
public class TicketCommentInput implements Serializable {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 3000000, message = "评论正文和图片总量过大")
    private String content;

    /** 缺省为 INTERNAL；客户插件由可信服务入口固定为 PUBLIC。 */
    private String visibility;

    /** 内部旧调用与 Agent 缺省仍为纯文本；编辑器显式传 HTML。 */
    private String contentFormat;

    /** 内部图片随 HTML 正文内嵌；拒绝另外提交的公开附件。 */
    private List<Object> attachments;

    /**
     * 评论中 @ 的用户主键列表（可选）；服务端仅对在职且存在于系统中的用户发通知
     */
    @Size(max = 20, message = "最多@提醒20人")
    private List<Long> mentionedUserIds;
}
