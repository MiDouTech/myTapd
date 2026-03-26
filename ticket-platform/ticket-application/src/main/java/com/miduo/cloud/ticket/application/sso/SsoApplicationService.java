package com.miduo.cloud.ticket.application.sso;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.miduo.cloud.ticket.application.auth.TokenService;
import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.application.operationlog.OperationLogApplicationService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.domain.user.model.User;
import com.miduo.cloud.ticket.domain.user.repository.UserRepository;
import com.miduo.cloud.ticket.entity.dto.auth.LoginOutput;
import com.miduo.cloud.ticket.entity.dto.sso.SsoCallbackInput;
import com.miduo.cloud.ticket.entity.dto.sso.SsoStatusOutput;
import com.miduo.cloud.ticket.infrastructure.external.sso.MiduoSsoClient;
import com.miduo.cloud.ticket.infrastructure.external.sso.MiduoSsoProperties;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sso.mapper.SsoSessionMapper;
import com.miduo.cloud.ticket.infrastructure.persistence.mybatis.sso.po.SsoSessionPO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * SSO 应用服务
 * 处理米多星球 SSO 登录、登出、会话续期、登录桥等业务
 */
@Service
public class SsoApplicationService extends BaseApplicationService {

    private static final String SSO_STATE_PREFIX = "sso:state:";
    private static final long SSO_STATE_TTL_SECONDS = 600;
    private static final long SESSION_TOKEN_TTL_SECONDS = 28800;

    private static final String PATH_SSO_CALLBACK = "/api/auth/sso/callback";
    private static final String PATH_SSO_LOGOUT = "/api/auth/sso/logout";

    private final MiduoSsoClient ssoClient;
    private final MiduoSsoProperties ssoProperties;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final SsoSessionMapper ssoSessionMapper;
    private final StringRedisTemplate redisTemplate;
    private final OperationLogApplicationService operationLogService;

    public SsoApplicationService(MiduoSsoClient ssoClient,
                                 MiduoSsoProperties ssoProperties,
                                 UserRepository userRepository,
                                 TokenService tokenService,
                                 SsoSessionMapper ssoSessionMapper,
                                 StringRedisTemplate redisTemplate,
                                 OperationLogApplicationService operationLogService) {
        this.ssoClient = ssoClient;
        this.ssoProperties = ssoProperties;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.ssoSessionMapper = ssoSessionMapper;
        this.redisTemplate = redisTemplate;
        this.operationLogService = operationLogService;
    }

    /**
     * 获取 SSO 状态信息
     */
    public SsoStatusOutput getSsoStatus() {
        SsoStatusOutput output = new SsoStatusOutput();
        output.setEnabled(ssoProperties.isEnabled());
        output.setAppCode(ssoProperties.getAppCode());
        output.setCallbackUrl(ssoProperties.getRedirectUri());
        return output;
    }

    /**
     * 生成 state 参数（CSRF 防护）
     */
    public String generateState() {
        String state = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
                SSO_STATE_PREFIX + state, "1", SSO_STATE_TTL_SECONDS, TimeUnit.SECONDS);
        return state;
    }

    /**
     * SSO 回调处理：校验 exchange token → 匹配/创建本地用户 → 发放 JWT
     */
    @Transactional
    public LoginOutput ssoCallback(SsoCallbackInput input, String operatorIp, String userAgent) {
        if (!ssoProperties.isEnabled()) {
            throw BusinessException.of(ErrorCode.SSO_DISABLED);
        }

        if (input.getState() != null && !input.getState().isEmpty()) {
            String stateKey = SSO_STATE_PREFIX + input.getState();
            Boolean consumed = redisTemplate.delete(stateKey);
            if (consumed == null || !consumed) {
                log.warn("SSO state 校验失败: state={}", input.getState());
                throw BusinessException.of(ErrorCode.SSO_STATE_INVALID, "state 参数无效或已过期");
            }
        }

        try {
            MiduoSsoClient.ValidateResult validateResult = ssoClient.validateLoginToken(input.getToken());

            User user = matchOrCreateUser(validateResult);

            List<String> roleCodes = userRepository.findRoleCodes(user.getId());
            user.setRoleCodes(roleCodes);

            saveSsoSession(user.getId(), validateResult);

            LoginOutput output = buildLoginOutput(user);

            operationLogService.saveLoginLog(user.getId(), user.getName(), operatorIp, userAgent,
                    PATH_SSO_CALLBACK, "POST", "米多SSO登录", true, null);

            log.info("SSO登录成功: userId={}, name={}, miduoUserId={}",
                    user.getId(), user.getName(), validateResult.getUserId());
            return output;

        } catch (BusinessException e) {
            operationLogService.saveLoginLog(null, "", operatorIp, userAgent,
                    PATH_SSO_CALLBACK, "POST", "米多SSO登录", false, e.getMessage());
            throw e;
        } catch (Exception e) {
            operationLogService.saveLoginLog(null, "", operatorIp, userAgent,
                    PATH_SSO_CALLBACK, "POST", "米多SSO登录", false, e.getMessage());
            throw e;
        }
    }

    /**
     * SSO 登出：吊销米多 sessionToken + 清理本地会话记录
     */
    @Transactional
    public void ssoLogout(Long userId, String operatorIp, String userAgent) {
        LambdaQueryWrapper<SsoSessionPO> wrapper = new LambdaQueryWrapper<SsoSessionPO>()
                .eq(SsoSessionPO::getUserId, userId)
                .eq(SsoSessionPO::getRevoked, 0)
                .orderByDesc(SsoSessionPO::getCreateTime)
                .last("LIMIT 1");

        SsoSessionPO session = ssoSessionMapper.selectOne(wrapper);
        if (session != null) {
            ssoClient.revokeSessionToken(session.getSessionToken());
            session.setRevoked(1);
            session.setUpdateTime(new Date());
            ssoSessionMapper.updateById(session);
            log.info("SSO登出: userId={}, sessionId={}", userId, session.getId());
        }

        operationLogService.saveLoginLog(userId, "", operatorIp, userAgent,
                PATH_SSO_LOGOUT, "POST", "米多SSO登出", true, null);
    }

    /**
     * 获取登录桥 URL（会话失效时重新从米多获取身份）
     */
    public String getBridgeUrl() {
        if (!ssoProperties.isEnabled()) {
            throw BusinessException.of(ErrorCode.SSO_DISABLED);
        }
        String state = generateState();
        return ssoClient.buildBridgeUrl(state);
    }

    /**
     * 通过手机号/工号/邮箱匹配本地用户，匹配不到则自动创建
     */
    private User matchOrCreateUser(MiduoSsoClient.ValidateResult validateResult) {
        User user = null;

        if (validateResult.getMobile() != null && !validateResult.getMobile().isEmpty()) {
            user = userRepository.findByPhone(validateResult.getMobile());
        }

        if (user == null && validateResult.getEmployeeNo() != null && !validateResult.getEmployeeNo().isEmpty()) {
            List<User> allUsers = userRepository.findAllActive();
            for (User u : allUsers) {
                if (validateResult.getEmployeeNo().equals(u.getEmployeeNo())) {
                    user = u;
                    break;
                }
            }
        }

        if (user == null) {
            log.info("SSO登录自动创建用户: miduoUserId={}, name={}, mobile={}",
                    validateResult.getUserId(), validateResult.getUserName(), validateResult.getMobile());
            user = new User();
            user.setName(validateResult.getUserName() != null ? validateResult.getUserName() : "SSO用户");
            user.setPhone(validateResult.getMobile());
            user.setEmail(validateResult.getEmail());
            user.setEmployeeNo(validateResult.getEmployeeNo());
            user.setAccountStatus(1);
            user.setWecomUserid("SSO_" + validateResult.getUserId());
            user = userRepository.save(user);
            userRepository.assignRole(user.getId(), 4L);
            log.info("SSO自动创建用户成功: id={}, name={}", user.getId(), user.getName());
        }

        if (user.getAccountStatus() != null && user.getAccountStatus() == 2) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        return user;
    }

    private void saveSsoSession(Long userId, MiduoSsoClient.ValidateResult validateResult) {
        SsoSessionPO session = new SsoSessionPO();
        session.setUserId(userId);
        session.setSessionToken(validateResult.getSessionToken());
        session.setMiduoUserId(validateResult.getUserId());
        session.setMiduoUserName(validateResult.getUserName());
        session.setMiduoMobile(validateResult.getMobile());
        session.setMiduoEmail(validateResult.getEmail());
        session.setMiduoEmployeeNo(validateResult.getEmployeeNo());
        session.setExpireTime(new Date(System.currentTimeMillis() + SESSION_TOKEN_TTL_SECONDS * 1000));
        session.setRevoked(0);
        session.setCreateTime(new Date());
        session.setUpdateTime(new Date());
        ssoSessionMapper.insert(session);
    }

    private LoginOutput buildLoginOutput(User user) {
        String accessToken = tokenService.createAccessToken(user.getId(), user.getName(), user.getRoleCodes());
        String refreshToken = tokenService.createRefreshToken(user.getId());
        long expiresIn = tokenService.getAccessTokenExpireSeconds();

        LoginOutput output = new LoginOutput();
        output.setAccessToken(accessToken);
        output.setRefreshToken(refreshToken);
        output.setExpiresIn(expiresIn);

        LoginOutput.UserInfo userInfo = new LoginOutput.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setName(user.getName());
        userInfo.setAvatar(user.getAvatarUrl());
        userInfo.setRoles(user.getRoleCodes());
        output.setUserInfo(userInfo);

        return output;
    }
}
