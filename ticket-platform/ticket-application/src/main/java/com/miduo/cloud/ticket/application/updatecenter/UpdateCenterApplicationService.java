package com.miduo.cloud.ticket.application.updatecenter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.UpdateCenterSource;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.updatecenter.UpdateCenterOutput;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

/**
 * 更新中心应用服务
 */
@Service
public class UpdateCenterApplicationService {

    private static final int DEFAULT_FRAGMENT_DAYS_LIMIT = 4;
    private static final int DEFAULT_RELEASE_LIMIT = 8;
    private static final int DEFAULT_GIT_LOG_LIMIT = 80;
    private static final int MAX_RELEASE_LIMIT = 100;
    private static final int MAX_GIT_LOG_LIMIT = 1000;
    private static final int GIT_COMMAND_TIMEOUT_SECONDS = 8;
    private static final int HTTP_TIMEOUT_MS = 8000;
    private static final String DEFAULT_GITHUB_OWNER = "MiDouTech";
    private static final String DEFAULT_GITHUB_REPO = "myTapd";
    private static final String DEFAULT_GITHUB_BRANCH = "main";
    private static final String DEFAULT_GITHUB_API_BASE = "https://api.github.com";
    private static final String DEFAULT_GITHUB_RAW_BASE = "https://raw.githubusercontent.com";
    private static final Pattern DATE_IN_FILE_NAME = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}).*");
    private static final Pattern RELEASE_HEADER = Pattern.compile("^##\\s+\\[?([^\\]\\-]+)]?\\s*(?:-\\s*(\\d{4}-\\d{2}-\\d{2}))?.*$");
    private static final Pattern DAY_HEADER = Pattern.compile("^###\\s+(\\d{4}-\\d{2}-\\d{2}).*$");
    private static final Pattern SHA_PATTERN = Pattern.compile("^[0-9a-fA-F]{4,40}$");
    private static final Pattern REPORT_FILE_NAME = Pattern.compile("^report\\.(\\d{4}-W\\d{2})\\.md$");
    private static final Pattern REPORT_TITLE = Pattern.compile("^#\\s+周报\\s+(.+?)\\s*(?:\\((.+?)\\))?\\s*$");

    private final String configuredRepoRoot;
    private final String githubOwner;
    private final String githubRepo;
    private final String githubBranch;
    private final String githubApiBase;
    private final String githubRawBase;
    private final String githubToken;

    public UpdateCenterApplicationService(@Value("${update-center.repo-root:}") String configuredRepoRoot,
                                          @Value("${update-center.github-owner:" + DEFAULT_GITHUB_OWNER + "}") String githubOwner,
                                          @Value("${update-center.github-repo:" + DEFAULT_GITHUB_REPO + "}") String githubRepo,
                                          @Value("${update-center.github-branch:" + DEFAULT_GITHUB_BRANCH + "}") String githubBranch,
                                          @Value("${update-center.github-api-base:" + DEFAULT_GITHUB_API_BASE + "}") String githubApiBase,
                                          @Value("${update-center.github-raw-base:" + DEFAULT_GITHUB_RAW_BASE + "}") String githubRawBase,
                                          @Value("${update-center.github-token:${GITHUB_TOKEN:}}") String githubToken) {
        this.configuredRepoRoot = configuredRepoRoot;
        this.githubOwner = normalizeConfig(githubOwner, DEFAULT_GITHUB_OWNER);
        this.githubRepo = normalizeConfig(githubRepo, DEFAULT_GITHUB_REPO);
        this.githubBranch = normalizeConfig(githubBranch, DEFAULT_GITHUB_BRANCH);
        this.githubApiBase = trimTrailingSlash(normalizeConfig(githubApiBase, DEFAULT_GITHUB_API_BASE));
        this.githubRawBase = trimTrailingSlash(normalizeConfig(githubRawBase, DEFAULT_GITHUB_RAW_BASE));
        this.githubToken = githubToken == null ? "" : githubToken.trim();
    }

    public UpdateCenterOutput.CurrentWeekOutput getCurrentWeek(Integer daysLimit, Integer daysOffset, Boolean force) {
        SourceResult<List<UpdateCenterOutput.ChangelogFragmentOutput>> fragmentResult = readFragments();
        List<UpdateCenterOutput.ChangelogFragmentOutput> allFragments = fragmentResult.getData();
        Collections.sort(allFragments, new Comparator<UpdateCenterOutput.ChangelogFragmentOutput>() {
            @Override
            public int compare(UpdateCenterOutput.ChangelogFragmentOutput left,
                               UpdateCenterOutput.ChangelogFragmentOutput right) {
                int dateCompare = nullSafe(right.getDate()).compareTo(nullSafe(left.getDate()));
                if (dateCompare != 0) {
                    return dateCompare;
                }
                return nullSafe(right.getFileName()).compareTo(nullSafe(left.getFileName()));
            }
        });

        int normalizedLimit = normalizePositive(daysLimit, DEFAULT_FRAGMENT_DAYS_LIMIT, 100);
        int normalizedOffset = normalizeOffset(daysOffset);
        int fromIndex = Math.min(normalizedOffset, allFragments.size());
        int toIndex = Math.min(fromIndex + normalizedLimit, allFragments.size());
        List<UpdateCenterOutput.ChangelogFragmentOutput> pageFragments = new ArrayList<>(allFragments.subList(fromIndex, toIndex));

        UpdateCenterOutput.CurrentWeekOutput output = new UpdateCenterOutput.CurrentWeekOutput();
        output.setDataSourceAvailable(fragmentResult.isAvailable());
        output.setSource(fragmentResult.getSource().getCode());
        output.setFetchedAt(nowIso());
        output.setTotalDays(allFragments.size());
        output.setTotalEntries(countFragmentEntries(allFragments));
        output.setDaysOffset(normalizedOffset);
        output.setHasMore(toIndex < allFragments.size());
        output.setFragments(pageFragments);

        String today = LocalDate.now().toString();
        if (allFragments.isEmpty()) {
            output.setWeekStart(today);
            output.setWeekEnd(today);
        } else {
            output.setWeekStart(findMinFragmentDate(allFragments));
            output.setWeekEnd(findMaxFragmentDate(allFragments));
        }
        return output;
    }

    public UpdateCenterOutput.ReleasesOutput getReleases(Integer limit, Boolean summary, Boolean force) {
        SourceResult<List<UpdateCenterOutput.ChangelogReleaseOutput>> releaseResult = readReleases(false);
        List<UpdateCenterOutput.ChangelogReleaseOutput> allReleases = releaseResult.getData();
        int normalizedLimit = normalizePositive(limit, DEFAULT_RELEASE_LIMIT, MAX_RELEASE_LIMIT);
        List<UpdateCenterOutput.ChangelogReleaseOutput> pageReleases =
                new ArrayList<>(allReleases.subList(0, Math.min(normalizedLimit, allReleases.size())));
        if (Boolean.TRUE.equals(summary)) {
            for (UpdateCenterOutput.ChangelogReleaseOutput release : pageReleases) {
                release.setDays(new ArrayList<UpdateCenterOutput.ChangelogDayOutput>());
                release.setEntriesOmitted(true);
            }
        }

        UpdateCenterOutput.ReleasesOutput output = new UpdateCenterOutput.ReleasesOutput();
        output.setDataSourceAvailable(releaseResult.isAvailable());
        output.setSource(releaseResult.getSource().getCode());
        output.setFetchedAt(nowIso());
        output.setTotalReleases(allReleases.size());
        output.setTotalEntries(countReleaseEntries(allReleases));
        output.setReleases(pageReleases);
        return output;
    }

    public UpdateCenterOutput.ChangelogReleaseOutput getReleaseDetail(String version, Boolean force) {
        if (version == null || version.trim().isEmpty()) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "版本号不能为空");
        }
        List<UpdateCenterOutput.ChangelogReleaseOutput> allReleases = readReleases(false).getData();
        for (UpdateCenterOutput.ChangelogReleaseOutput release : allReleases) {
            if (version.trim().equals(release.getVersion())) {
                release.setEntriesOmitted(false);
                return release;
            }
        }
        throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "未找到对应版本更新记录");
    }

    public UpdateCenterOutput.GitHubLogsOutput getGitHubLogs(Integer limit, String before, Boolean force) {
        int normalizedLimit = normalizePositive(limit, DEFAULT_GIT_LOG_LIMIT, MAX_GIT_LOG_LIMIT);
        Path repoRoot = getRepoRoot();
        UpdateCenterOutput.GitHubLogsOutput output = new UpdateCenterOutput.GitHubLogsOutput();
        output.setFetchedAt(nowIso());
        output.setDataSourceAvailable(Files.isDirectory(repoRoot.resolve(".git")));
        output.setSource(output.getDataSourceAvailable() ? UpdateCenterSource.LOCAL.getCode() : UpdateCenterSource.NONE.getCode());
        output.setLogs(new ArrayList<UpdateCenterOutput.GitHubLogEntryOutput>());
        output.setTotalCount(0);
        output.setRepoTotalCommitCount(0);
        output.setHasMore(false);
        output.setNextCursor(null);
        String safeBefore = sanitizeSha(before);
        if (!output.getDataSourceAvailable()) {
            return getGitHubLogsFromApi(normalizedLimit, safeBefore);
        }

        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(repoRoot.toString());
        command.add("log");
        command.add("--format=%H%x1f%an%x1f%aI%x1f%s");
        command.add("--max-count=" + (normalizedLimit + 1));
        if (safeBefore != null) {
            command.add("--skip=1");
            command.add(safeBefore);
        }

        List<String> lines = runCommand(command);
        if (lines.isEmpty()) {
            return getGitHubLogsFromApi(normalizedLimit, safeBefore);
        }
        String commitBaseUrl = getCommitBaseUrl(repoRoot);
        List<UpdateCenterOutput.GitHubLogEntryOutput> logs = new ArrayList<>();
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            String[] parts = line.split(String.valueOf((char) 31), -1);
            if (parts.length < 4) {
                continue;
            }
            UpdateCenterOutput.GitHubLogEntryOutput item = new UpdateCenterOutput.GitHubLogEntryOutput();
            item.setSha(parts[0]);
            item.setShortSha(parts[0].length() > 7 ? parts[0].substring(0, 7) : parts[0]);
            item.setAuthorName(parts[1]);
            item.setCommitTimeUtc(parts[2]);
            item.setMessage(parts[3]);
            item.setAuthorAvatarUrl(null);
            item.setHtmlUrl(commitBaseUrl == null ? null : commitBaseUrl + "/commit/" + parts[0]);
            item.setCoAuthors(new ArrayList<UpdateCenterOutput.GitHubCoAuthorOutput>());
            logs.add(item);
        }

        boolean hasMore = logs.size() > normalizedLimit;
        if (hasMore) {
            logs = new ArrayList<>(logs.subList(0, normalizedLimit));
        }
        output.setLogs(logs);
        output.setTotalCount(logs.size());
        output.setRepoTotalCommitCount(readRepoTotalCommitCount(repoRoot));
        output.setHasMore(hasMore);
        output.setNextCursor(hasMore && !logs.isEmpty() ? logs.get(logs.size() - 1).getSha() : null);
        return output;
    }

    public UpdateCenterOutput.WeeklyReportsOutput getWeeklyReports(Boolean force) {
        SourceResult<List<UpdateCenterOutput.WeeklyReportDetailOutput>> reportResult = readWeeklyReports();
        List<UpdateCenterOutput.WeeklyReportSummaryOutput> reports = new ArrayList<>();
        for (UpdateCenterOutput.WeeklyReportDetailOutput detail : reportResult.getData()) {
            reports.add(toWeeklyReportSummary(detail));
        }

        Collections.sort(reports, new Comparator<UpdateCenterOutput.WeeklyReportSummaryOutput>() {
            @Override
            public int compare(UpdateCenterOutput.WeeklyReportSummaryOutput left,
                               UpdateCenterOutput.WeeklyReportSummaryOutput right) {
                return nullSafe(right.getFileName()).compareTo(nullSafe(left.getFileName()));
            }
        });

        UpdateCenterOutput.WeeklyReportsOutput output = new UpdateCenterOutput.WeeklyReportsOutput();
        output.setDataSourceAvailable(reportResult.isAvailable());
        output.setSource(reportResult.getSource().getCode());
        output.setFetchedAt(nowIso());
        output.setTotalReports(reports.size());
        output.setReports(reports);
        return output;
    }

    public UpdateCenterOutput.WeeklyReportDetailOutput getWeeklyReportDetail(String fileName, Boolean force) {
        if (fileName == null || fileName.trim().isEmpty() || !REPORT_FILE_NAME.matcher(fileName.trim()).matches()) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "周报文件名不合法");
        }
        SourceResult<List<UpdateCenterOutput.WeeklyReportDetailOutput>> reportResult = readWeeklyReports();
        for (UpdateCenterOutput.WeeklyReportDetailOutput detail : reportResult.getData()) {
            if (fileName.trim().equals(detail.getFileName())) {
                detail.setDataSourceAvailable(reportResult.isAvailable());
                detail.setSource(reportResult.getSource().getCode());
                detail.setFetchedAt(nowIso());
                return detail;
            }
        }
        throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "未找到对应周报");
    }

    private SourceResult<List<UpdateCenterOutput.ChangelogFragmentOutput>> readFragments() {
        Path changelogsPath = getChangelogsPath();
        if (!Files.isDirectory(changelogsPath)) {
            return readFragmentsFromGitHub();
        }
        List<UpdateCenterOutput.ChangelogFragmentOutput> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(changelogsPath, "*.md")) {
            for (Path file : stream) {
                UpdateCenterOutput.ChangelogFragmentOutput fragment = new UpdateCenterOutput.ChangelogFragmentOutput();
                fragment.setFileName(file.getFileName().toString());
                fragment.setDate(resolveFragmentDate(file));
                fragment.setEntries(parseEntries(Files.readAllLines(file, StandardCharsets.UTF_8)));
                result.add(fragment);
            }
        } catch (IOException ex) {
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "读取待发布更新失败：" + ex.getMessage());
        }
        return SourceResult.available(result, UpdateCenterSource.LOCAL);
    }

    private SourceResult<List<UpdateCenterOutput.ChangelogReleaseOutput>> readReleases(boolean includeUnreleased) {
        Path changelogPath = getChangelogPath();
        if (!Files.isRegularFile(changelogPath)) {
            return readReleasesFromGitHub(includeUnreleased);
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(changelogPath, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "读取已发布更新失败：" + ex.getMessage());
        }
        return SourceResult.available(parseReleaseLines(lines, includeUnreleased), UpdateCenterSource.LOCAL);
    }

    private SourceResult<List<UpdateCenterOutput.WeeklyReportDetailOutput>> readWeeklyReports() {
        Path reportPath = getReportPath();
        if (!Files.isDirectory(reportPath)) {
            return readWeeklyReportsFromGitHub();
        }
        List<UpdateCenterOutput.WeeklyReportDetailOutput> reports = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(reportPath, "report.*.md")) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                if (!REPORT_FILE_NAME.matcher(fileName).matches()) {
                    continue;
                }
                String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                UpdateCenterOutput.WeeklyReportDetailOutput detail = buildWeeklyReportDetail(
                        fileName,
                        content,
                        Files.getLastModifiedTime(file).toInstant().toString());
                reports.add(detail);
            }
        } catch (IOException ex) {
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "读取周报失败：" + ex.getMessage());
        }
        return SourceResult.available(reports, UpdateCenterSource.LOCAL);
    }

    private SourceResult<List<UpdateCenterOutput.WeeklyReportDetailOutput>> readWeeklyReportsFromGitHub() {
        String body = httpGet(buildGitHubApiUrl("contents/doc", "ref=" + encodeQueryValue(githubBranch)));
        if (body == null || body.trim().isEmpty()) {
            return SourceResult.unavailable(new ArrayList<UpdateCenterOutput.WeeklyReportDetailOutput>());
        }
        List<UpdateCenterOutput.WeeklyReportDetailOutput> reports = new ArrayList<>();
        try {
            JSONArray files = JSON.parseArray(body);
            for (int i = 0; i < files.size(); i++) {
                JSONObject file = files.getJSONObject(i);
                String name = file.getString("name");
                String path = file.getString("path");
                if (name == null || path == null || !REPORT_FILE_NAME.matcher(name).matches()) {
                    continue;
                }
                String content = fetchGitHubFileContent(path);
                if (content == null || content.trim().isEmpty()) {
                    continue;
                }
                reports.add(buildWeeklyReportDetail(name, content, null));
            }
            return SourceResult.available(reports, UpdateCenterSource.GITHUB);
        } catch (RuntimeException ex) {
            return SourceResult.unavailable(new ArrayList<UpdateCenterOutput.WeeklyReportDetailOutput>());
        }
    }

    private UpdateCenterOutput.WeeklyReportDetailOutput buildWeeklyReportDetail(String fileName,
                                                                                String content,
                                                                                String updatedAt) {
        UpdateCenterOutput.WeeklyReportDetailOutput detail = new UpdateCenterOutput.WeeklyReportDetailOutput();
        detail.setFileName(fileName);
        detail.setContent(content);
        detail.setUpdatedAt(updatedAt);
        Matcher fileMatcher = REPORT_FILE_NAME.matcher(fileName);
        if (fileMatcher.matches()) {
            detail.setReportWeek(fileMatcher.group(1));
        }
        for (String line : splitLines(content)) {
            Matcher titleMatcher = REPORT_TITLE.matcher(line.trim());
            if (titleMatcher.matches()) {
                detail.setTitle("周报 " + titleMatcher.group(1).trim());
                detail.setPeriod(titleMatcher.group(2));
                break;
            }
            if (line.startsWith("# ")) {
                detail.setTitle(line.substring(2).trim());
                break;
            }
        }
        if (detail.getTitle() == null || detail.getTitle().trim().isEmpty()) {
            detail.setTitle(fileName);
        }
        if (detail.getPeriod() == null) {
            detail.setPeriod("");
        }
        detail.setDataSourceAvailable(true);
        detail.setSource(UpdateCenterSource.LOCAL.getCode());
        detail.setFetchedAt(nowIso());
        return detail;
    }

    private UpdateCenterOutput.WeeklyReportSummaryOutput toWeeklyReportSummary(
            UpdateCenterOutput.WeeklyReportDetailOutput detail) {
        UpdateCenterOutput.WeeklyReportSummaryOutput summary = new UpdateCenterOutput.WeeklyReportSummaryOutput();
        summary.setFileName(detail.getFileName());
        summary.setTitle(detail.getTitle());
        summary.setReportWeek(detail.getReportWeek());
        summary.setPeriod(detail.getPeriod());
        summary.setUpdatedAt(detail.getUpdatedAt());
        return summary;
    }

    private List<UpdateCenterOutput.ChangelogReleaseOutput> parseReleaseLines(List<String> lines,
                                                                              boolean includeUnreleased) {
        List<UpdateCenterOutput.ChangelogReleaseOutput> releases = new ArrayList<>();
        UpdateCenterOutput.ChangelogReleaseOutput currentRelease = null;
        String currentDay = null;
        Map<String, UpdateCenterOutput.ChangelogDayOutput> currentDays = new LinkedHashMap<>();

        for (String line : lines) {
            Matcher releaseMatcher = RELEASE_HEADER.matcher(line.trim());
            if (releaseMatcher.matches()) {
                appendRelease(releases, currentRelease, currentDays, includeUnreleased);
                currentRelease = createRelease(releaseMatcher.group(1), releaseMatcher.group(2));
                currentDays = new LinkedHashMap<>();
                currentDay = currentRelease.getReleaseDate();
                continue;
            }
            if (currentRelease == null) {
                continue;
            }
            Matcher dayMatcher = DAY_HEADER.matcher(line.trim());
            if (dayMatcher.matches()) {
                currentDay = dayMatcher.group(1);
                continue;
            }
            UpdateCenterOutput.ChangelogEntryOutput entry = parseTableEntry(line);
            if (entry != null) {
                String dayKey = currentDay != null ? currentDay : "未分组";
                UpdateCenterOutput.ChangelogDayOutput day = currentDays.get(dayKey);
                if (day == null) {
                    day = new UpdateCenterOutput.ChangelogDayOutput();
                    day.setDate(dayKey);
                    day.setEntries(new ArrayList<UpdateCenterOutput.ChangelogEntryOutput>());
                    currentDays.put(dayKey, day);
                }
                day.getEntries().add(entry);
            }
        }
        appendRelease(releases, currentRelease, currentDays, includeUnreleased);
        return releases;
    }

    private SourceResult<List<UpdateCenterOutput.ChangelogFragmentOutput>> readFragmentsFromGitHub() {
        String body = httpGet(buildGitHubApiUrl("contents/changelogs", "ref=" + encodeQueryValue(githubBranch)));
        if (body == null || body.trim().isEmpty()) {
            return SourceResult.unavailable(new ArrayList<UpdateCenterOutput.ChangelogFragmentOutput>());
        }
        List<UpdateCenterOutput.ChangelogFragmentOutput> result = new ArrayList<>();
        try {
            JSONArray files = JSON.parseArray(body);
            for (int i = 0; i < files.size(); i++) {
                JSONObject file = files.getJSONObject(i);
                String name = file.getString("name");
                String path = file.getString("path");
                if (name == null || path == null || !name.endsWith(".md")) {
                    continue;
                }
                String content = fetchGitHubFileContent(path);
                if (content == null) {
                    continue;
                }
                UpdateCenterOutput.ChangelogFragmentOutput fragment = new UpdateCenterOutput.ChangelogFragmentOutput();
                fragment.setFileName(name);
                fragment.setDate(resolveFragmentDate(name));
                fragment.setEntries(parseEntries(splitLines(content)));
                result.add(fragment);
            }
            return SourceResult.available(result, UpdateCenterSource.GITHUB);
        } catch (RuntimeException ex) {
            return SourceResult.unavailable(new ArrayList<UpdateCenterOutput.ChangelogFragmentOutput>());
        }
    }

    private SourceResult<List<UpdateCenterOutput.ChangelogReleaseOutput>> readReleasesFromGitHub(boolean includeUnreleased) {
        String content = fetchGitHubFileContent("CHANGELOG.md");
        if (content == null || content.trim().isEmpty()) {
            return SourceResult.unavailable(new ArrayList<UpdateCenterOutput.ChangelogReleaseOutput>());
        }
        return SourceResult.available(parseReleaseLines(splitLines(content), includeUnreleased), UpdateCenterSource.GITHUB);
    }

    private UpdateCenterOutput.GitHubLogsOutput getGitHubLogsFromApi(int normalizedLimit, String safeBefore) {
        UpdateCenterOutput.GitHubLogsOutput output = new UpdateCenterOutput.GitHubLogsOutput();
        output.setFetchedAt(nowIso());
        output.setDataSourceAvailable(false);
        output.setSource(UpdateCenterSource.NONE.getCode());
        output.setLogs(new ArrayList<UpdateCenterOutput.GitHubLogEntryOutput>());
        output.setTotalCount(0);
        output.setRepoTotalCommitCount(null);
        output.setHasMore(false);
        output.setNextCursor(null);

        StringBuilder query = new StringBuilder();
        query.append("sha=").append(encodeQueryValue(safeBefore != null ? safeBefore : githubBranch));
        query.append("&per_page=").append(normalizedLimit + (safeBefore == null ? 1 : 2));
        String body = httpGet(buildGitHubApiUrl("commits", query.toString()));
        if (body == null || body.trim().isEmpty()) {
            return output;
        }

        try {
            JSONArray commits = JSON.parseArray(body);
            List<UpdateCenterOutput.GitHubLogEntryOutput> logs = new ArrayList<>();
            for (int i = 0; i < commits.size(); i++) {
                if (safeBefore != null && i == 0) {
                    continue;
                }
                JSONObject item = commits.getJSONObject(i);
                UpdateCenterOutput.GitHubLogEntryOutput log = mapGitHubCommit(item);
                if (log != null) {
                    logs.add(log);
                }
            }
            boolean hasMore = logs.size() > normalizedLimit;
            if (hasMore) {
                logs = new ArrayList<>(logs.subList(0, normalizedLimit));
            }
            output.setDataSourceAvailable(true);
            output.setSource(UpdateCenterSource.GITHUB.getCode());
            output.setLogs(logs);
            output.setTotalCount(logs.size());
            output.setRepoTotalCommitCount(readGitHubTotalCommitCount());
            output.setHasMore(hasMore);
            output.setNextCursor(hasMore && !logs.isEmpty() ? logs.get(logs.size() - 1).getSha() : null);
            return output;
        } catch (RuntimeException ex) {
            return output;
        }
    }

    private void appendRelease(List<UpdateCenterOutput.ChangelogReleaseOutput> releases,
                               UpdateCenterOutput.ChangelogReleaseOutput release,
                               Map<String, UpdateCenterOutput.ChangelogDayOutput> dayMap,
                               boolean includeUnreleased) {
        if (release == null) {
            return;
        }
        if (!includeUnreleased && isUnreleasedVersion(release.getVersion())) {
            return;
        }
        List<UpdateCenterOutput.ChangelogDayOutput> days = new ArrayList<>(dayMap.values());
        release.setDays(days);
        int entryCount = 0;
        List<String> highlights = new ArrayList<>();
        for (UpdateCenterOutput.ChangelogDayOutput day : days) {
            for (UpdateCenterOutput.ChangelogEntryOutput entry : day.getEntries()) {
                entryCount++;
                if (highlights.size() < 3) {
                    highlights.add(entry.getDescription());
                }
            }
        }
        release.setEntryCount(entryCount);
        release.setHighlights(highlights);
        release.setEntriesOmitted(false);
        releases.add(release);
    }

    private UpdateCenterOutput.ChangelogReleaseOutput createRelease(String rawVersion, String releaseDate) {
        UpdateCenterOutput.ChangelogReleaseOutput release = new UpdateCenterOutput.ChangelogReleaseOutput();
        String version = rawVersion == null ? "" : rawVersion.trim();
        release.setVersion(version);
        release.setReleaseDate(releaseDate);
        release.setSourceScope(isUnreleasedVersion(version) ? "changelog-unreleased-block" : "changelog-release-block");
        release.setHighlights(new ArrayList<String>());
        release.setDays(new ArrayList<UpdateCenterOutput.ChangelogDayOutput>());
        return release;
    }

    private List<UpdateCenterOutput.ChangelogEntryOutput> parseEntries(List<String> lines) {
        List<UpdateCenterOutput.ChangelogEntryOutput> entries = new ArrayList<>();
        for (String line : lines) {
            UpdateCenterOutput.ChangelogEntryOutput entry = parseTableEntry(line);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private UpdateCenterOutput.ChangelogEntryOutput parseTableEntry(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        if (!trimmed.startsWith("|") || !trimmed.endsWith("|")) {
            return null;
        }
        String[] rawParts = trimmed.substring(1, trimmed.length() - 1).split("\\|");
        if (rawParts.length < 3) {
            return null;
        }
        String typeCell = cleanCell(rawParts[0]);
        if (typeCell.contains("---") || "类型".equals(typeCell) || "type".equalsIgnoreCase(typeCell)) {
            return null;
        }
        UpdateCenterOutput.ChangelogEntryOutput entry = new UpdateCenterOutput.ChangelogEntryOutput();
        entry.setType(typeCell);
        entry.setModule(cleanCell(rawParts[1]));
        entry.setDescription(cleanCell(rawParts[2]));
        if (entry.getType().isEmpty() || entry.getModule().isEmpty() || entry.getDescription().isEmpty()) {
            return null;
        }
        return entry;
    }

    private String cleanCell(String raw) {
        return raw == null ? "" : raw.trim().replace("&vert;", "|");
    }

    private String resolveFragmentDate(Path file) {
        String fileName = file.getFileName().toString();
        Matcher matcher = DATE_IN_FILE_NAME.matcher(fileName);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        try {
            return Files.getLastModifiedTime(file).toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString();
        } catch (IOException ex) {
            return LocalDate.now().toString();
        }
    }

    private int countFragmentEntries(List<UpdateCenterOutput.ChangelogFragmentOutput> fragments) {
        int total = 0;
        for (UpdateCenterOutput.ChangelogFragmentOutput fragment : fragments) {
            total += fragment.getEntries() == null ? 0 : fragment.getEntries().size();
        }
        return total;
    }

    private int countReleaseEntries(List<UpdateCenterOutput.ChangelogReleaseOutput> releases) {
        int total = 0;
        for (UpdateCenterOutput.ChangelogReleaseOutput release : releases) {
            total += release.getEntryCount() == null ? 0 : release.getEntryCount();
        }
        return total;
    }

    private String findMinFragmentDate(List<UpdateCenterOutput.ChangelogFragmentOutput> fragments) {
        String min = fragments.get(0).getDate();
        for (UpdateCenterOutput.ChangelogFragmentOutput fragment : fragments) {
            if (nullSafe(fragment.getDate()).compareTo(nullSafe(min)) < 0) {
                min = fragment.getDate();
            }
        }
        return min;
    }

    private String findMaxFragmentDate(List<UpdateCenterOutput.ChangelogFragmentOutput> fragments) {
        String max = fragments.get(0).getDate();
        for (UpdateCenterOutput.ChangelogFragmentOutput fragment : fragments) {
            if (nullSafe(fragment.getDate()).compareTo(nullSafe(max)) > 0) {
                max = fragment.getDate();
            }
        }
        return max;
    }

    private boolean isUnreleasedVersion(String version) {
        return "未发布".equals(version) || "unreleased".equalsIgnoreCase(version);
    }

    private int normalizePositive(Integer raw, int fallback, int max) {
        if (raw == null || raw <= 0) {
            return fallback;
        }
        return Math.min(raw, max);
    }

    private int normalizeOffset(Integer raw) {
        if (raw == null || raw < 0) {
            return 0;
        }
        return raw;
    }

    private String sanitizeSha(String before) {
        if (before == null || before.trim().isEmpty()) {
            return null;
        }
        String trimmed = before.trim();
        return SHA_PATTERN.matcher(trimmed).matches() ? trimmed : null;
    }

    private int readRepoTotalCommitCount(Path repoRoot) {
        List<String> lines = runCommand("git", "-C", repoRoot.toString(), "rev-list", "--count", "HEAD");
        if (lines.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(lines.get(0).trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String getCommitBaseUrl(Path repoRoot) {
        List<String> urls = runCommand("git", "-C", repoRoot.toString(), "remote", "get-url", "origin");
        if (urls.isEmpty()) {
            return null;
        }
        String url = urls.get(0).trim();
        if (url.startsWith("git@github.com:")) {
            url = "https://github.com/" + url.substring("git@github.com:".length());
        }
        if (url.startsWith("https://")) {
            url = url.replaceFirst("https://[^@]+@", "https://");
        }
        if (url.endsWith(".git")) {
            url = url.substring(0, url.length() - 4);
        }
        return url;
    }

    private List<String> runCommand(String... command) {
        List<String> args = new ArrayList<>();
        Collections.addAll(args, command);
        return runCommand(args);
    }

    private List<String> runCommand(List<String> command) {
        List<String> output = new ArrayList<>();
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean finished = process.waitFor(GIT_COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return output;
            }
            if (process.exitValue() != 0) {
                return output;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                }
            }
        } catch (IOException ex) {
            return output;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return output;
        }
        return output;
    }

    private UpdateCenterOutput.GitHubLogEntryOutput mapGitHubCommit(JSONObject item) {
        if (item == null) {
            return null;
        }
        String sha = item.getString("sha");
        JSONObject commit = item.getJSONObject("commit");
        if (sha == null || commit == null) {
            return null;
        }
        JSONObject commitAuthor = commit.getJSONObject("author");
        JSONObject committer = commit.getJSONObject("committer");
        JSONObject githubAuthor = item.getJSONObject("author");

        String message = commit.getString("message");
        String authorName = githubAuthor == null ? null : githubAuthor.getString("login");
        if (authorName == null || authorName.trim().isEmpty()) {
            authorName = commitAuthor == null ? "" : nullSafe(commitAuthor.getString("name"));
        }
        String commitTime = null;
        if (committer != null) {
            commitTime = committer.getString("date");
        }
        if ((commitTime == null || commitTime.trim().isEmpty()) && commitAuthor != null) {
            commitTime = commitAuthor.getString("date");
        }

        UpdateCenterOutput.GitHubLogEntryOutput output = new UpdateCenterOutput.GitHubLogEntryOutput();
        output.setSha(sha);
        output.setShortSha(sha.length() > 7 ? sha.substring(0, 7) : sha);
        output.setMessage(firstLine(message));
        output.setAuthorName(authorName);
        output.setAuthorAvatarUrl(githubAuthor == null ? null : githubAuthor.getString("avatar_url"));
        output.setCommitTimeUtc(commitTime);
        output.setHtmlUrl(item.getString("html_url"));
        output.setCoAuthors(new ArrayList<UpdateCenterOutput.GitHubCoAuthorOutput>());
        return output;
    }

    private Integer readGitHubTotalCommitCount() {
        // GitHub commits API 的总数需要解析 Link header。列表内容更重要，统计失败时前端会降级展示已加载条数。
        return null;
    }

    private String fetchRawGitHubFile(String repoPath) {
        String url = githubRawBase + "/" + encodePath(githubOwner) + "/" + encodePath(githubRepo)
                + "/" + encodePath(githubBranch) + "/" + encodePath(repoPath);
        return httpGet(url);
    }

    private String fetchGitHubFileContent(String repoPath) {
        String rawContent = fetchRawGitHubFile(repoPath);
        if (rawContent != null && !rawContent.trim().isEmpty()) {
            return rawContent;
        }
        return fetchGitHubContentsFile(repoPath);
    }

    private String fetchGitHubContentsFile(String repoPath) {
        String body = httpGet(buildGitHubApiUrl("contents/" + encodePath(repoPath), "ref=" + encodeQueryValue(githubBranch)));
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        try {
            JSONObject file = JSON.parseObject(body);
            String encoding = file.getString("encoding");
            String content = file.getString("content");
            if (!"base64".equalsIgnoreCase(encoding) || content == null || content.trim().isEmpty()) {
                return null;
            }
            byte[] bytes = Base64.getMimeDecoder().decode(content);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String buildGitHubApiUrl(String path, String query) {
        StringBuilder builder = new StringBuilder();
        builder.append(githubApiBase)
                .append("/repos/")
                .append(encodePath(githubOwner))
                .append("/")
                .append(encodePath(githubRepo))
                .append("/")
                .append(path);
        if (query != null && !query.trim().isEmpty()) {
            builder.append("?").append(query);
        }
        return builder.toString();
    }

    private String httpGet(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            applyHttpsCompatibility(connection);
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(HTTP_TIMEOUT_MS);
            connection.setReadTimeout(HTTP_TIMEOUT_MS);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "miduo-ticket-update-center");
            connection.setRequestProperty("Accept", "application/vnd.github+json,text/plain,*/*");
            if (githubToken != null && !githubToken.trim().isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + githubToken.trim());
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
                return builder.toString();
            }
        } catch (IOException ex) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void applyHttpsCompatibility(HttpURLConnection connection) {
        if (!(connection instanceof HttpsURLConnection)) {
            return;
        }
        try {
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, null, null);
            ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
        } catch (Exception ex) {
            // 为什么忽略：容器已配置 -Djdk.tls.client.protocols 时，这里只是额外兜底。
        }
    }

    private List<String> splitLines(String content) {
        List<String> lines = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return lines;
        }
        String[] rawLines = content.split("\\r?\\n");
        Collections.addAll(lines, rawLines);
        return lines;
    }

    private String resolveFragmentDate(String fileName) {
        Matcher matcher = DATE_IN_FILE_NAME.matcher(fileName);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return LocalDate.now().toString();
    }

    private String firstLine(String value) {
        if (value == null) {
            return "";
        }
        int index = value.indexOf('\n');
        return index >= 0 ? value.substring(0, index) : value;
    }

    private String encodePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        String[] segments = path.split("/");
        List<String> encoded = new ArrayList<>();
        for (String segment : segments) {
            encoded.add(encodeQueryValue(segment));
        }
        return String.join("/", encoded);
    }

    private String encodeQueryValue(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException ex) {
            return value == null ? "" : value;
        }
    }

    private String normalizeConfig(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private Path getChangelogPath() {
        return getRepoRoot().resolve("CHANGELOG.md");
    }

    private Path getChangelogsPath() {
        return getRepoRoot().resolve("changelogs");
    }

    private Path getReportPath() {
        return getRepoRoot().resolve("doc");
    }

    private Path getRepoRoot() {
        List<Path> candidates = new ArrayList<>();
        if (configuredRepoRoot != null && !configuredRepoRoot.trim().isEmpty()) {
            candidates.add(Paths.get(configuredRepoRoot.trim()).toAbsolutePath().normalize());
        }
        Path userDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path current = userDir;
        while (current != null) {
            candidates.add(current);
            current = current.getParent();
        }
        candidates.add(Paths.get("/workspace"));
        candidates.add(Paths.get("/app"));

        for (Path candidate : candidates) {
            if (isUpdateCenterRoot(candidate)) {
                return candidate;
            }
        }
        return userDir;
    }

    private boolean isUpdateCenterRoot(Path candidate) {
        if (candidate == null || !Files.isDirectory(candidate)) {
            return false;
        }
        return Files.isDirectory(candidate.resolve(".git"))
                || Files.isRegularFile(candidate.resolve("CHANGELOG.md"))
                || Files.isDirectory(candidate.resolve("changelogs"));
    }

    private String nowIso() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static class SourceResult<T> {

        private final T data;
        private final boolean available;
        private final UpdateCenterSource source;

        private SourceResult(T data, boolean available, UpdateCenterSource source) {
            this.data = data;
            this.available = available;
            this.source = source;
        }

        private static <T> SourceResult<T> available(T data, UpdateCenterSource source) {
            return new SourceResult<>(data, true, source);
        }

        private static <T> SourceResult<T> unavailable(T data) {
            return new SourceResult<>(data, false, UpdateCenterSource.NONE);
        }

        private T getData() {
            return data;
        }

        private boolean isAvailable() {
            return available;
        }

        private UpdateCenterSource getSource() {
            return source;
        }
    }
}
