package com.miduo.cloud.ticket.infrastructure.persistence.mybatis.operationlog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.operationlog.po.OperationLogPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作日志Mapper
 * 接口编号段：API000600–API000605
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLogPO> {

    /**
     * 多条件分页查询操作日志
     * 接口编号：API000600
     */
    IPage<OperationLogPO> selectOperationLogPage(
            Page<OperationLogPO> page,
            @Param("accountId") Long accountId,
            @Param("operatorName") String operatorName,
            @Param("operatorIp") String operatorIp,
            @Param("logLevel") String logLevel,
            @Param("appCode") String appCode,
            @Param("moduleName") String moduleName,
            @Param("operationItem") String operationItem,
            @Param("operationDetail") String operationDetail,
            @Param("executeResult") String executeResult,
            @Param("startTime") String startTime,
            @Param("endTime") String endTime,
            @Param("sortField") String sortField,
            @Param("sortOrder") String sortOrder);

    /**
     * 查询今日操作总数
     */
    long countTodayTotal();

    /**
     * 查询今日失败操作数
     */
    long countTodayFailure();

    /**
     * 查询今日活跃操作人数（distinct account_id）
     */
    long countTodayActiveUsers();

    /**
     * 查询今日安全告警数（log_level = SECURITY）
     */
    long countTodaySecurityAlert();

    /**
     * 查询所有不重复的模块名称列表
     */
    List<String> selectDistinctModuleNames();
}
