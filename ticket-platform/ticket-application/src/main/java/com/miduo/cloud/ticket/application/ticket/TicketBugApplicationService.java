package com.miduo.cloud.ticket.application.ticket;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.ticket.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.mapper.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.ticket.po.*;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 缺陷工单扩展信息应用服务
 */
@Service
public class TicketBugApplicationService extends BaseApplicationService {

    private static final Set<String> CUSTOMER_EDIT_STATUSES = new HashSet<>(Arrays.asList(
            "PENDING_DISPATCH", "PENDING_TEST", "PENDING_TEST_ACCEPT", "PENDING_CS_CONFIRM"
    ));

    private static final Set<String> TEST_EDIT_STATUSES = new HashSet<>(Arrays.asList(
            "PENDING_TEST", "PENDING_TEST_ACCEPT", "TESTING", "PENDING_VERIFY"
    ));

    private static final Set<String> DEV_EDIT_STATUSES = new HashSet<>(Arrays.asList(
            "PENDING_DEV", "PENDING_DEV_ACCEPT", "DEVELOPING", "PENDING_VERIFY"
    ));

    private final TicketMapper ticketMapper;
    private final TicketBugInfoMapper bugInfoMapper;
    private final TicketBugTestInfoMapper bugTestInfoMapper;
    private final TicketBugDevInfoMapper bugDevInfoMapper;
    private final SysUserMapper sysUserMapper;

    public TicketBugApplicationService(TicketMapper ticketMapper,
                                       TicketBugInfoMapper bugInfoMapper,
                                       TicketBugTestInfoMapper bugTestInfoMapper,
                                       TicketBugDevInfoMapper bugDevInfoMapper,
                                       SysUserMapper sysUserMapper) {
        this.ticketMapper = ticketMapper;
        this.bugInfoMapper = bugInfoMapper;
        this.bugTestInfoMapper = bugTestInfoMapper;
        this.bugDevInfoMapper = bugDevInfoMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 更新缺陷工单客服信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomerInfo(Long ticketId, TicketBugCustomerInfoInput input, Long currentUserId) {
        TicketPO ticket = requireTicket(ticketId);
        List<String> roleCodes = getRoleCodes(currentUserId);
        assertCanEditCustomerInfo(ticket, currentUserId, roleCodes);

        TicketBugInfoPO infoPO = getOrCreateBugInfo(ticketId);
        infoPO.setMerchantNo(input.getMerchantNo());
        infoPO.setCompanyName(input.getCompanyName());
        infoPO.setMerchantAccount(input.getMerchantAccount());
        infoPO.setProblemDesc(input.getProblemDesc());
        infoPO.setExpectedResult(input.getExpectedResult());
        infoPO.setSceneCode(input.getSceneCode());
        infoPO.setProblemScreenshot(input.getProblemScreenshot());
        saveBugInfo(infoPO);
    }

    /**
     * 更新缺陷工单测试信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTestInfo(Long ticketId, TicketBugTestInfoInput input, Long currentUserId) {
        TicketPO ticket = requireTicket(ticketId);
        List<String> roleCodes = getRoleCodes(currentUserId);
        assertCanEditTestInfo(ticket, currentUserId, roleCodes);

        TicketBugTestInfoPO testInfoPO = getOrCreateBugTestInfo(ticketId);
        testInfoPO.setReproduceEnv(input.getReproduceEnv());
        testInfoPO.setReproduceSteps(input.getReproduceSteps());
        testInfoPO.setActualResult(input.getActualResult());
        testInfoPO.setImpactScope(input.getImpactScope());
        testInfoPO.setSeverityLevel(input.getSeverityLevel());
        testInfoPO.setModuleName(input.getModuleName());
        testInfoPO.setReproduceScreenshot(input.getReproduceScreenshot());
        testInfoPO.setTestRemark(input.getTestRemark());
        saveBugTestInfo(testInfoPO);
    }

    /**
     * 更新缺陷工单开发信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateDevInfo(Long ticketId, TicketBugDevInfoInput input, Long currentUserId) {
        TicketPO ticket = requireTicket(ticketId);
        List<String> roleCodes = getRoleCodes(currentUserId);
        assertCanEditDevInfo(ticket, currentUserId, roleCodes);

        TicketBugDevInfoPO devInfoPO = getOrCreateBugDevInfo(ticketId);
        devInfoPO.setRootCause(input.getRootCause());
        devInfoPO.setFixSolution(input.getFixSolution());
        devInfoPO.setGitBranch(input.getGitBranch());
        devInfoPO.setImpactAssessment(input.getImpactAssessment());
        devInfoPO.setDevRemark(input.getDevRemark());
        saveBugDevInfo(devInfoPO);
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
        return output;
    }

    private TicketPO requireTicket(Long ticketId) {
        TicketPO ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw BusinessException.of(ErrorCode.TICKET_NOT_FOUND);
        }
        return ticket;
    }

    private List<String> getRoleCodes(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<String> roleCodes = sysUserMapper.selectRoleCodesByUserId(userId);
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return roleCodes.stream()
                .filter(Objects::nonNull)
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toList());
    }

    private void assertCanEditCustomerInfo(TicketPO ticket, Long currentUserId, List<String> roleCodes) {
        String status = normalizeStatus(ticket.getStatus());
        if (isAdmin(roleCodes)) {
            return;
        }
        if (!CUSTOMER_EDIT_STATUSES.contains(status)) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "当前状态不允许编辑客服信息");
        }
        if (hasAnyRole(roleCodes, "CUSTOMER_SERVICE", "SUBMITTER")) {
            return;
        }
        if (currentUserId != null && currentUserId.equals(ticket.getCreatorId())) {
            return;
        }
        throw BusinessException.of(ErrorCode.FORBIDDEN, "无权限编辑客服信息");
    }

    private void assertCanEditTestInfo(TicketPO ticket, Long currentUserId, List<String> roleCodes) {
        String status = normalizeStatus(ticket.getStatus());
        if (isAdmin(roleCodes)) {
            return;
        }
        if (!TEST_EDIT_STATUSES.contains(status)) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "当前状态不允许编辑测试信息");
        }
        if (hasAnyRole(roleCodes, "TESTER")) {
            return;
        }
        if (hasAnyRole(roleCodes, "HANDLER") && isTestStage(status)) {
            return;
        }
        if (currentUserId != null && currentUserId.equals(ticket.getAssigneeId()) && isTestStage(status)) {
            return;
        }
        throw BusinessException.of(ErrorCode.FORBIDDEN, "无权限编辑测试信息");
    }

    private void assertCanEditDevInfo(TicketPO ticket, Long currentUserId, List<String> roleCodes) {
        String status = normalizeStatus(ticket.getStatus());
        if (isAdmin(roleCodes)) {
            return;
        }
        if (!DEV_EDIT_STATUSES.contains(status)) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "当前状态不允许编辑开发信息");
        }
        if (hasAnyRole(roleCodes, "DEVELOPER")) {
            return;
        }
        if (hasAnyRole(roleCodes, "HANDLER") && isDevStage(status)) {
            return;
        }
        if (currentUserId != null && currentUserId.equals(ticket.getAssigneeId()) && isDevStage(status)) {
            return;
        }
        throw BusinessException.of(ErrorCode.FORBIDDEN, "无权限编辑开发信息");
    }

    private boolean isAdmin(List<String> roleCodes) {
        return hasAnyRole(roleCodes, "ADMIN", "TICKET_ADMIN");
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

    private boolean isTestStage(String status) {
        return "PENDING_TEST".equals(status)
                || "PENDING_TEST_ACCEPT".equals(status)
                || "TESTING".equals(status)
                || "PENDING_VERIFY".equals(status);
    }

    private boolean isDevStage(String status) {
        return "PENDING_DEV".equals(status)
                || "PENDING_DEV_ACCEPT".equals(status)
                || "DEVELOPING".equals(status)
                || "PENDING_VERIFY".equals(status);
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
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
