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
        SysWeworkConfigPO po = existing == null ? new SysWeworkConfigPO() : existing;

        po.setCorpId(input.getCorpId().trim());
        po.setAgentId(input.getAgentId().trim());
        po.setCorpSecret(weworkSecretCodec.encode(input.getCorpSecret().trim()));
        po.setApiBaseUrl(input.getApiBaseUrl().trim());
        po.setConnectTimeoutMs(input.getConnectTimeoutMs());
        po.setReadTimeoutMs(input.getReadTimeoutMs());
        po.setScheduleEnabled(Boolean.TRUE.equals(input.getScheduleEnabled()) ? 1 : 0);
        po.setScheduleCron(input.getScheduleCron());
        po.setRetryCount(input.getRetryCount());
        po.setBatchSize(input.getBatchSize());
        po.setStatus(Boolean.FALSE.equals(input.getEnabled()) ? 0 : 1);
        po.setCallbackToken(input.getCallbackToken() != null ? input.getCallbackToken().trim() : "");
        po.setCallbackAesKey(input.getCallbackAesKey() != null ? input.getCallbackAesKey().trim() : "");

        if (existing == null) {
            sysWeworkConfigMapper.insert(po);
        } else {
            sysWeworkConfigMapper.updateById(po);
        }

        // 配置更新后强制刷新token缓存，避免旧配置继续生效
        wecomTokenManager.refreshAccessToken();
        return po.getId();
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
        output.setCallbackToken(po.getCallbackToken());
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
