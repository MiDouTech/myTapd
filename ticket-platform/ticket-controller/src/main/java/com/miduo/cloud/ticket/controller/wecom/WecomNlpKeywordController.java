package com.miduo.cloud.ticket.controller.wecom;

import com.miduo.cloud.ticket.application.wecom.WecomNlpKeywordService;
import com.miduo.cloud.ticket.application.wecom.WecomNlpLogService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.entity.dto.wecom.NlpKeywordCreateInput;
import com.miduo.cloud.ticket.entity.dto.wecom.NlpKeywordListOutput;
import com.miduo.cloud.ticket.entity.dto.wecom.NlpKeywordUpdateInput;
import com.miduo.cloud.ticket.entity.dto.wecom.NlpLogPageInput;
import com.miduo.cloud.ticket.entity.dto.wecom.NlpLogPageOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 企微NLP关键词配置管理与解析日志查询
 * Task023：企微文本消息自动创建工单
 */
@Tag(name = "企微NLP关键词", description = "自然语言建单关键词规则配置与解析日志")
@RestController
@RequestMapping("/api/wecom")
public class WecomNlpKeywordController {

    private final WecomNlpKeywordService nlpKeywordService;
    private final WecomNlpLogService nlpLogService;

    public WecomNlpKeywordController(WecomNlpKeywordService nlpKeywordService,
                                      WecomNlpLogService nlpLogService) {
        this.nlpKeywordService = nlpKeywordService;
        this.nlpLogService = nlpLogService;
    }

    /**
     * 查询NLP关键词配置列表
     * 接口编号：API000432
     * 产品文档功能：企微自然语言建单 - 关键词管理
     */
    @Operation(summary = "查询NLP关键词配置列表", description = "接口编号：API000432")
    @GetMapping("/nlp-keyword/list")
    public ApiResult<List<NlpKeywordListOutput>> listKeywords(
            @RequestParam(required = false) Integer matchType) {
        return ApiResult.success(nlpKeywordService.listKeywords(matchType));
    }

    /**
     * 创建NLP关键词配置
     * 接口编号：API000433
     * 产品文档功能：企微自然语言建单 - 新增关键词
     */
    @Operation(summary = "创建NLP关键词配置", description = "接口编号：API000433")
    @PostMapping("/nlp-keyword/create")
    public ApiResult<Long> createKeyword(@Valid @RequestBody NlpKeywordCreateInput input) {
        Long id = nlpKeywordService.createKeyword(input);
        return ApiResult.success(id);
    }

    /**
     * 更新NLP关键词配置
     * 接口编号：API000434
     * 产品文档功能：企微自然语言建单 - 修改关键词
     */
    @Operation(summary = "更新NLP关键词配置", description = "接口编号：API000434")
    @PutMapping("/nlp-keyword/update/{id}")
    public ApiResult<Void> updateKeyword(@PathVariable("id") Long id,
                                          @Valid @RequestBody NlpKeywordUpdateInput input) {
        nlpKeywordService.updateKeyword(id, input);
        return ApiResult.success();
    }

    /**
     * 删除NLP关键词配置
     * 接口编号：API000435
     * 产品文档功能：企微自然语言建单 - 删除关键词
     */
    @Operation(summary = "删除NLP关键词配置", description = "接口编号：API000435")
    @DeleteMapping("/nlp-keyword/delete/{id}")
    public ApiResult<Void> deleteKeyword(@PathVariable("id") Long id) {
        nlpKeywordService.deleteKeyword(id);
        return ApiResult.success();
    }

    /**
     * NLP解析日志分页查询
     * 接口编号：API000436
     * 产品文档功能：企微自然语言建单 - 解析日志
     */
    @Operation(summary = "NLP解析日志分页查询", description = "接口编号：API000436")
    @GetMapping("/nlp-log/page")
    public ApiResult<PageOutput<NlpLogPageOutput>> pageNlpLogs(@Valid NlpLogPageInput input) {
        return ApiResult.success(nlpLogService.pageNlpLogs(input));
    }
}
