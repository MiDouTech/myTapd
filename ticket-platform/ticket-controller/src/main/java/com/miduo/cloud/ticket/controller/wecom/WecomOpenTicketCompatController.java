package com.miduo.cloud.ticket.controller.wecom;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企微历史工单链接兼容控制器。
 */
@Hidden
@RestController
@RequestMapping("/api/wecom/callback/open/ticket")
public class WecomOpenTicketCompatController {

    /**
     * 兼容历史错误链接，重定向到当前公开工单详情页。
     * 接口编号：API000439
     * 产品文档功能：企业微信历史链接兼容访问
     */
    @GetMapping("/{ticketNo}")
    public ResponseEntity<Void> redirectToPublicOpenPage(@PathVariable String ticketNo) {
        HttpHeaders headers = new HttpHeaders();
        // 为什么要重定向：历史消息里的链接已发出，补一个转发口可以让老消息立刻可用。
        headers.add(HttpHeaders.LOCATION, "/open/ticket/" + ticketNo);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
