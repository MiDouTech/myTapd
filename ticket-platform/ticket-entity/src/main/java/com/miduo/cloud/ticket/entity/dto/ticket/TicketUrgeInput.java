package com.miduo.cloud.ticket.entity.dto.ticket;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 工单催办请求：在默认通知当前处理人的基础上，可追加通知人
 */
@Data
public class TicketUrgeInput implements Serializable {

    /**
     * 额外通知用户 ID（不含默认处理人；与处理人去重后一并发送）
     */
    private List<Long> extraNotifyUserIds = new ArrayList<>();
}
