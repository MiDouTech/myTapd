package com.miduo.cloud.ticket.application.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.BugChangeTypeEnum;
import com.miduo.cloud.ticket.common.enums.BugReproduceEnv;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.enums.SeverityLevel;
import com.miduo.cloud.ticket.common.enums.TicketStatus;
import com.miduo.cloud.ticket.common.enums.ValidReportOption;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.ticket.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 缺陷工单扩展信息应用服务
 */
@Service
public class TicketBugApplicationService extends BaseApplicationService {

    private final TicketMapper ticketMapper;
    private final TicketBugInfoMapper bugInfoMapper;
    private final TicketBugTestInfoMapper bugTestInfoMapper;
    private final TicketBugDevInfoMapper bugDevInfoMapper;
    private final TicketChangeHistoryRecorder changeHistoryRecorder;

    public TicketBugApplicationService(TicketMapper ticketMapper,
                                       TicketBugInfoMapper bugInfoMapper,
                                       TicketBugTestInfoMapper bugTestInfoMapper,
                                       TicketBugDevInfoMapper bugDevInfoMapper,
                                       TicketChangeHistoryRecorder changeHistoryRecorder) {
        this.ticketMapper = ticketMapper;
        this.bugInfoMapper = bugInfoMapper;
        this.bugTestInfoMapper = bugTestInfoMapper;
        this.bugDevInfoMapper = bugDevInfoMapper;
        this.changeHistoryRecorder = changeHistoryRecorder;
    }

    /**
     * 系统自动初始化缺陷工单客服信息（企微机器人建单时自动填充，跳过状态和权限校验）
     * 仅在工单刚创建、客服信息记录不存在时执行，不覆盖已有数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void initCustomerInfoFromBot(Long ticketId, TicketBugCustomerInfoInput input) {
        if (ticketId == null || input == null) {
            return;
        }
        TicketBugInfoPO existing = getBugInfoByTicketId(ticketId);
        if (existing != null) {
            return;
        }
        TicketBugInfoPO infoPO = new TicketBugInfoPO();
        infoPO.setTicketId(ticketId);
        applyCustomerInfoChanges(infoPO, input);
        bugInfoMapper.insert(infoPO);
    }

    /**
     * 更新缺陷工单客服信息
     * 变更检测必须在 apply 赋值之前，保证对比的是数据库中的旧值
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomerInfo(Long ticketId, TicketBugCustomerInfoInput input, Long currentUserId) {
        TicketPO ticket = requireTicket(ticketId);

        TicketBugInfoPO infoPO = getOrCreateBugInfo(ticketId);
        TicketBugCustomerInfoInput normalizedInput = normalizeCustomerInfoInput(input);
        validateManualValidReportUpdate(ticket, infoPO, normalizedInput);
        List<BugFieldChangeItem> changes = changeHistoryRecorder.detectCustomerInfoChanges(infoPO, normalizedInput);

        applyCustomerInfoChanges(infoPO, normalizedInput);
        saveBugInfo(infoPO);

        changeHistoryRecorder.recordWithTimeTrack(ticketId, currentUserId, BugChangeTypeEnum.MANUAL_CHANGE, changes);
    }

    /**
     * 更新缺陷工单测试信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTestInfo(Long ticketId, TicketBugTestInfoInput input, Long currentUserId) {
        requireTicket(ticketId);

        TicketBugTestInfoInput normalizedInput = normalizeTestInfoInput(input);
        TicketBugTestInfoPO testInfoPO = getOrCreateBugTestInfo(ticketId);
        List<BugFieldChangeItem> changes = changeHistoryRecorder.detectTestInfoChanges(testInfoPO, normalizedInput);

        applyTestInfoChanges(testInfoPO, normalizedInput);
        saveBugTestInfo(testInfoPO);

        changeHistoryRecorder.recordWithTimeTrack(ticketId, currentUserId, BugChangeTypeEnum.MANUAL_CHANGE, changes);
    }

    /**
     * 更新缺陷工单开发信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateDevInfo(Long ticketId, TicketBugDevInfoInput input, Long currentUserId) {
        requireTicket(ticketId);

        TicketBugDevInfoPO devInfoPO = getOrCreateBugDevInfo(ticketId);
        List<BugFieldChangeItem> changes = changeHistoryRecorder.detectDevInfoChanges(devInfoPO, input);

        applyDevInfoChanges(devInfoPO, input);
        if (devInfoPO.getPlannedFullResolveAt() != null) {
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            LocalDate plannedDay = devInfoPO.getPlannedFullResolveAt().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            if (plannedDay.isBefore(today)) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "计划彻底解决时间不能早于今天");
            }
        }
        saveBugDevInfo(devInfoPO);

        changeHistoryRecorder.recordWithTimeTrack(ticketId, currentUserId, BugChangeTypeEnum.MANUAL_CHANGE, changes);
    }

    public TicketBugCustomerInfoOutput getCustomerInfo(Long ticketId) {
        TicketBugInfoPO po = getBugInfoByTicketId(ticketId);
        if (po == null) {
            return null;
        }
        TicketBugCustomerInfoOutput output = new TicketBugCustomerInfoOutput();
        output.setTicketId(po.getTicketId());
        output.setMerchantNo(po.getMerchantNo());
        output.setCompanyName(po.getCompanyName());
        output.setMerchantAccount(po.getMerchantAccount());
        output.setProblemDesc(po.getProblemDesc());
        output.setExpectedResult(po.getExpectedResult());
        output.setSceneCode(po.getSceneCode());
        output.setProblemScreenshot(po.getProblemScreenshot());
        output.setManualValidReport(po.getManualValidReport());
        return output;
    }

    public TicketBugTestInfoOutput getTestInfo(Long ticketId) {
        TicketBugTestInfoPO po = getBugTestInfoByTicketId(ticketId);
        if (po == null) {
            return null;
        }
        TicketBugTestInfoOutput output = new TicketBugTestInfoOutput();
        output.setTicketId(po.getTicketId());
        output.setReproduceEnv(po.getReproduceEnv());
        output.setReproduceSteps(po.getReproduceSteps());
        output.setActualResult(po.getActualResult());
        output.setImpactScope(po.getImpactScope());
        output.setSeverityLevel(po.getSeverityLevel());
        output.setModuleName(po.getModuleName());
        output.setReproduceScreenshot(po.getReproduceScreenshot());
        output.setTestRemark(po.getTestRemark());
        return output;
    }

    public TicketBugDevInfoOutput getDevInfo(Long ticketId) {
        TicketBugDevInfoPO po = getBugDevInfoByTicketId(ticketId);
        if (po == null) {
            return null;
        }
        TicketBugDevInfoOutput output = new TicketBugDevInfoOutput();
        output.setTicketId(po.getTicketId());
        output.setRootCause(po.getRootCause());
        output.setFixSolution(po.getFixSolution());
        output.setGitBranch(po.getGitBranch());
        output.setImpactAssessment(po.getImpactAssessment());
        output.setDevRemark(po.getDevRemark());
        output.setPlannedFullResolveAt(po.getPlannedFullResolveAt());
        return output;
    }

    /**
     * 缺陷工作流：测试复现中 → 待开发受理前，复现环境和缺陷等级必须已填写（流转参数或测试信息表）
     */
    @Transactional(rollbackFor = Exception.class)
    public void requireTestInfoBeforeDevTransfer(Long ticketId, String reproduceEnvFromTransit,
                                                 String severityLevelFromTransit) {
        requireTicket(ticketId);
        TicketBugTestInfoPO testInfoPO = null;
        if (StringUtils.hasText(reproduceEnvFromTransit) || StringUtils.hasText(severityLevelFromTransit)) {
            // 这里先保存弹窗里的值，是因为后续 Bug 简报会从测试信息表自动带出缺陷等级。
            testInfoPO = getOrCreateBugTestInfo(ticketId);
        }
        if (StringUtils.hasText(reproduceEnvFromTransit)) {
            String code = reproduceEnvFromTransit.trim();
            if (BugReproduceEnv.fromCode(code) == null) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR,
                        "复现环境取值无效，请选择：生产环境(PRODUCTION)、测试环境(TEST) 或 均可复现(BOTH)");
            }
            testInfoPO.setReproduceEnv(code);
        }
        if (StringUtils.hasText(severityLevelFromTransit)) {
            testInfoPO.setSeverityLevel(normalizeSeverityLevel(severityLevelFromTransit));
        }
        if (testInfoPO != null) {
            saveBugTestInfo(testInfoPO);
        }

        TicketBugTestInfoPO savedTestInfoPO = testInfoPO != null ? testInfoPO : getBugTestInfoByTicketId(ticketId);
        String env = savedTestInfoPO != null ? savedTestInfoPO.getReproduceEnv() : null;
        if (!StringUtils.hasText(env) || BugReproduceEnv.fromCode(env.trim()) == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR,
                    "确认缺陷转开发前必须填写复现环境，请在流转时选择或在测试信息中维护");
        }
        String severity = savedTestInfoPO != null ? savedTestInfoPO.getSeverityLevel() : null;
        if (!StringUtils.hasText(severity)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR,
                    "确认缺陷转开发前必须填写缺陷等级，请在流转时选择或在测试信息中维护");
        }
        normalizeSeverityLevel(severity);
    }

    /**
     * 缺陷工作流：进入临时解决前必须存在「计划彻底解决时间」（流转参数或开发信息表），且不得早于当天 0 点
     */
    @Transactional(rollbackFor = Exception.class)
    public void requireAndPersistPlannedFullResolveForTempResolved(Long ticketId, String plannedFullResolveAtRaw) {
        requireTicket(ticketId);
        Date planned = parsePlannedFullResolveAt(plannedFullResolveAtRaw);
        if (planned == null) {
            TicketBugDevInfoPO existing = getBugDevInfoByTicketId(ticketId);
            planned = existing != null ? existing.getPlannedFullResolveAt() : null;
        }
        if (planned == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR,
                    "临时解决必须填写计划彻底解决时间，请在流转时选择或在开发信息中维护");
        }
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate plannedDay = planned.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (plannedDay.isBefore(today)) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "计划彻底解决时间不能早于今天");
        }
        TicketBugDevInfoPO devInfoPO = getOrCreateBugDevInfo(ticketId);
        devInfoPO.setPlannedFullResolveAt(planned);
        saveBugDevInfo(devInfoPO);
    }

    private Date parsePlannedFullResolveAt(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String s = raw.trim();
        try {
            LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException ignored) {
            // fall through
        }
        try {
            LocalDate ld = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
            return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException e) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR,
                    "计划彻底解决时间格式无效，请使用 yyyy-MM-dd 或 yyyy-MM-ddTHH:mm:ss");
        }
    }

    private void applyCustomerInfoChanges(TicketBugInfoPO infoPO, TicketBugCustomerInfoInput input) {
        infoPO.setMerchantNo(input.getMerchantNo());
        infoPO.setCompanyName(input.getCompanyName());
        infoPO.setMerchantAccount(input.getMerchantAccount());
        infoPO.setProblemDesc(input.getProblemDesc());
        infoPO.setExpectedResult(input.getExpectedResult());
        infoPO.setSceneCode(input.getSceneCode());
        infoPO.setProblemScreenshot(input.getProblemScreenshot());
        infoPO.setManualValidReport(input.getManualValidReport());
    }

    private TicketBugCustomerInfoInput normalizeCustomerInfoInput(TicketBugCustomerInfoInput input) {
        TicketBugCustomerInfoInput normalized = new TicketBugCustomerInfoInput();
        normalized.setMerchantNo(input.getMerchantNo());
        normalized.setCompanyName(input.getCompanyName());
        normalized.setMerchantAccount(input.getMerchantAccount());
        normalized.setProblemDesc(input.getProblemDesc());
        normalized.setExpectedResult(input.getExpectedResult());
        normalized.setSceneCode(input.getSceneCode());
        normalized.setProblemScreenshot(input.getProblemScreenshot());
        normalized.setManualValidReport(normalizeManualValidReport(input.getManualValidReport()));
        return normalized;
    }

    private void validateManualValidReportUpdate(TicketPO ticket, TicketBugInfoPO existing, TicketBugCustomerInfoInput input) {
        String oldVal = existing == null ? null : existing.getManualValidReport();
        String newVal = input == null ? null : input.getManualValidReport();
        if (Objects.equals(trimToNull(oldVal), trimToNull(newVal))) {
            return;
        }
        TicketStatus status = TicketStatus.fromCode(ticket.getStatus());
        if (status != TicketStatus.CLOSED) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "仅已关闭工单可手工设置有效报告");
        }
    }

    private String normalizeManualValidReport(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        ValidReportOption option = ValidReportOption.fromCode(source);
        if (option == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "有效报告仅支持 YES 或 NO");
        }
        return option.getCode();
    }

    private String trimToNull(String source) {
        if (source == null) {
            return null;
        }
        String v = source.trim();
        return v.isEmpty() ? null : v;
    }

    private void applyTestInfoChanges(TicketBugTestInfoPO testInfoPO, TicketBugTestInfoInput input) {
        testInfoPO.setReproduceEnv(input.getReproduceEnv());
        testInfoPO.setReproduceSteps(input.getReproduceSteps());
        testInfoPO.setActualResult(input.getActualResult());
        testInfoPO.setImpactScope(input.getImpactScope());
        testInfoPO.setSeverityLevel(input.getSeverityLevel());
        testInfoPO.setModuleName(input.getModuleName());
        testInfoPO.setReproduceScreenshot(input.getReproduceScreenshot());
        testInfoPO.setTestRemark(input.getTestRemark());
    }

    private TicketBugTestInfoInput normalizeTestInfoInput(TicketBugTestInfoInput input) {
        TicketBugTestInfoInput normalized = new TicketBugTestInfoInput();
        normalized.setReproduceEnv(input.getReproduceEnv());
        normalized.setReproduceSteps(input.getReproduceSteps());
        normalized.setActualResult(input.getActualResult());
        normalized.setImpactScope(input.getImpactScope());
        normalized.setSeverityLevel(normalizeSeverityLevel(input.getSeverityLevel()));
        normalized.setModuleName(input.getModuleName());
        normalized.setReproduceScreenshot(input.getReproduceScreenshot());
        normalized.setTestRemark(input.getTestRemark());
        return normalized;
    }

    private void applyDevInfoChanges(TicketBugDevInfoPO devInfoPO, TicketBugDevInfoInput input) {
        devInfoPO.setRootCause(input.getRootCause());
        devInfoPO.setFixSolution(input.getFixSolution());
        devInfoPO.setGitBranch(input.getGitBranch());
        devInfoPO.setImpactAssessment(input.getImpactAssessment());
        devInfoPO.setDevRemark(input.getDevRemark());
        devInfoPO.setPlannedFullResolveAt(input.getPlannedFullResolveAt());
    }

    private TicketPO requireTicket(Long ticketId) {
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }
        return ticket;
    }

    private String normalizeSeverityLevel(String source) {
        if (!StringUtils.hasText(source)) {
            return source;
        }
        String value = source.trim().toUpperCase(Locale.ROOT);
        switch (value) {
            case "FATAL":
                value = "P0";
                break;
            case "CRITICAL":
                value = "P1";
                break;
            case "NORMAL":
                value = "P2";
                break;
            case "MINOR":
                value = "P3";
                break;
            default:
                break;
        }
        SeverityLevel level = SeverityLevel.fromCode(value);
        if (level == null) {
            throw BusinessException.of(ErrorCode.PARAM_ERROR, "缺陷等级仅支持P0-P4");
        }
        return level.getCode();
    }

    private TicketBugInfoPO getOrCreateBugInfo(Long ticketId) {
        TicketBugInfoPO existing = getBugInfoByTicketId(ticketId);
        if (existing != null) {
            return existing;
        }
        TicketBugInfoPO infoPO = new TicketBugInfoPO();
        infoPO.setTicketId(ticketId);
        return infoPO;
    }

    private TicketBugInfoPO getBugInfoByTicketId(Long ticketId) {
        return bugInfoMapper.selectOne(new LambdaQueryWrapper<TicketBugInfoPO>()
                .eq(TicketBugInfoPO::getTicketId, ticketId)
                .last("LIMIT 1"));
    }

    private void saveBugInfo(TicketBugInfoPO infoPO) {
        if (infoPO.getId() == null) {
            bugInfoMapper.insert(infoPO);
        } else {
            bugInfoMapper.updateById(infoPO);
        }
    }

    private TicketBugTestInfoPO getOrCreateBugTestInfo(Long ticketId) {
        TicketBugTestInfoPO existing = getBugTestInfoByTicketId(ticketId);
        if (existing != null) {
            return existing;
        }
        TicketBugTestInfoPO infoPO = new TicketBugTestInfoPO();
        infoPO.setTicketId(ticketId);
        return infoPO;
    }

    private TicketBugTestInfoPO getBugTestInfoByTicketId(Long ticketId) {
        return bugTestInfoMapper.selectOne(new LambdaQueryWrapper<TicketBugTestInfoPO>()
                .eq(TicketBugTestInfoPO::getTicketId, ticketId)
                .last("LIMIT 1"));
    }

    private void saveBugTestInfo(TicketBugTestInfoPO infoPO) {
        if (infoPO.getId() == null) {
            bugTestInfoMapper.insert(infoPO);
        } else {
            bugTestInfoMapper.updateById(infoPO);
        }
    }

    private TicketBugDevInfoPO getOrCreateBugDevInfo(Long ticketId) {
        TicketBugDevInfoPO existing = getBugDevInfoByTicketId(ticketId);
        if (existing != null) {
            return existing;
        }
        TicketBugDevInfoPO infoPO = new TicketBugDevInfoPO();
        infoPO.setTicketId(ticketId);
        return infoPO;
    }

    private TicketBugDevInfoPO getBugDevInfoByTicketId(Long ticketId) {
        return bugDevInfoMapper.selectOne(new LambdaQueryWrapper<TicketBugDevInfoPO>()
                .eq(TicketBugDevInfoPO::getTicketId, ticketId)
                .last("LIMIT 1"));
    }

    private void saveBugDevInfo(TicketBugDevInfoPO infoPO) {
        if (infoPO.getId() == null) {
            bugDevInfoMapper.insert(infoPO);
        } else {
            bugDevInfoMapper.updateById(infoPO);
        }
    }
}
