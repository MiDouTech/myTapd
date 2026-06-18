package com.miduo.cloud.ticket.application.sla;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.mapper.SystemConfigMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po.SystemConfigPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工作时间计算器
 * 排除非工作时间、节假日，只计算工作时间内的消耗分钟数
 */
@Component
public class WorkingTimeCalculator {

    private static final Logger log = LoggerFactory.getLogger(WorkingTimeCalculator.class);
    private static final String CONFIG_GROUP_BASIC = "BASIC";
    private static final String CONFIG_KEY_TIMEZONE = "timezone";
    private static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    private final SystemConfigMapper systemConfigMapper;

    public WorkingTimeCalculator(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }

    /**
     * 计算从起始时间到当前时间之间的工作分钟数
     *
     * @param startTime 起始时间
     * @return 工作分钟数
     */
    public int calculateElapsedWorkingMinutes(LocalDateTime startTime) {
        return calculateWorkingMinutes(startTime, now());
    }

    /**
     * 当前业务时间。
     */
    public LocalDateTime now() {
        return LocalDateTime.now(getBusinessZoneId());
    }

    /**
     * 将数据库时间转换为业务时区下的本地时间，是因为 SLA 配置的 09:00-18:00 是业务时间口径。
     */
    public LocalDateTime toBusinessLocalDateTime(Date date) {
        if (date == null) {
            return now();
        }
        return date.toInstant().atZone(getBusinessZoneId()).toLocalDateTime();
    }

    /**
     * 将业务本地时间转换为 Date 后落库，避免 JVM 默认时区为 UTC 时把截止时间推迟 8 小时。
     */
    public Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(getBusinessZoneId()).toInstant());
    }

    /**
     * 计算两个时间点之间的工作分钟数
     *
     * @param start 起始时间
     * @param end   结束时间
     * @return 工作分钟数
     */
    public int calculateWorkingMinutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            return 0;
        }

        WorkingTimeConfig config = loadWorkingTimeConfig();
        int totalMinutes = 0;

        LocalDate currentDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();

        while (!currentDate.isAfter(endDate)) {
            if (isWorkingDay(currentDate, config)) {
                LocalDateTime dayStart = currentDate.equals(start.toLocalDate())
                        ? start : currentDate.atTime(config.getStartTime());
                LocalDateTime dayEnd = currentDate.equals(endDate)
                        ? end : currentDate.atTime(config.getEndTime());

                LocalDateTime effectiveStart = dayStart.toLocalTime().isBefore(config.getStartTime())
                        ? currentDate.atTime(config.getStartTime()) : dayStart;
                LocalDateTime effectiveEnd = dayEnd.toLocalTime().isAfter(config.getEndTime())
                        ? currentDate.atTime(config.getEndTime()) : dayEnd;

                if (effectiveStart.isBefore(effectiveEnd)) {
                    totalMinutes += (int) ChronoUnit.MINUTES.between(effectiveStart, effectiveEnd);
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        return totalMinutes;
    }

    /**
     * 从起始时间往后推算指定工作分钟后的截止时间
     *
     * @param start           起始时间
     * @param workingMinutes  需要经过的工作分钟数
     * @return 截止时间
     */
    public LocalDateTime calculateDeadline(LocalDateTime start, int workingMinutes) {
        if (start == null || workingMinutes <= 0) {
            return start;
        }

        WorkingTimeConfig config = loadWorkingTimeConfig();
        int remainingMinutes = workingMinutes;
        LocalDate currentDate = start.toLocalDate();
        LocalTime currentTime = start.toLocalTime();

        if (currentTime.isBefore(config.getStartTime())) {
            currentTime = config.getStartTime();
        }
        if (currentTime.isAfter(config.getEndTime())) {
            currentDate = currentDate.plusDays(1);
            currentTime = config.getStartTime();
        }

        while (remainingMinutes > 0) {
            if (isWorkingDay(currentDate, config)) {
                LocalTime effectiveStart = currentDate.equals(start.toLocalDate()) ? currentTime : config.getStartTime();
                if (effectiveStart.isBefore(config.getStartTime())) {
                    effectiveStart = config.getStartTime();
                }

                int availableMinutes = (int) ChronoUnit.MINUTES.between(effectiveStart, config.getEndTime());
                if (availableMinutes <= 0) {
                    currentDate = currentDate.plusDays(1);
                    continue;
                }

                if (remainingMinutes <= availableMinutes) {
                    return currentDate.atTime(effectiveStart).plusMinutes(remainingMinutes);
                }

                remainingMinutes -= availableMinutes;
            }
            currentDate = currentDate.plusDays(1);
        }

        return currentDate.atTime(config.getStartTime());
    }

    /**
     * 判断当前时间是否在工作时间内
     */
    public boolean isCurrentlyWorkingTime() {
        WorkingTimeConfig config = loadWorkingTimeConfig();
        LocalDateTime now = now();
        if (!isWorkingDay(now.toLocalDate(), config)) {
            return false;
        }
        LocalTime currentTime = now.toLocalTime();
        return !currentTime.isBefore(config.getStartTime()) && !currentTime.isAfter(config.getEndTime());
    }

    private boolean isWorkingDay(LocalDate date, WorkingTimeConfig config) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int isoDay = dayOfWeek.getValue();
        return config.getWorkingDays().contains(isoDay);
    }

    public ZoneId getBusinessZoneId() {
        try {
            List<SystemConfigPO> configs = systemConfigMapper.selectByGroup(CONFIG_GROUP_BASIC);
            Map<String, String> configMap = configs.stream()
                    .collect(Collectors.toMap(SystemConfigPO::getConfigKey, SystemConfigPO::getConfigValue, (a, b) -> a));
            String timezone = configMap.getOrDefault(CONFIG_KEY_TIMEZONE, DEFAULT_TIMEZONE);
            return ZoneId.of(timezone);
        } catch (DateTimeException e) {
            log.warn("系统时区配置非法，使用默认时区: timezone={}", DEFAULT_TIMEZONE, e);
            return ZoneId.of(DEFAULT_TIMEZONE);
        } catch (Exception e) {
            log.warn("读取系统时区配置失败，使用默认时区: timezone={}", DEFAULT_TIMEZONE, e);
            return ZoneId.of(DEFAULT_TIMEZONE);
        }
    }

    private WorkingTimeConfig loadWorkingTimeConfig() {
        List<SystemConfigPO> configs = systemConfigMapper.selectByGroup("WORKING_TIME");
        Map<String, String> configMap = configs.stream()
                .collect(Collectors.toMap(SystemConfigPO::getConfigKey, SystemConfigPO::getConfigValue));

        String startTimeStr = configMap.getOrDefault("working_time_start", "09:00");
        String endTimeStr = configMap.getOrDefault("working_time_end", "18:00");
        String workingDaysStr = configMap.getOrDefault("working_days", "1,2,3,4,5");

        LocalTime startTime = LocalTime.parse(startTimeStr);
        LocalTime endTime = LocalTime.parse(endTimeStr);
        Set<Integer> workingDays = Arrays.stream(workingDaysStr.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .collect(Collectors.toSet());

        return new WorkingTimeConfig(startTime, endTime, workingDays);
    }

    /**
     * 工作时间配置内部类
     */
    private static class WorkingTimeConfig {
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final Set<Integer> workingDays;

        WorkingTimeConfig(LocalTime startTime, LocalTime endTime, Set<Integer> workingDays) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.workingDays = workingDays;
        }

        LocalTime getStartTime() {
            return startTime;
        }

        LocalTime getEndTime() {
            return endTime;
        }

        Set<Integer> getWorkingDays() {
            return workingDays;
        }
    }
}
