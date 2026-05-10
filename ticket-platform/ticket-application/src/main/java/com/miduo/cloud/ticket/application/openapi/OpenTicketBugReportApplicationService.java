package com.miduo.cloud.ticket.application.openapi;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.common.enums.BugReportStatus;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.openapi.OpenTicketBugReportOutput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.mapper.BugReportMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.mapper.BugReportResponsibleMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.mapper.BugReportTicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po.BugReportPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po.BugReportResponsiblePO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.bugreport.po.BugReportTicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 开放接口：按工单编号查询 Bug 简报应用服务
 */
@Service
public class OpenTicketBugReportApplicationService {

    private final TicketMapper ticketMapper;
    private final BugReportTicketMapper bugReportTicketMapper;
    private final BugReportMapper bugReportMapper;
    private final BugReportResponsibleMapper bugReportResponsibleMapper;
    private final SysUserMapper sysUserMapper;

    public OpenTicketBugReportApplicationService(TicketMapper ticketMapper,
                                                 BugReportTicketMapper bugReportTicketMapper,
                                                 BugReportMapper bugReportMapper,
                                                 BugReportResponsibleMapper bugReportResponsibleMapper,
                                                 SysUserMapper sysUserMapper) {
        this.ticketMapper = ticketMapper;
        this.bugReportTicketMapper = bugReportTicketMapper;
        this.bugReportMapper = bugReportMapper;
        this.bugReportResponsibleMapper = bugReportResponsibleMapper;
        this.sysUserMapper = sysUserMapper;
    }

    public OpenTicketBugReportOutput detailByTicketNo(String ticketNo) {
        TicketPO ticket = findTicketByNo(ticketNo);

        OpenTicketBugReportOutput output = new OpenTicketBugReportOutput();
        output.setTicketNo(ticket.getTicketNo());
        output.setBugReport(buildLatestArchivedReport(ticket.getId()));
        return output;
    }

    private TicketPO findTicketByNo(String ticketNo) {
        if (!StringUtils.hasText(ticketNo)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "工单编号不能为空");
        }
        TicketPO ticket = ticketMapper.selectOne(new LambdaQueryWrapper<TicketPO>()
                .eq(TicketPO::getTicketNo, ticketNo.trim())
                .last("LIMIT 1"));
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }
        return ticket;
    }

    private OpenTicketBugReportOutput.ArchivedBugReport buildLatestArchivedReport(Long ticketId) {
        if (ticketId == null) {
            return null;
        }
        List<BugReportTicketPO> reportLinks = bugReportTicketMapper.selectList(
                new LambdaQueryWrapper<BugReportTicketPO>()
                        .eq(BugReportTicketPO::getTicketId, ticketId)
                        .orderByDesc(BugReportTicketPO::getCreateTime)
        );
        if (reportLinks == null || reportLinks.isEmpty()) {
            return null;
        }

        Set<Long> reportIds = reportLinks.stream()
                .map(BugReportTicketPO::getReportId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (reportIds.isEmpty()) {
            return null;
        }
        List<BugReportPO> reports = bugReportMapper.selectBatchIds(reportIds);
        if (reports == null || reports.isEmpty()) {
            return null;
        }
        Map<Long, BugReportPO> reportMap = reports.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(BugReportPO::getId, item -> item, (left, right) -> left));

        BugReportPO latestArchivedReport = null;
        for (BugReportTicketPO link : reportLinks) {
            BugReportPO report = reportMap.get(link.getReportId());
            if (report != null && BugReportStatus.ARCHIVED.getCode().equals(report.getStatus())) {
                latestArchivedReport = report;
                break;
            }
        }
        if (latestArchivedReport == null) {
            return null;
        }

        OpenTicketBugReportOutput.ArchivedBugReport output = new OpenTicketBugReportOutput.ArchivedBugReport();
        output.setId(latestArchivedReport.getId());
        output.setReportNo(latestArchivedReport.getReportNo());
        output.setStatus(latestArchivedReport.getStatus());
        BugReportStatus status = BugReportStatus.fromCode(latestArchivedReport.getStatus());
        output.setStatusLabel(status != null ? status.getLabel() : latestArchivedReport.getStatus());
        output.setDefectCategory(latestArchivedReport.getDefectCategory());
        output.setSeverityLevel(latestArchivedReport.getSeverityLevel());
        output.setLogicCauseLevel1(latestArchivedReport.getLogicCauseLevel1());
        output.setLogicCauseLevel2(latestArchivedReport.getLogicCauseLevel2());
        output.setLogicCauseDetail(latestArchivedReport.getLogicCauseDetail());
        output.setProblemDesc(latestArchivedReport.getProblemDesc());
        output.setImpactScope(latestArchivedReport.getImpactScope());
        output.setSolution(latestArchivedReport.getSolution());
        output.setTempSolution(latestArchivedReport.getTempSolution());
        output.setIntroducedProject(latestArchivedReport.getIntroducedProject());
        output.setStartDate(latestArchivedReport.getStartDate());
        output.setTempResolveDate(latestArchivedReport.getTempResolveDate());
        output.setResolveDate(latestArchivedReport.getResolveDate());
        output.setReviewedAt(latestArchivedReport.getReviewedAt());
        output.setUpdateTime(latestArchivedReport.getUpdateTime());
        output.setReporterName(resolveReporterName(latestArchivedReport.getReporterId()));
        output.setResponsibleUserNames(resolveResponsibleUserNames(latestArchivedReport.getId()));
        return output;
    }

    private String resolveReporterName(Long reporterId) {
        if (reporterId == null) {
            return null;
        }
        SysUserPO reporter = sysUserMapper.selectById(reporterId);
        if (reporter == null || !StringUtils.hasText(reporter.getName())) {
            return null;
        }
        return reporter.getName().trim();
    }

    private String resolveResponsibleUserNames(Long reportId) {
        if (reportId == null) {
            return null;
        }
        List<BugReportResponsiblePO> responsibles = bugReportResponsibleMapper.selectList(
                new LambdaQueryWrapper<BugReportResponsiblePO>()
                        .eq(BugReportResponsiblePO::getReportId, reportId)
                        .orderByAsc(BugReportResponsiblePO::getCreateTime)
        );
        if (responsibles == null || responsibles.isEmpty()) {
            return null;
        }
        Set<Long> userIds = responsibles.stream()
                .map(BugReportResponsiblePO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (userIds.isEmpty()) {
            return null;
        }
        List<SysUserPO> users = sysUserMapper.selectBatchIds(userIds);
        if (users == null || users.isEmpty()) {
            return null;
        }
        Map<Long, String> userNameMap = users.stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(SysUserPO::getId, SysUserPO::getName, (left, right) -> left));
        String names = responsibles.stream()
                .map(BugReportResponsiblePO::getUserId)
                .map(userNameMap::get)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining("、"));
        return StringUtils.hasText(names) ? names : null;
    }
}
