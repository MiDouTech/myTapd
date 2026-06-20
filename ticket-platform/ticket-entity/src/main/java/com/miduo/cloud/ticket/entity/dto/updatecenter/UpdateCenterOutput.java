package com.miduo.cloud.ticket.entity.dto.updatecenter;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 更新中心输出对象集合
 */
public class UpdateCenterOutput {

    private UpdateCenterOutput() {
    }

    @Data
    public static class ChangelogEntryOutput {
        private String type;
        private String module;
        private String description;
    }

    @Data
    public static class ChangelogFragmentOutput {
        private String fileName;
        private String date;
        private List<ChangelogEntryOutput> entries = new ArrayList<>();
    }

    @Data
    public static class CurrentWeekOutput {
        private String weekStart;
        private String weekEnd;
        private Boolean dataSourceAvailable;
        private String source;
        private String fetchedAt;
        private Integer totalDays;
        private Integer totalEntries;
        private Integer daysOffset;
        private Boolean hasMore;
        private List<ChangelogFragmentOutput> fragments = new ArrayList<>();
    }

    @Data
    public static class ChangelogDayOutput {
        private String date;
        private String commitTimeUtc;
        private List<ChangelogEntryOutput> entries = new ArrayList<>();
    }

    @Data
    public static class ChangelogReleaseOutput {
        private String version;
        private String releaseDate;
        private Integer entryCount;
        private String sourceScope;
        private List<String> highlights = new ArrayList<>();
        private Boolean entriesOmitted;
        private List<ChangelogDayOutput> days = new ArrayList<>();
    }

    @Data
    public static class ReleasesOutput {
        private Boolean dataSourceAvailable;
        private String source;
        private String fetchedAt;
        private Integer totalReleases;
        private Integer totalEntries;
        private List<ChangelogReleaseOutput> releases = new ArrayList<>();
    }

    @Data
    public static class GitHubCoAuthorOutput {
        private String name;
        private String matchedUsername;
        private String matchedDisplayName;
    }

    @Data
    public static class GitHubLogEntryOutput {
        private String sha;
        private String shortSha;
        private String message;
        private String authorName;
        private String authorAvatarUrl;
        private String commitTimeUtc;
        private String htmlUrl;
        private String matchedUsername;
        private String matchedDisplayName;
        private List<GitHubCoAuthorOutput> coAuthors = new ArrayList<>();
    }

    @Data
    public static class GitHubLogsOutput {
        private Boolean dataSourceAvailable;
        private String source;
        private String fetchedAt;
        private Integer totalCount;
        private Integer repoTotalCommitCount;
        private Boolean hasMore;
        private String nextCursor;
        private List<GitHubLogEntryOutput> logs = new ArrayList<>();
    }

    @Data
    public static class WeeklyReportSummaryOutput {
        private String fileName;
        private String title;
        private String reportWeek;
        private String period;
        private String updatedAt;
    }

    @Data
    public static class WeeklyReportsOutput {
        private Boolean dataSourceAvailable;
        private String source;
        private String fetchedAt;
        private Integer totalReports;
        private List<WeeklyReportSummaryOutput> reports = new ArrayList<>();
    }

    @Data
    public static class WeeklyReportDetailOutput {
        private String fileName;
        private String title;
        private String reportWeek;
        private String period;
        private String updatedAt;
        private String content;
        private Boolean dataSourceAvailable;
        private String source;
        private String fetchedAt;
    }
}
