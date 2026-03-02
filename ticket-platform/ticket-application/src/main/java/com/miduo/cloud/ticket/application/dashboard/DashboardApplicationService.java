package com.miduo.cloud.ticket.application.dashboard;

import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.entity.dto.dashboard.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.mapper.TicketDashboardMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.model.DailyCountRow;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.dashboard.model.StatusCountRow;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 数据看板应用服务
 */
@Service
public class DashboardApplicationService extends BaseApplicationService {

    private static final int DEFAULT_TREND_DAYS = 7;
    private static final int DEFAULT_WORKLOAD_LIMIT = 10;
    private static final int MAX_WORKLOAD_LIMIT = 50;

    private final TicketDashboardMapper dashboardMapper;

    public DashboardApplicationService(TicketDashboardMapper dashboardMapper) {
        this.dashboardMapper = dashboardMapper;
    }

    public DashboardOverviewOutput getOverview() {
        List<StatusCountRow> statusRows = dashboardMapper.selectStatusCounts();
        Map<String, Long> statusCountMap = new HashMap<>();
        long totalCount = 0L;
        for (StatusCountRow row : statusRows) {
            String status = row.getStatus() == null ? "" : row.getStatus().toLowerCase(Locale.ROOT);
            long total = safeLong(row.getTotal());
            statusCountMap.put(status, total);
            totalCount += total;
        }

        DashboardOverviewOutput output = new DashboardOverviewOutput();
        output.setPendingAcceptCount(calculatePendingCount(statusCountMap));
        output.setProcessingCount(safeGet(statusCountMap, "processing")
                + safeGet(statusCountMap, "testing")
                + safeGet(statusCountMap, "developing"));
        output.setSuspendedCount(safeGet(statusCountMap, "suspended"));
        output.setCompletedCount(safeGet(statusCountMap, "completed") + safeGet(statusCountMap, "closed"));
        output.setSlaBreachedCount(safeLong(dashboardMapper.selectSlaBreachedTicketCount()));
        output.setTotalCount(totalCount);
        return output;
    }

    public List<DashboardTrendPointOutput> getTrend(Integer days) {
        int trendDays = normalizeDays(days);
        Date startDay = startOfDay(addDays(new Date(), -(trendDays - 1)));
        Date endDay = addDays(startOfDay(new Date()), 1);

        Map<String, Long> createdMap = toDailyMap(dashboardMapper.selectCreatedTrend(startDay, endDay));
        Map<String, Long> closedMap = toDailyMap(dashboardMapper.selectClosedTrend(startDay, endDay));
        long backlog = safeLong(dashboardMapper.countOpenBefore(startDay));

        List<DashboardTrendPointOutput> outputs = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (int i = 0; i < trendDays; i++) {
            Date currentDay = addDays(startDay, i);
            String day = sdf.format(currentDay);
            long createdCount = createdMap.getOrDefault(day, 0L);
            long closedCount = closedMap.getOrDefault(day, 0L);
            backlog = Math.max(0L, backlog + createdCount - closedCount);

            DashboardTrendPointOutput point = new DashboardTrendPointOutput();
            point.setDay(day);
            point.setCreatedCount(createdCount);
            point.setClosedCount(closedCount);
            point.setBacklogCount(backlog);
            outputs.add(point);
        }
        return outputs;
    }

    public List<DashboardCategoryDistributionOutput> getCategoryDistribution() {
        List<DashboardCategoryDistributionOutput> outputs = dashboardMapper.selectCategoryDistribution();
        if (outputs == null || outputs.isEmpty()) {
            return Collections.emptyList();
        }
        long total = 0L;
        for (DashboardCategoryDistributionOutput output : outputs) {
            total += safeLong(output.getTicketCount());
        }
        for (DashboardCategoryDistributionOutput output : outputs) {
            output.setTicketCount(safeLong(output.getTicketCount()));
            output.setPercentage(calculateRate(output.getTicketCount(), total));
        }
        return outputs;
    }

    public DashboardEfficiencyOutput getEfficiency() {
        DashboardEfficiencyOutput output = dashboardMapper.selectEfficiency();
        if (output == null) {
            output = new DashboardEfficiencyOutput();
        }
        output.setAvgResponseMinutes(safeDouble(output.getAvgResponseMinutes()));
        output.setAvgResolveMinutes(safeDouble(output.getAvgResolveMinutes()));
        output.setCompletedCount(safeLong(output.getCompletedCount()));
        output.setTotalCount(safeLong(output.getTotalCount()));
        output.setCompletionRate(calculateRate(output.getCompletedCount(), output.getTotalCount()));
        return output;
    }

    public DashboardSlaAchievementOutput getSlaAchievement() {
        DashboardSlaAchievementOutput output = dashboardMapper.selectSlaAchievement();
        if (output == null) {
            output = new DashboardSlaAchievementOutput();
        }
        output.setTotalCount(safeLong(output.getTotalCount()));
        output.setAchievedCount(safeLong(output.getAchievedCount()));
        output.setBreachedCount(safeLong(output.getBreachedCount()));
        output.setAchievementRate(calculateRate(output.getAchievedCount(), output.getTotalCount()));
        return output;
    }

    public List<DashboardWorkloadOutput> getWorkloadTop(Integer limit) {
        int topN = normalizeWorkloadLimit(limit);
        List<DashboardWorkloadOutput> outputs = dashboardMapper.selectWorkloadTop(topN);
        if (outputs == null || outputs.isEmpty()) {
            return Collections.emptyList();
        }
        for (DashboardWorkloadOutput output : outputs) {
            output.setTotalCount(safeLong(output.getTotalCount()));
            output.setProcessingCount(safeLong(output.getProcessingCount()));
            output.setCompletedCount(safeLong(output.getCompletedCount()));
        }
        return outputs;
    }

    private long calculatePendingCount(Map<String, Long> statusCountMap) {
        long count = 0L;
        for (Map.Entry<String, Long> entry : statusCountMap.entrySet()) {
            String status = entry.getKey();
            if (status.startsWith("pending") && !"pending_verify".equals(status)) {
                count += safeLong(entry.getValue());
            }
        }
        return count;
    }

    private Map<String, Long> toDailyMap(List<DailyCountRow> rows) {
        Map<String, Long> result = new HashMap<>();
        if (rows == null || rows.isEmpty()) {
            return result;
        }
        for (DailyCountRow row : rows) {
            if (row.getDay() != null) {
                result.put(row.getDay(), safeLong(row.getTotal()));
            }
        }
        return result;
    }

    private int normalizeDays(Integer days) {
        if (days == null || days <= 0) {
            return DEFAULT_TREND_DAYS;
        }
        return Math.min(days, 60);
    }

    private int normalizeWorkloadLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_WORKLOAD_LIMIT;
        }
        return Math.min(limit, MAX_WORKLOAD_LIMIT);
    }

    private Date addDays(Date date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return calendar.getTime();
    }

    private Date startOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private long safeGet(Map<String, Long> map, String key) {
        if (map == null || key == null) {
            return 0L;
        }
        return safeLong(map.get(key));
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0D : value;
    }

    private double calculateRate(Long numerator, Long denominator) {
        long denominatorValue = safeLong(denominator);
        if (denominatorValue <= 0) {
            return 0D;
        }
        BigDecimal rate = BigDecimal.valueOf(safeLong(numerator))
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominatorValue), 2, RoundingMode.HALF_UP);
        return rate.doubleValue();
    }
}
