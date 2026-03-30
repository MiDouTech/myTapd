package com.miduo.cloud.ticket.application.wecom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomConfigOutput;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomConfigUpdateInput;
import com.miduo.cloud.ticket.entity.dto.wecom.WecomConnectionTestOutput;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomClient;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomTokenManager;
import com.miduo.cloud.ticket.infrastructure.external.wework.WeworkSecretCodec;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.mapper.SysWeworkConfigMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.system.po.SysWeworkConfigPO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 企业微信连接配置应用服务
 */
@Service
public class WecomConfigApplicationService extends BaseApplicationService {

    private final SysWeworkConfigMapper sysWeworkConfigMapper;
    private final WeworkSecretCodec weworkSecretCodec;
    private final WecomClient wecomClient;
    private final WecomTokenManager wecomTokenManager;

    public WecomConfigApplicationService(SysWeworkConfigMapper sysWeworkConfigMapper,
                                         WeworkSecretCodec weworkSecretCodec,
                                         WecomClient wecomClient,
                                         WecomTokenManager wecomTokenManager) {
        this.sysWeworkConfigMapper = sysWeworkConfigMapper;
        this.weworkSecretCodec = weworkSecretCodec;
        this.wecomClient = wecomClient;
        this.wecomTokenManager = wecomTokenManager;
    }

    public WecomConfigOutput detail() {
        SysWeworkConfigPO po = getLatestConfig();
        if (po == null) {
            return null;
        }
        return toOutput(po);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveOrUpdate(WecomConfigUpdateInput input) {
        SysWeworkConfigPO existing = getLatestConfig();
        if (existing == null) {
            String plainSecret = input.getCorpSecret() == null ? "" : input.getCorpSecret().trim();
            if (plainSecret.isEmpty()) {
                throw BusinessException.of(ErrorCode.PARAM_ERROR, "首次保存必须填写应用Secret");
            }
            SysWeworkConfigPO po = new SysWeworkConfigPO();
            applyNonSecretFields(po, input);
            po.setCorpSecret(weworkSecretCodec.encode(plainSecret));
            po.setCallbackToken(normalizeCallbackToken(input.getCallbackToken(), true, null));
            po.setCallbackAesKey(normalizeCallbackAesKey(input.getCallbackAesKey(), true, null));
            sysWeworkConfigMapper.insert(po);
            wecomTokenManager.refreshAccessToken();
            return po.getId();
        }

        String newCorpSecretStored = mergeCorpSecretStored(existing, input);
        String newCallbackToken = normalizeCallbackToken(input.getCallbackToken(), false, existing.getCallbackToken());
        String newCallbackAesKey = normalizeCallbackAesKey(input.getCallbackAesKey(), false, existing.getCallbackAesKey());

        SysWeworkConfigPO proposed = new SysWeworkConfigPO();
        proposed.setId(existing.getId());
        applyNonSecretFields(proposed, input);
        proposed.setCorpSecret(newCorpSecretStored);
        proposed.setCallbackToken(newCallbackToken);
        proposed.setCallbackAesKey(newCallbackAesKey);

        if (isWecomConfigUnchanged(existing, proposed)) {
            return existing.getId();
        }

        proposed.setCreateTime(existing.getCreateTime());
        proposed.setCreateBy(existing.getCreateBy());
        sysWeworkConfigMapper.updateById(proposed);
        wecomTokenManager.refreshAccessToken();
        return proposed.getId();
    }

    private void applyNonSecretFields(SysWeworkConfigPO po, WecomConfigUpdateInput input) {
        po.setCorpId(input.getCorpId().trim());
        po.setAgentId(input.getAgentId().trim());
        po.setApiBaseUrl(input.getApiBaseUrl().trim());
        po.setConnectTimeoutMs(input.getConnectTimeoutMs());
        po.setReadTimeoutMs(input.getReadTimeoutMs());
        po.setScheduleEnabled(Boolean.TRUE.equals(input.getScheduleEnabled()) ? 1 : 0);
        po.setScheduleCron(input.getScheduleCron());
        po.setRetryCount(input.getRetryCount());
        po.setBatchSize(input.getBatchSize());
        po.setStatus(Boolean.FALSE.equals(input.getEnabled()) ? 0 : 1);
    }

    private String mergeCorpSecretStored(SysWeworkConfigPO existing, WecomConfigUpdateInput input) {
        String plain = input.getCorpSecret() == null ? "" : input.getCorpSecret().trim();
        if (plain.isEmpty()) {
            return existing.getCorpSecret();
        }
        return weworkSecretCodec.encode(plain);
    }

    /**
     * 新建：null/空白写入空串；更新：null 或空白表示保留原值。
     */
    private String normalizeCallbackToken(String raw, boolean isInsert, String previous) {
        if (raw == null) {
            return isInsert ? "" : previous;
        }
        String trimmed = raw.trim();
        if (!isInsert && trimmed.isEmpty()) {
            return previous;
        }
        return trimmed;
    }

    private String normalizeCallbackAesKey(String raw, boolean isInsert, String previous) {
        if (raw == null) {
            return isInsert ? "" : previous;
        }
        String trimmed = raw.trim();
        if (!isInsert && trimmed.isEmpty()) {
            return previous;
        }
        return trimmed;
    }

    private boolean isWecomConfigUnchanged(SysWeworkConfigPO current, SysWeworkConfigPO next) {
        return Objects.equals(trimToEmpty(current.getCorpId()), trimToEmpty(next.getCorpId()))
                && Objects.equals(trimToEmpty(current.getAgentId()), trimToEmpty(next.getAgentId()))
                && Objects.equals(trimToEmpty(current.getCorpSecret()), trimToEmpty(next.getCorpSecret()))
                && Objects.equals(trimToEmpty(current.getApiBaseUrl()), trimToEmpty(next.getApiBaseUrl()))
                && Objects.equals(current.getConnectTimeoutMs(), next.getConnectTimeoutMs())
                && Objects.equals(current.getReadTimeoutMs(), next.getReadTimeoutMs())
                && Objects.equals(current.getScheduleEnabled(), next.getScheduleEnabled())
                && Objects.equals(trimToEmpty(current.getScheduleCron()), trimToEmpty(next.getScheduleCron()))
                && Objects.equals(current.getRetryCount(), next.getRetryCount())
                && Objects.equals(current.getBatchSize(), next.getBatchSize())
                && Objects.equals(current.getStatus(), next.getStatus())
                && Objects.equals(trimToEmpty(current.getCallbackToken()), trimToEmpty(next.getCallbackToken()))
                && Objects.equals(trimToEmpty(current.getCallbackAesKey()), trimToEmpty(next.getCallbackAesKey()));
    }

    private static String trimToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    public WecomConnectionTestOutput testConnection() {
        WecomConnectionTestOutput output = new WecomConnectionTestOutput();
        try {
            List<WecomClient.WecomDepartment> departments = wecomClient.getDepartmentList();
            output.setSuccess(true);
            output.setMessage("连接成功");
            output.setDepartmentCount(departments == null ? 0 : departments.size());
            return output;
        } catch (BusinessException ex) {
            output.setSuccess(false);
            output.setMessage(ex.getMessage());
            output.setDepartmentCount(0);
            return output;
        } catch (Exception ex) {
            throw BusinessException.of(ErrorCode.WECOM_API_ERROR, "连接测试失败: " + ex.getMessage());
        }
    }

    private SysWeworkConfigPO getLatestConfig() {
        return sysWeworkConfigMapper.selectOne(
                new LambdaQueryWrapper<SysWeworkConfigPO>()
                        .orderByDesc(SysWeworkConfigPO::getUpdateTime)
                        .orderByDesc(SysWeworkConfigPO::getId)
                        .last("limit 1")
        );
    }

    private WecomConfigOutput toOutput(SysWeworkConfigPO po) {
        WecomConfigOutput output = new WecomConfigOutput();
        output.setId(po.getId());
        output.setCorpId(po.getCorpId());
        output.setAgentId(po.getAgentId());
        output.setCorpSecretMasked(maskSecret(weworkSecretCodec.decode(po.getCorpSecret())));
        output.setApiBaseUrl(po.getApiBaseUrl());
        output.setConnectTimeoutMs(po.getConnectTimeoutMs());
        output.setReadTimeoutMs(po.getReadTimeoutMs());
        output.setScheduleEnabled(po.getScheduleEnabled() != null && po.getScheduleEnabled() == 1);
        output.setScheduleCron(po.getScheduleCron());
        output.setRetryCount(po.getRetryCount());
        output.setBatchSize(po.getBatchSize());
        output.setEnabled(po.getStatus() == null || po.getStatus() == 1);
        output.setUpdateTime(po.getUpdateTime());
        output.setCallbackTokenMasked(maskSecret(po.getCallbackToken()));
        output.setCallbackAesKeyMasked(maskSecret(po.getCallbackAesKey()));
        return output;
    }

    private String maskSecret(String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            return "";
        }
        String value = secret.trim();
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
