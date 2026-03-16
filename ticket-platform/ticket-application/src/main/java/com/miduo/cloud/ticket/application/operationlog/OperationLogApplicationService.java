package com.miduo.cloud.ticket.application.operationlog;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.common.enums.AppCodeEnum;
import com.miduo.cloud.ticket.common.enums.ExecuteResultEnum;
import com.miduo.cloud.ticket.common.enums.LogLevelEnum;
import com.miduo.cloud.ticket.entity.dto.operationlog.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.operationlog.mapper.OperationLogMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.operationlog.po.OperationLogPO;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志应用服务
 * 接口编号段：API000600–API000605
 */
@Service
public class OperationLogApplicationService extends BaseApplicationService {

    private final OperationLogMapper operationLogMapper;

    public OperationLogApplicationService(OperationLogMapper operationLogMapper) {
        this.operationLogMapper = operationLogMapper;
    }

    /**
     * 保存操作日志（由AOP切面异步调用）
     */
    public void saveLog(OperationLogPO logPO) {
        operationLogMapper.insert(logPO);
    }

    /**
     * 多条件分页查询操作日志
     * 接口编号：API000600
     */
    public PageOutput<OperationLogListOutput> page(OperationLogPageInput input) {
        Page<OperationLogPO> page = new Page<>(input.getPageNum(), input.getPageSize());
        IPage<OperationLogPO> result = operationLogMapper.selectOperationLogPage(
                page,
                input.getAccountId(),
                trimOrNull(input.getOperatorName()),
                trimOrNull(input.getOperatorIp()),
                trimOrNull(input.getLogLevel()),
                trimOrNull(input.getAppCode()),
                trimOrNull(input.getModuleName()),
                trimOrNull(input.getOperationItem()),
                trimOrNull(input.getOperationDetail()),
                trimOrNull(input.getExecuteResult()),
                trimOrNull(input.getStartTime()),
                trimOrNull(input.getEndTime()),
                trimOrNull(input.getSortField()),
                trimOrNull(input.getSortOrder())
        );
        List<OperationLogListOutput> outputs = result.getRecords().stream()
                .map(this::convertToListOutput)
                .collect(Collectors.toList());
        return PageOutput.of(outputs, result.getTotal(), input.getPageNum(), input.getPageSize());
    }

    /**
     * 获取操作日志详情
     * 接口编号：API000601
     */
    public OperationLogDetailOutput detail(Long id) {
        OperationLogPO po = operationLogMapper.selectById(id);
        if (po == null) {
            return null;
        }
        return convertToDetailOutput(po);
    }

    /**
     * 获取日志统计概览（今日数据）
     * 接口编号：API000602
     */
    public OperationLogStatisticsOutput statistics() {
        OperationLogStatisticsOutput output = new OperationLogStatisticsOutput();
        output.setTodayTotalCount(operationLogMapper.countTodayTotal());
        output.setTodayFailureCount(operationLogMapper.countTodayFailure());
        output.setTodayActiveUserCount(operationLogMapper.countTodayActiveUsers());
        output.setTodaySecurityAlertCount(operationLogMapper.countTodaySecurityAlert());
        return output;
    }

    /**
     * 获取操作模块枚举列表
     * 接口编号：API000604
     */
    public List<String> listModuleNames() {
        return operationLogMapper.selectDistinctModuleNames();
    }

    /**
     * 获取所属应用枚举列表
     * 接口编号：API000605
     */
    public List<AppCodeOutput> listAppCodes() {
        return Arrays.stream(AppCodeEnum.values())
                .map(e -> new AppCodeOutput(e.getCode(), e.getAppName()))
                .collect(Collectors.toList());
    }

    private OperationLogListOutput convertToListOutput(OperationLogPO po) {
        OperationLogListOutput output = new OperationLogListOutput();
        output.setId(po.getId());
        output.setOperateTime(po.getOperateTime());
        output.setAccountId(po.getAccountId());
        output.setOperatorName(po.getOperatorName());
        output.setOperatorIp(po.getOperatorIp());
        output.setLogLevel(po.getLogLevel());
        LogLevelEnum logLevel = LogLevelEnum.fromCode(po.getLogLevel());
        output.setLogLevelDesc(logLevel.getDesc());
        output.setAppCode(po.getAppCode());
        output.setAppName(po.getAppName());
        output.setModuleName(po.getModuleName());
        output.setRequestPath(po.getRequestPath());
        output.setOperationItem(po.getOperationItem());
        output.setExecuteResult(po.getExecuteResult());
        ExecuteResultEnum executeResult = ExecuteResultEnum.fromCode(po.getExecuteResult());
        output.setExecuteResultDesc(executeResult.getDesc());
        return output;
    }

    private OperationLogDetailOutput convertToDetailOutput(OperationLogPO po) {
        OperationLogDetailOutput output = new OperationLogDetailOutput();
        output.setId(po.getId());
        output.setOperateTime(po.getOperateTime());
        output.setAccountId(po.getAccountId());
        output.setOperatorName(po.getOperatorName());
        output.setOperatorIp(po.getOperatorIp());
        output.setUserAgent(po.getUserAgent());
        output.setLogLevel(po.getLogLevel());
        LogLevelEnum logLevel = LogLevelEnum.fromCode(po.getLogLevel());
        output.setLogLevelDesc(logLevel.getDesc());
        output.setAppCode(po.getAppCode());
        output.setAppName(po.getAppName());
        output.setModuleName(po.getModuleName());
        output.setRequestPath(po.getRequestPath());
        output.setRequestMethod(po.getRequestMethod());
        output.setOperationItem(po.getOperationItem());
        output.setRequestParams(po.getRequestParams());
        output.setExecuteResult(po.getExecuteResult());
        ExecuteResultEnum executeResult = ExecuteResultEnum.fromCode(po.getExecuteResult());
        output.setExecuteResultDesc(executeResult.getDesc());
        output.setCostMillis(po.getCostMillis());
        output.setChangeRecords(parseChangeRecords(po.getChangeRecords()));
        output.setErrorCode(po.getErrorCode());
        output.setErrorMessage(po.getErrorMessage());
        output.setErrorStack(po.getErrorStack());
        return output;
    }

    private List<ChangeRecordItem> parseChangeRecords(String changeRecordsJson) {
        if (changeRecordsJson == null || changeRecordsJson.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(changeRecordsJson, new TypeReference<List<ChangeRecordItem>>() {});
        } catch (Exception e) {
            log.warn("解析变更记录JSON失败: {}", changeRecordsJson, e);
            return null;
        }
    }

    private String trimOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
