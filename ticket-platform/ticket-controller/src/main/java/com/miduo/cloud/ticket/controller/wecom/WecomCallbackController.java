package com.miduo.cloud.ticket.controller.wecom;

import com.miduo.cloud.ticket.application.wecom.WecomCallbackApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 企微回调入口
 */
@Tag(name = "企业微信回调", description = "企微URL验证与消息接收")
@RestController
@RequestMapping("/api/wecom")
public class WecomCallbackController {

    private static final Logger log = LoggerFactory.getLogger(WecomCallbackController.class);

    private final WecomCallbackApplicationService callbackApplicationService;

    public WecomCallbackController(WecomCallbackApplicationService callbackApplicationService) {
        this.callbackApplicationService = callbackApplicationService;
    }

    /**
     * 企微回调URL验证
     * 接口编号：API000020
     * 产品文档功能：4.6.4 企微群机器人工单 - 回调URL验证
     */
    @Operation(summary = "企微回调URL验证", description = "接口编号：API000020")
    @GetMapping(value = "/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public String verifyUrl(@RequestParam("msg_signature") String msgSignature,
                            @RequestParam("timestamp") String timestamp,
                            @RequestParam("nonce") String nonce,
                            @RequestParam("echostr") String echostr) {
        try {
            return callbackApplicationService.verifyUrl(msgSignature, timestamp, nonce, echostr);
        } catch (Exception ex) {
            log.error("企微URL验证失败", ex);
            return "failed";
        }
    }

    /**
     * 企微回调消息接收
     * 接口编号：API000020
     * 产品文档功能：4.6.4 企微群机器人工单 - 消息接收与异步处理
     */
    @Operation(summary = "企微回调消息接收", description = "接口编号：API000020")
    @PostMapping(
            value = "/callback",
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.ALL_VALUE},
            produces = MediaType.TEXT_PLAIN_VALUE
    )
    public String receiveMessage(@RequestParam("msg_signature") String msgSignature,
                                 @RequestParam("timestamp") String timestamp,
                                 @RequestParam("nonce") String nonce,
                                 @RequestBody String body) {
        try {
            callbackApplicationService.receiveCallback(msgSignature, timestamp, nonce, body);
            return "success";
        } catch (Exception ex) {
            log.error("企微回调消息处理失败", ex);
            return "failed";
        }
    }
}
