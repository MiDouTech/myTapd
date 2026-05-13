package com.miduo.cloud.ticket.application.openapi;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.miduo.cloud.ticket.common.dto.common.PageOutput;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.common.util.DateTimeRangeQueryUtil;
import com.miduo.cloud.ticket.entity.dto.openapi.OpenTicketExportOutput;
import com.miduo.cloud.ticket.entity.dto.openapi.OpenTicketExportPageInput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketCategoryMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketNodeDurationMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketTimeTrackMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketCategoryPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketNodeDurationPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketTimeTrackPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 开放接口：工单全量数据拉取应用服务
 */
@Service
public class OpenTicketExportApplicationService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TicketMapper ticketMapper;
    private final TicketCategoryMapper ticketCategoryMapper;
    private final TicketNodeDurationMapper ticketNodeDurationMapper;
    private final TicketTimeTrackMapper ticketTimeTrackMapper;
    private final SysUserMapper sysUserMapper;

    public OpenTicketExportApplicationService(TicketMapper ticketMapper,
                                              TicketCategoryMapper ticketCategoryMapper,
                                              TicketNodeDurationMapper ticketNodeDurationMapper,
                                              TicketTimeTrackMapper ticketTimeTrackMapper,
                                              SysUserMapper sysUserMapper) {
        this.ticketMapper = ticketMapper;
        this.ticketCategoryMapper = ticketCategoryMapper;
        this.ticketNodeDurationMapper = ticketNodeDurationMapper;
        this.ticketTimeTrackMapper = ticketTimeTrackMapper;
        this.sysUserMapper = sysUserMapper;
    }

    public PageOutput<OpenTicketExportOutput> pageExport(OpenTicketExportPageInput input) {
        validateTimeRange(input);

        List<String> statusList = normalizeStatuses(input.getStatuses());
        String createTimeStart = DateTimeRangeQueryUtil.normalizeRangeStart(input.getCreateTimeStart());
        String createTimeEnd = DateTimeRangeQueryUtil.normalizeRangeEndInclusive(input.getCreateTimeEnd());
        String completeTimeStart = DateTimeRangeQueryUtil.normalizeRangeStart(input.getCompleteTimeStart());
        String completeTimeEnd = DateTimeRangeQueryUtil.normalizeRangeEndInclusive(input.getCompleteTimeEnd());
        IPage<TicketPO> pageResult = ticketMapper.selectOpenTicketExportPage(
                new Page<>(input.getPageNum(), input.getPageSize()),
                createTimeStart,
                createTimeEnd,
                completeTimeStart,
                completeTimeEnd,
                statusList,
                input.getBusinessTypeId(),
                input.getBusinessTypeName()
        );
        if (pageResult.getRecords() == null || pageResult.getRecords().isEmpty()) {
            return PageOutput.empty(input.getPageNum(), input.getPageSize());
        }

        List<TicketPO> tickets = pageResult.getRecords();
        List<Long> ticketIds = tickets.stream().map(TicketPO::getId).filter(Objects::nonNull).collect(Collectors.toList());
        Set<Long> categoryIds = tickets.stream().map(TicketPO::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, String> categoryNameMap = loadCategoryNameMap(categoryIds);
        List<TicketNodeDurationPO> nodeDurations = loadNodeDurations(ticketIds);
        Map<Long, String> userNameMap = loadUserNameMap(nodeDurations);
        Map<String, String> nodeRemarkMap = loadNodeRemarkMap(ticketIds);
        Map<Long, List<OpenTicketExportOutput.ProcessNode>> nodesByTicketId =
                buildProcessNodes(nodeDurations, userNameMap, nodeRemarkMap);

        List<OpenTicketExportOutput> outputs = new ArrayList<>();
        for (TicketPO ticket : tickets) {
            OpenTicketExportOutput output = new OpenTicketExportOutput();
            output.setId(ticket.getId());
            output.setTicketNo(ticket.getTicketNo());
            output.setTitle(ticket.getTitle());
            output.setStatus(ticket.getStatus());
            TicketStatus status = TicketStatus.fromCode(ticket.getStatus());
            output.setStatusLabel(status != null ? status.getLabel() : ticket.getStatus());
            output.setCreateTime(ticket.getCreateTime());
            output.setCompleteTime(ticket.getClosedAt() != null ? ticket.getClosedAt() : ticket.getResolvedAt());
            output.setBriefDescription(ticket.getDescription());
            output.setBusinessTypeId(ticket.getCategoryId());
            output.setBusinessTypeName(categoryNameMap.get(ticket.getCategoryId()));
            output.setProcessNodes(nodesByTicketId.getOrDefault(ticket.getId(), Collections.emptyList()));
            outputs.add(output);
        }
        return PageOutput.of(outputs, pageResult.getTotal(), input.getPageNum(), input.getPageSize());
    }

    private void validateTimeRange(OpenTicketExportPageInput input) {
        boolean hasCreateRange = StringUtils.hasText(input.getCreateTimeStart()) || StringUtils.hasText(input.getCreateTimeEnd());
        boolean hasCompleteRange = StringUtils.hasText(input.getCompleteTimeStart()) || StringUtils.hasText(input.getCompleteTimeEnd());
        if (!hasCreateRange && !hasCompleteRange) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "创建时间范围和完成时间范围至少填写一组");
        }
        validateRange("创建时间", input.getCreateTimeStart(), input.getCreateTimeEnd(), hasCreateRange);
        validateRange("完成时间", input.getCompleteTimeStart(), input.getCompleteTimeEnd(), hasCompleteRange);
    }

    private void validateRange(String rangeName, String start, String end, boolean hasRange) {
        if (!hasRange) {
            return;
        }
        if (!StringUtils.hasText(start) || !StringUtils.hasText(end)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, rangeName + "范围必须同时填写开始和结束时间");
        }
        LocalDateTime startTime = parseTime(start, rangeName + "开始时间格式错误，必须是 yyyy-MM-dd HH:mm:ss");
        LocalDateTime endTime = parseTime(end, rangeName + "结束时间格式错误，必须是 yyyy-MM-dd HH:mm:ss");
        if (startTime.isAfter(endTime)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, rangeName + "开始时间不能晚于结束时间");
        }
    }

    private LocalDateTime parseTime(String text, String errorMessage) {
        try {
            return LocalDateTime.parse(text.trim(), TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, errorMessage);
        }
    }

    private List<String> normalizeStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String status : statuses) {
            if (!StringUtils.hasText(status)) {
                continue;
            }
            normalized.add(status.trim().toLowerCase(Locale.ROOT));
        }
        if (normalized.isEmpty()) {
            return null;
        }
        return new ArrayList<>(normalized);
    }

    private Map<Long, String> loadCategoryNameMap(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<TicketCategoryPO> categories = ticketCategoryMapper.selectBatchIds(categoryIds);
        if (categories == null || categories.isEmpty()) {
            return Collections.emptyMap();
        }
        return categories.stream()
                .filter(category -> category.getId() != null)
                .collect(Collectors.toMap(TicketCategoryPO::getId, TicketCategoryPO::getName, (left, right) -> left));
    }

    private List<TicketNodeDurationPO> loadNodeDurations(List<Long> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return Collections.emptyList();
        }
        return ticketNodeDurationMapper.selectList(new LambdaQueryWrapper<TicketNodeDurationPO>()
                .in(TicketNodeDurationPO::getTicketId, ticketIds)
                .orderByAsc(TicketNodeDurationPO::getArriveAt)
                .orderByAsc(TicketNodeDurationPO::getId));
    }

    private Map<Long, String> loadUserNameMap(List<TicketNodeDurationPO> nodeDurations) {
        if (nodeDurations == null || nodeDurations.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> userIds = nodeDurations.stream()
                .map(TicketNodeDurationPO::getAssigneeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUserPO> users = sysUserMapper.selectBatchIds(userIds);
        if (users == null || users.isEmpty()) {
            return Collections.emptyMap();
        }
        return users.stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(SysUserPO::getId, SysUserPO::getName, (left, right) -> left));
    }

    private Map<String, String> loadNodeRemarkMap(List<Long> ticketIds) {
        if (ticketIds == null || ticketIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<TicketTimeTrackPO> tracks = ticketTimeTrackMapper.selectList(new LambdaQueryWrapper<TicketTimeTrackPO>()
                .in(TicketTimeTrackPO::getTicketId, ticketIds)
                .isNotNull(TicketTimeTrackPO::getToStatus)
                .isNotNull(TicketTimeTrackPO::getRemark)
                .orderByDesc(TicketTimeTrackPO::getTimestamp)
                .orderByDesc(TicketTimeTrackPO::getId));
        if (tracks == null || tracks.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> remarkMap = new HashMap<>();
        for (TicketTimeTrackPO track : tracks) {
            String toStatus = normalizeStatus(track.getToStatus());
            if (track.getTicketId() == null || !StringUtils.hasText(toStatus) || !StringUtils.hasText(track.getRemark())) {
                continue;
            }
            String key = buildNodeRemarkKey(track.getTicketId(), toStatus);
            if (!remarkMap.containsKey(key)) {
                remarkMap.put(key, track.getRemark().trim());
            }
        }
        return remarkMap;
    }

    private Map<Long, List<OpenTicketExportOutput.ProcessNode>> buildProcessNodes(
            List<TicketNodeDurationPO> nodeDurations,
            Map<Long, String> userNameMap,
            Map<String, String> nodeRemarkMap) {
        if (nodeDurations == null || nodeDurations.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<OpenTicketExportOutput.ProcessNode>> result = new LinkedHashMap<>();
        for (TicketNodeDurationPO duration : nodeDurations) {
            if (duration.getTicketId() == null) {
                continue;
            }
            OpenTicketExportOutput.ProcessNode node = new OpenTicketExportOutput.ProcessNode();
            String nodeCode = normalizeStatus(duration.getNodeName());
            node.setNodeName(nodeCode);
            TicketStatus nodeStatus = TicketStatus.fromCode(nodeCode);
            node.setNodeLabel(nodeStatus != null ? nodeStatus.getLabel() : duration.getNodeName());
            node.setEnterTime(duration.getArriveAt());
            node.setLeaveTime(duration.getLeaveAt());
            node.setProcessDurationSec(duration.getProcessDurationSec());
            node.setHandlerName(userNameMap.get(duration.getAssigneeId()));
            node.setRemark(nodeRemarkMap.get(buildNodeRemarkKey(duration.getTicketId(), nodeCode)));
            result.computeIfAbsent(duration.getTicketId(), key -> new ArrayList<>()).add(node);
        }
        return result;
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        return status.trim().toLowerCase(Locale.ROOT);
    }

    private String buildNodeRemarkKey(Long ticketId, String nodeCode) {
        return ticketId + "#" + nodeCode;
    }
}
