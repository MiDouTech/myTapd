package com.miduo.cloud.ticket.application.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.constants.RedisKeyConstants;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.TicketAction;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.domain.common.event.TicketTimeTrackRecordedEvent;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketNodeDurationOutput;
import com.miduo.cloud.ticket.entity.dto.ticket.TicketTimeTrackOutput;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketNodeDurationMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.TicketTimeTrackMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketNodeDurationPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.TicketTimeTrackPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.po.SysUserPO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 工单时间追踪应用服务
 */
@Service
public class TicketTimeTrackApplicationService extends BaseApplicationService {

    private static final Set<String> START_PROCESS_STATUS = new HashSet<>(Arrays.asList("TESTING", "DEVELOPING", "PROCESSING"));
    private static final Set<String> TERMINAL_STATUS = new HashSet<>(Arrays.asList("COMPLETED", "CLOSED"));

    private final TicketMapper ticketMapper;
    private final TicketTimeTrackMapper timeTrackMapper;
    private final TicketNodeDurationMapper nodeDurationMapper;
    private final SysUserMapper sysUserMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate stringRedisTemplate;

    public TicketTimeTrackApplicationService(TicketMapper ticketMapper,
                                             TicketTimeTrackMapper timeTrackMapper,
                                             TicketNodeDurationMapper nodeDurationMapper,
                                             SysUserMapper sysUserMapper,
                                             ApplicationEventPublisher eventPublisher,
                                             StringRedisTemplate stringRedisTemplate) {
        this.ticketMapper = ticketMapper;
        this.timeTrackMapper = timeTrackMapper;
        this.nodeDurationMapper = nodeDurationMapper;
        this.sysUserMapper = sysUserMapper;
        this.eventPublisher = eventPublisher;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordCreate(Long ticketId, Long userId, String toStatus, String remark) {
        TicketPO ticket = requireTicket(ticketId);
        saveTrack(buildTrack(ticket, userId, TicketAction.CREATE.getCode(),
                null, toStatus, null, ticket.getAssigneeId(), remark, null, new Date()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordAssign(Long ticketId, Long userId, Long fromUserId, Long toUserId,
                             String fromStatus, String toStatus, String remark) {
        TicketPO ticket = requireTicket(ticketId);
        saveTrack(buildTrack(ticket, userId, TicketAction.ASSIGN.getCode(),
                fromStatus, toStatus, fromUserId, toUserId, remark, null, new Date()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordTransfer(Long ticketId, Long userId, Long fromUserId, Long toUserId,
                               String status, String remark) {
        TicketPO ticket = requireTicket(ticketId);
        saveTrack(buildTrack(ticket, userId, TicketAction.TRANSFER.getCode(),
                status, status, fromUserId, toUserId, remark, null, new Date()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordReturn(Long ticketId, Long userId, String fromStatus, String toStatus,
                             Long fromUserId, Long toUserId, String remark) {
        TicketPO ticket = requireTicket(ticketId);
        saveTrack(buildTrack(ticket, userId, TicketAction.RETURN.getCode(),
                fromStatus, toStatus, fromUserId, toUserId, remark, null, new Date()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordStatusTrack(Long ticketId, Long userId, String action,
                                  String fromStatus, String toStatus,
                                  Long fromUserId, Long toUserId, String remark) {
        TicketPO ticket = requireTicket(ticketId);
        saveTrack(buildTrack(ticket, userId, action, fromStatus, toStatus,
                fromUserId, toUserId, remark, null, new Date()));
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean recordReadTrack(Long ticketId, Long userId) {
        TicketPO ticket = requireTicket(ticketId);
        String currentStatus = normalizeStatus(ticket.getStatus());
        String deduplicateKey = RedisKeyConstants.TRACK_READ_PREFIX + ticketId + ":" + currentStatus + ":" + userId;
        if (!shouldRecordRead(deduplicateKey)) {
            return false;
        }

        Long count = timeTrackMapper.selectCount(new LambdaQueryWrapper<TicketTimeTrackPO>()
                .eq(TicketTimeTrackPO::getTicketId, ticketId)
                .eq(TicketTimeTrackPO::getAction, TicketAction.READ.getCode())
                .eq(TicketTimeTrackPO::getToStatus, currentStatus));
        int isFirstRead = (count == null || count == 0) ? 1 : 0;

        saveTrack(buildTrack(ticket, userId, TicketAction.READ.getCode(),
                currentStatus, currentStatus, null, userId, null, isFirstRead, new Date()));
        return true;
    }

    public String resolveTransitionAction(String fromStatus, String toStatus) {
        String normalizedFrom = normalizeStatus(fromStatus);
        String normalizedTo = normalizeStatus(toStatus);
        if (TERMINAL_STATUS.contains(normalizedTo)) {
            return TicketAction.COMPLETE.getCode();
        }
        if (isStartProcessTransition(normalizedFrom, normalizedTo)) {
            return TicketAction.START_PROCESS.getCode();
        }
        return TicketAction.ESCALATE.getCode();
    }

    public TicketTimeTrackOutput getTimeTrack(Long ticketId) {
        requireTicket(ticketId);
        List<TicketTimeTrackPO> tracks = timeTrackMapper.selectList(new LambdaQueryWrapper<TicketTimeTrackPO>()
                .eq(TicketTimeTrackPO::getTicketId, ticketId)
                .orderByAsc(TicketTimeTrackPO::getTimestamp)
                .orderByAsc(TicketTimeTrackPO::getId));

        Set<Long> userIds = new HashSet<>();
        for (TicketTimeTrackPO track : tracks) {
            if (track.getUserId() != null) {
                userIds.add(track.getUserId());
            }
            if (track.getFromUserId() != null) {
                userIds.add(track.getFromUserId());
            }
            if (track.getToUserId() != null) {
                userIds.add(track.getToUserId());
            }
        }

        Map<Long, String> userNameMap = loadUserNames(userIds);
        List<TicketTimeTrackOutput.TrackItem> items = tracks.stream()
                .map(track -> convertTrackItem(track, userNameMap))
                .collect(Collectors.toList());

        TicketTimeTrackOutput output = new TicketTimeTrackOutput();
        output.setTicketId(ticketId);
        output.setTracks(items);
        return output;
    }

    public TicketNodeDurationOutput getNodeDuration(Long ticketId) {
        requireTicket(ticketId);
        List<TicketNodeDurationPO> nodes = nodeDurationMapper.selectList(new LambdaQueryWrapper<TicketNodeDurationPO>()
                .eq(TicketNodeDurationPO::getTicketId, ticketId)
                .orderByAsc(TicketNodeDurationPO::getArriveAt)
                .orderByAsc(TicketNodeDurationPO::getId));

        Set<Long> assigneeIds = nodes.stream()
                .map(TicketNodeDurationPO::getAssigneeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> userNameMap = loadUserNames(assigneeIds);

        List<TicketNodeDurationOutput.NodeItem> items = nodes.stream()
                .map(node -> convertNodeItem(node, userNameMap))
                .collect(Collectors.toList());
        TicketNodeDurationOutput output = new TicketNodeDurationOutput();
        output.setTicketId(ticketId);
        output.setNodes(items);
        return output;
    }

    private TicketPO requireTicket(Long ticketId) {
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }
        return ticket;
    }

    private boolean shouldRecordRead(String key) {
        try {
            Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 7, TimeUnit.DAYS);
            return first == null || first;
        } catch (Exception ex) {
            log.warn("记录阅读去重Key失败，降级为允许写入: key={}", key, ex);
            return true;
        }
    }

    private TicketTimeTrackPO buildTrack(TicketPO ticket, Long userId, String action,
                                         String fromStatus, String toStatus,
                                         Long fromUserId, Long toUserId, String remark,
                                         Integer isFirstRead, Date timestamp) {
        TicketTimeTrackPO trackPO = new TicketTimeTrackPO();
        trackPO.setTicketId(ticket.getId());
        trackPO.setUserId(userId);
        trackPO.setUserRole(resolveTrackRole(userId, ticket));
        trackPO.setAction(action);
        trackPO.setFromStatus(normalizeStatus(fromStatus));
        trackPO.setToStatus(normalizeStatus(toStatus));
        trackPO.setFromUserId(fromUserId);
        trackPO.setToUserId(toUserId);
        trackPO.setRemark(remark);
        trackPO.setIsFirstRead(isFirstRead);
        trackPO.setTimestamp(timestamp != null ? timestamp : new Date());
        return trackPO;
    }

    private void saveTrack(TicketTimeTrackPO trackPO) {
        timeTrackMapper.insert(trackPO);
        eventPublisher.publishEvent(new TicketTimeTrackRecordedEvent(
                trackPO.getTicketId(),
                trackPO.getUserId(),
                trackPO.getUserRole(),
                trackPO.getAction(),
                trackPO.getFromStatus(),
                trackPO.getToStatus(),
                trackPO.getFromUserId(),
                trackPO.getToUserId(),
                trackPO.getTimestamp()
        ));
    }

    private TicketTimeTrackOutput.TrackItem convertTrackItem(TicketTimeTrackPO trackPO, Map<Long, String> userNameMap) {
        TicketTimeTrackOutput.TrackItem item = new TicketTimeTrackOutput.TrackItem();
        item.setId(trackPO.getId());
        item.setUserId(trackPO.getUserId());
        item.setUserName(userNameMap.get(trackPO.getUserId()));
        item.setUserRole(trackPO.getUserRole());
        item.setAction(trackPO.getAction());
        TicketAction ticketAction = TicketAction.fromCode(trackPO.getAction());
        if (ticketAction != null) {
            item.setActionLabel(ticketAction.getLabel());
        }
        item.setFromStatus(trackPO.getFromStatus());
        item.setToStatus(trackPO.getToStatus());
        item.setFromUserId(trackPO.getFromUserId());
        item.setFromUserName(userNameMap.get(trackPO.getFromUserId()));
        item.setToUserId(trackPO.getToUserId());
        item.setToUserName(userNameMap.get(trackPO.getToUserId()));
        item.setRemark(trackPO.getRemark());
        item.setIsFirstRead(trackPO.getIsFirstRead() != null && trackPO.getIsFirstRead() == 1);
        item.setTimestamp(trackPO.getTimestamp());
        return item;
    }

    private TicketNodeDurationOutput.NodeItem convertNodeItem(TicketNodeDurationPO nodePO, Map<Long, String> userNameMap) {
        TicketNodeDurationOutput.NodeItem item = new TicketNodeDurationOutput.NodeItem();
        item.setId(nodePO.getId());
        item.setNodeName(nodePO.getNodeName());
        item.setAssigneeId(nodePO.getAssigneeId());
        item.setAssigneeName(userNameMap.get(nodePO.getAssigneeId()));
        item.setAssigneeRole(nodePO.getAssigneeRole());
        item.setArriveAt(nodePO.getArriveAt());
        item.setFirstReadAt(nodePO.getFirstReadAt());
        item.setStartProcessAt(nodePO.getStartProcessAt());
        item.setLeaveAt(nodePO.getLeaveAt());
        item.setWaitDurationSec(nodePO.getWaitDurationSec());
        item.setProcessDurationSec(nodePO.getProcessDurationSec());
        item.setTotalDurationSec(nodePO.getTotalDurationSec());
        return item;
    }

    private Map<Long, String> loadUserNames(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUserPO> users = sysUserMapper.selectBatchIds(userIds);
        if (users == null || users.isEmpty()) {
            return Collections.emptyMap();
        }
        return users.stream().collect(Collectors.toMap(SysUserPO::getId, SysUserPO::getName));
    }

    private String resolveTrackRole(Long userId, TicketPO ticket) {
        if (userId == null) {
            return "SYSTEM";
        }
        List<String> roleCodes = getRoleCodes(userId);
        if (hasAnyRole(roleCodes, "ADMIN", "TICKET_ADMIN")) {
            return "SYSTEM";
        }
        if (hasAnyRole(roleCodes, "CUSTOMER_SERVICE", "SUBMITTER")) {
            return "CUSTOMER_SERVICE";
        }
        if (hasAnyRole(roleCodes, "TESTER")) {
            return "TESTER";
        }
        if (hasAnyRole(roleCodes, "DEVELOPER")) {
            return "DEVELOPER";
        }
        if (hasAnyRole(roleCodes, "HANDLER")) {
            String status = normalizeStatus(ticket.getStatus());
            return resolveByStatus(status);
        }
        if (userId.equals(ticket.getCreatorId())) {
            return "CUSTOMER_SERVICE";
        }
        if (userId.equals(ticket.getAssigneeId())) {
            return resolveByStatus(normalizeStatus(ticket.getStatus()));
        }
        return "SYSTEM";
    }

    private String resolveByStatus(String status) {
        if (status == null) {
            return "SYSTEM";
        }
        if (status.contains("DEV")) {
            return "DEVELOPER";
        }
        if (status.contains("TEST") || status.contains("VERIFY")) {
            return "TESTER";
        }
        if (status.contains("CS") || status.contains("DISPATCH")) {
            return "CUSTOMER_SERVICE";
        }
        return "SYSTEM";
    }

    private List<String> getRoleCodes(Long userId) {
        List<String> roleCodes = sysUserMapper.selectRoleCodesByUserId(userId);
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return roleCodes.stream()
                .filter(Objects::nonNull)
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toList());
    }

    private boolean hasAnyRole(List<String> roleCodes, String... targets) {
        if (roleCodes == null || roleCodes.isEmpty() || targets == null || targets.length == 0) {
            return false;
        }
        Set<String> targetSet = new HashSet<>(Arrays.asList(targets));
        for (String roleCode : roleCodes) {
            if (targetSet.contains(roleCode)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeStatus(String status) {
        return status == null ? null : status.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isStartProcessTransition(String fromStatus, String toStatus) {
        if (toStatus == null) {
            return false;
        }
        if (!START_PROCESS_STATUS.contains(toStatus)) {
            return false;
        }
        if (fromStatus == null) {
            return true;
        }
        return !fromStatus.equals(toStatus);
    }
}
