package com.miduo.cloud.ticket.application.sla;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.DefectSeverityResolveSlaRule;
import com.miduo.cloud.ticket.common.enums.SlaLevel;
import com.miduo.cloud.ticket.common.enums.SlaTimerStatus;
import com.miduo.cloud.ticket.common.enums.SlaTimerType;
import com.miduo.cloud.ticket.domain.common.event.SlaBreachedEvent;
import com.miduo.cloud.ticket.domain.common.event.SlaWarningEvent;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sla.mapper.SlaPolicyMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sla.mapper.SlaTimerMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sla.po.SlaPolicyPO;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sla.po.SlaTimerPO;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * SLA计时器服务
 * 负责计时器的启动、暂停、恢复、完成和超时检查
 */
@Service
public class SlaTimerService extends BaseApplicationService {

    private final SlaTimerMapper slaTimerMapper;
    private final SlaPolicyMapper slaPolicyMapper;
    private final WorkingTimeCalculator workingTimeCalculator;
    private final ApplicationEventPublisher eventPublisher;

    public SlaTimerService(SlaTimerMapper slaTimerMapper,
                           SlaPolicyMapper slaPolicyMapper,
                           WorkingTimeCalculator workingTimeCalculator,
                           ApplicationEventPublisher eventPublisher) {
        this.slaTimerMapper = slaTimerMapper;
        this.slaPolicyMapper = slaPolicyMapper;
        this.workingTimeCalculator = workingTimeCalculator;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 启动SLA计时器（工单创建时调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void startTimers(Long ticketId, Long slaPolicyId) {
        SlaPolicyPO policy = slaPolicyMapper.selectById(slaPolicyId);
        if (policy == null || policy.getIsActive() == 0) {
            log.warn("SLA策略不存在或已禁用: policyId={}", slaPolicyId);
            return;
        }

        LocalDateTime now = workingTimeCalculator.now();

        SlaTimerPO responseTimer = buildTimer(ticketId, slaPolicyId,
                SlaTimerType.RESPONSE.getCode(), policy.getResponseTime(), now);
        slaTimerMapper.insert(responseTimer);

        SlaTimerPO resolveTimer = buildTimer(ticketId, slaPolicyId,
                SlaTimerType.RESOLVE.getCode(), policy.getResolveTime(), now);
        slaTimerMapper.insert(resolveTimer);

        log.info("SLA计时器已启动: ticketId={}, policyId={}", ticketId, slaPolicyId);
    }

    /**
     * 暂停SLA计时器（挂起、待验收状态）
     */
    @Transactional(rollbackFor = Exception.class)
    public void pauseTimers(Long ticketId) {
        List<SlaTimerPO> runningTimers = getRunningTimersByTicketId(ticketId);
        Date now = new Date();
        LocalDateTime nowLdt = workingTimeCalculator.now();
        for (SlaTimerPO timer : runningTimers) {
            LocalDateTime startAt = toLocalDateTime(timer.getStartAt());
            int baseElapsed = timer.getBaseElapsedMinutes() != null ? timer.getBaseElapsedMinutes() : 0;
            int elapsed = baseElapsed + workingTimeCalculator.calculateWorkingMinutes(startAt, nowLdt);
            timer.setStatus(SlaTimerStatus.PAUSED.getCode());
            timer.setElapsedMinutes(elapsed);
            timer.setPauseAt(now);
            slaTimerMapper.updateById(timer);
        }
        log.info("SLA计时器已暂停: ticketId={}, count={}", ticketId, runningTimers.size());
    }

    /**
     * 恢复SLA计时器（从挂起/待验收恢复到处理中）
     */
    @Transactional(rollbackFor = Exception.class)
    public void resumeTimers(Long ticketId) {
        List<SlaTimerPO> pausedTimers = getPausedTimersByTicketId(ticketId);
        Date now = new Date();
        for (SlaTimerPO timer : pausedTimers) {
            timer.setStatus(SlaTimerStatus.RUNNING.getCode());
            timer.setBaseElapsedMinutes(timer.getElapsedMinutes() != null ? timer.getElapsedMinutes() : 0);
            timer.setStartAt(now);
            timer.setPauseAt(null);
            int remainingMinutes = timer.getThresholdMinutes() - timer.getElapsedMinutes();
            if (remainingMinutes > 0) {
                LocalDateTime businessNow = workingTimeCalculator.now();
                LocalDateTime deadline = workingTimeCalculator.calculateDeadline(
                        businessNow, remainingMinutes);
                timer.setDeadline(toDate(deadline));
            }
            slaTimerMapper.updateById(timer);
        }
        log.info("SLA计时器已恢复: ticketId={}, count={}", ticketId, pausedTimers.size());
    }

    /**
     * 完成响应计时器（首次响应时调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeResponseTimer(Long ticketId) {
        LambdaUpdateWrapper<SlaTimerPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SlaTimerPO::getTicketId, ticketId)
                .eq(SlaTimerPO::getTimerType, SlaTimerType.RESPONSE.getCode())
                .eq(SlaTimerPO::getStatus, SlaTimerStatus.RUNNING.getCode());

        SlaTimerPO update = new SlaTimerPO();
        update.setStatus(SlaTimerStatus.COMPLETED.getCode());
        update.setCompletedAt(new Date());

        slaTimerMapper.update(update, wrapper);
        log.info("响应计时器已完成: ticketId={}", ticketId);
    }

    /**
     * 完成所有计时器（工单完成/关闭时调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeAllTimers(Long ticketId) {
        LambdaUpdateWrapper<SlaTimerPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SlaTimerPO::getTicketId, ticketId)
                .in(SlaTimerPO::getStatus, SlaTimerStatus.RUNNING.getCode(), SlaTimerStatus.PAUSED.getCode());

        SlaTimerPO update = new SlaTimerPO();
        update.setStatus(SlaTimerStatus.COMPLETED.getCode());
        update.setCompletedAt(new Date());

        slaTimerMapper.update(update, wrapper);
        log.info("所有SLA计时器已完成: ticketId={}", ticketId);
    }

    /**
     * 根据测试确认的缺陷等级调整解决计时器，不影响首次响应计时器。
     */
    @Transactional(rollbackFor = Exception.class)
    public void adjustResolveTimerBySeverity(Long ticketId, String severityLevel) {
        if (ticketId == null) {
            return;
        }
        DefectSeverityResolveSlaRule rule = DefectSeverityResolveSlaRule.fromSeverityLevel(severityLevel);
        if (rule == null) {
            log.warn("缺陷等级未配置解决SLA规则: ticketId={}, severityLevel={}", ticketId, severityLevel);
            return;
        }

        SlaTimerPO timer = slaTimerMapper.selectOne(new LambdaQueryWrapper<SlaTimerPO>()
                .eq(SlaTimerPO::getTicketId, ticketId)
                .eq(SlaTimerPO::getTimerType, SlaTimerType.RESOLVE.getCode())
                .ne(SlaTimerPO::getStatus, SlaTimerStatus.COMPLETED.getCode())
                .orderByDesc(SlaTimerPO::getId)
                .last("LIMIT 1"));
        if (timer == null) {
            log.info("缺陷等级已保存但未找到解决SLA计时器: ticketId={}, severityLevel={}", ticketId, severityLevel);
            return;
        }

        LocalDateTime now = workingTimeCalculator.now();
        int elapsed = calculateCurrentElapsed(timer, now);
        int threshold = rule.getResolveMinutes();
        boolean wasBreached = SlaTimerStatus.BREACHED.getCode().equals(timer.getStatus())
                || Integer.valueOf(1).equals(timer.getIsBreached());

        timer.setThresholdMinutes(threshold);
        timer.setElapsedMinutes(elapsed);
        timer.setBaseElapsedMinutes(elapsed);
        timer.setStartAt(toDate(now));
        timer.setIsWarned(0);

        if (elapsed >= threshold) {
            timer.setStatus(SlaTimerStatus.BREACHED.getCode());
            timer.setIsBreached(1);
            timer.setBreachedAt(new Date());
            timer.setDeadline(toDate(now));
            slaTimerMapper.updateById(timer);
            if (!wasBreached) {
                eventPublisher.publishEvent(new SlaBreachedEvent(
                        timer.getTicketId(), timer.getId(), timer.getTimerType(), elapsed, threshold));
            }
            log.warn("缺陷等级调整后解决SLA已超时: ticketId={}, severityLevel={}, elapsed={}, threshold={}",
                    ticketId, severityLevel, elapsed, threshold);
            return;
        }

        if (SlaTimerStatus.BREACHED.getCode().equals(timer.getStatus())) {
            timer.setStatus(SlaTimerStatus.RUNNING.getCode());
        }
        timer.setIsBreached(0);
        timer.setBreachedAt(null);
        int remainingMinutes = threshold - elapsed;
        LocalDateTime deadline = workingTimeCalculator.calculateDeadline(now, remainingMinutes);
        timer.setDeadline(toDate(deadline));
        slaTimerMapper.updateById(timer);
        log.info("缺陷等级调整解决SLA成功: ticketId={}, severityLevel={}, threshold={}, elapsed={}, deadline={}",
                ticketId, severityLevel, threshold, elapsed, deadline);
    }

    /**
     * 检查所有运行中的计时器，触发预警或超时事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void checkRunningTimers() {
        List<SlaTimerPO> runningTimers = slaTimerMapper.selectRunningTimers();
        if (runningTimers.isEmpty()) {
            return;
        }

        for (SlaTimerPO timer : runningTimers) {
            checkSingleTimer(timer);
        }
    }

    private void checkSingleTimer(SlaTimerPO timer) {
        LocalDateTime startAt = toLocalDateTime(timer.getStartAt());
        LocalDateTime now = workingTimeCalculator.now();
        int baseElapsed = timer.getBaseElapsedMinutes() != null ? timer.getBaseElapsedMinutes() : 0;
        int elapsed = baseElapsed + workingTimeCalculator.calculateWorkingMinutes(startAt, now);
        timer.setElapsedMinutes(elapsed);

        int threshold = timer.getThresholdMinutes();
        int remaining = threshold - elapsed;
        int remainingPct = threshold > 0 ? (remaining * 100 / threshold) : 0;

        if (remaining <= 0) {
            timer.setStatus(SlaTimerStatus.BREACHED.getCode());
            timer.setIsBreached(1);
            timer.setBreachedAt(new Date());
            slaTimerMapper.updateById(timer);

            eventPublisher.publishEvent(new SlaBreachedEvent(
                    timer.getTicketId(), timer.getId(), timer.getTimerType(),
                    elapsed, threshold));
            log.warn("SLA已超时: ticketId={}, timerType={}, elapsed={}, threshold={}",
                    timer.getTicketId(), timer.getTimerType(), elapsed, threshold);
            return;
        }

        SlaPolicyPO policy = slaPolicyMapper.selectById(timer.getSlaPolicyId());
        int warningPct = (policy != null) ? policy.getWarningPct() : 75;
        int usedPct = threshold > 0 ? (elapsed * 100 / threshold) : 0;

        if (usedPct >= warningPct && timer.getIsWarned() == 0) {
            timer.setIsWarned(1);
            slaTimerMapper.updateById(timer);

            SlaLevel level = SlaLevel.fromRemainingPct(remainingPct);
            eventPublisher.publishEvent(new SlaWarningEvent(
                    timer.getTicketId(), timer.getId(), timer.getTimerType(),
                    elapsed, threshold, level.getCode()));
            log.warn("SLA预警触发: ticketId={}, timerType={}, level={}, usedPct={}%",
                    timer.getTicketId(), timer.getTimerType(), level.getCode(), usedPct);
        } else {
            slaTimerMapper.updateById(timer);
        }
    }

    private SlaTimerPO buildTimer(Long ticketId, Long slaPolicyId,
                                  String timerType, int thresholdMinutes,
                                  LocalDateTime startTime) {
        SlaTimerPO timer = new SlaTimerPO();
        timer.setTicketId(ticketId);
        timer.setSlaPolicyId(slaPolicyId);
        timer.setTimerType(timerType);
        timer.setStatus(SlaTimerStatus.RUNNING.getCode());
        timer.setThresholdMinutes(thresholdMinutes);
        timer.setElapsedMinutes(0);
        timer.setBaseElapsedMinutes(0);
        timer.setStartAt(toDate(startTime));
        timer.setIsWarned(0);
        timer.setIsBreached(0);

        LocalDateTime deadline = workingTimeCalculator.calculateDeadline(startTime, thresholdMinutes);
        timer.setDeadline(toDate(deadline));
        return timer;
    }

    private List<SlaTimerPO> getRunningTimersByTicketId(Long ticketId) {
        LambdaQueryWrapper<SlaTimerPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SlaTimerPO::getTicketId, ticketId)
                .eq(SlaTimerPO::getStatus, SlaTimerStatus.RUNNING.getCode());
        return slaTimerMapper.selectList(wrapper);
    }

    private List<SlaTimerPO> getPausedTimersByTicketId(Long ticketId) {
        LambdaQueryWrapper<SlaTimerPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SlaTimerPO::getTicketId, ticketId)
                .eq(SlaTimerPO::getStatus, SlaTimerStatus.PAUSED.getCode());
        return slaTimerMapper.selectList(wrapper);
    }

    private int calculateCurrentElapsed(SlaTimerPO timer, LocalDateTime now) {
        if (timer == null) {
            return 0;
        }
        int savedElapsed = timer.getElapsedMinutes() != null ? timer.getElapsedMinutes() : 0;
        if (SlaTimerStatus.PAUSED.getCode().equals(timer.getStatus())
                || SlaTimerStatus.COMPLETED.getCode().equals(timer.getStatus())) {
            return savedElapsed;
        }
        int baseElapsed = timer.getBaseElapsedMinutes() != null ? timer.getBaseElapsedMinutes() : savedElapsed;
        LocalDateTime startAt = toLocalDateTime(timer.getStartAt());
        return baseElapsed + workingTimeCalculator.calculateWorkingMinutes(startAt, now);
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return workingTimeCalculator.now();
        }
        return workingTimeCalculator.toBusinessLocalDateTime(date);
    }

    private Date toDate(LocalDateTime localDateTime) {
        return workingTimeCalculator.toDate(localDateTime);
    }
}
