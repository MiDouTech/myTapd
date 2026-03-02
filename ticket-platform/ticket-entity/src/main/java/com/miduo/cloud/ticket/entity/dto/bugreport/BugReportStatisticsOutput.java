package com.miduo.cloud.ticket.entity.dto.bugreport;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Bug简报统计输出
 */
@Data
public class BugReportStatisticsOutput implements Serializable {

    private List<DistributionItem> logicCauseDistribution;

    private List<DistributionItem> defectCategoryDistribution;

    private List<DistributionItem> introducedProjectTop;

    private List<ResponsibleStatItem> responsibleStatistics;

    private Integer timelyCount;

    private Integer totalCount;

    private Double timelyRate;

    @Data
    public static class DistributionItem implements Serializable {
        private String name;
        private Long count;
    }

    @Data
    public static class ResponsibleStatItem implements Serializable {
        private Long userId;
        private String userName;
        private Long count;
    }
}
