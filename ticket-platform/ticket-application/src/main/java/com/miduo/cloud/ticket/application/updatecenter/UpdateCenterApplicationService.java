package com.miduo.cloud.ticket.application.updatecenter;

import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.UpdateCenterSource;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.updatecenter.UpdateCenterOutput;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Pattern DATE_IN_FILE_NAME = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}).*");
    private static final Pattern RELEASE_HEADER = Pattern.compile("^##\\s+\\[?([^\\]\\-]+)]?\\s*(?:-\\s*(\\d{4}-\\d{2}-\\d{2}))?.*$");
    private static final Pattern DAY_HEADER = Pattern.compile("^###\\s+(\\d{4}-\\d{2}-\\d{2}).*$");
    private static final Pattern SHA_PATTERN = Pattern.compile("^[0-9a-fA-F]{4,40}$");

    private final String configuredRepoRoot;

    public UpdateCenterApplicationService(@Value("${update-center.repo-root:}") String configuredRepoRoot) {
        this.configuredRepoRoot = configuredRepoRoot;
    }

    public UpdateCenterOutput.CurrentWeekOutput getCurrentWeek(Integer daysLimit, Integer daysOffset, Boolean force) {
        List<UpdateCenterOutput.ChangelogFragmentOutput> allFragments = readFragments();
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
        output.setDataSourceAvailable(Files.isDirectory(getChangelogsPath()));
        output.setSource(output.getDataSourceAvailable() ? UpdateCenterSource.LOCAL.getCode() : UpdateCenterSource.NONE.getCode());
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
        List<UpdateCenterOutput.ChangelogReleaseOutput> allReleases = readReleases(false);
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
        output.setDataSourceAvailable(Files.isRegularFile(getChangelogPath()));
        output.setSource(output.getDataSourceAvailable() ? UpdateCenterSource.LOCAL.getCode() : UpdateCenterSource.NONE.getCode());
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
        List<UpdateCenterOutput.ChangelogReleaseOutput> allReleases = readReleases(false);
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
        if (!output.getDataSourceAvailable()) {
            return output;
        }

        String safeBefore = sanitizeSha(before);
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

    private List<UpdateCenterOutput.ChangelogFragmentOutput> readFragments() {
        Path changelogsPath = getChangelogsPath();
        if (!Files.isDirectory(changelogsPath)) {
            return new ArrayList<>();
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
        return result;
    }

    private List<UpdateCenterOutput.ChangelogReleaseOutput> readReleases(boolean includeUnreleased) {
        Path changelogPath = getChangelogPath();
        if (!Files.isRegularFile(changelogPath)) {
            return new ArrayList<>();
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(changelogPath, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "读取已发布更新失败：" + ex.getMessage());
        }

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

    private Path getChangelogPath() {
        return getRepoRoot().resolve("CHANGELOG.md");
    }

    private Path getChangelogsPath() {
        return getRepoRoot().resolve("changelogs");
    }

    private Path getRepoRoot() {
        if (configuredRepoRoot != null && !configuredRepoRoot.trim().isEmpty()) {
            return Paths.get(configuredRepoRoot.trim()).toAbsolutePath().normalize();
        }
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve(".git"))) {
                return current;
            }
            current = current.getParent();
        }
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    private String nowIso() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
