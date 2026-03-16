package com.miduo.cloud.ticket.application.auth;

import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.domain.user.model.Department;
import com.miduo.cloud.ticket.domain.user.model.User;
import com.miduo.cloud.ticket.domain.user.repository.DepartmentRepository;
import com.miduo.cloud.ticket.domain.user.repository.UserRepository;
import com.miduo.cloud.ticket.entity.dto.auth.DevLoginInput;
import com.miduo.cloud.ticket.entity.dto.auth.LoginOutput;
import com.miduo.cloud.ticket.entity.dto.auth.RefreshTokenInput;
import com.miduo.cloud.ticket.entity.dto.auth.WecomLoginInput;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 认证应用服务
 * 处理企微登录、Token刷新等认证业务
 */
@Service
public class AuthApplicationService extends BaseApplicationService {

    private static final String DEV_USERNAME = "admin";
    private static final String DEV_PASSWORD = "admin2026";
    private static final String PHONE_LOGIN_PASSWORD = "admin123";

    @Value("${dev-login.enabled:false}")
    private boolean devLoginEnabled;

    private final WecomClient wecomClient;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TokenService tokenService;

    public AuthApplicationService(WecomClient wecomClient,
                                  UserRepository userRepository,
                                  DepartmentRepository departmentRepository,
                                  TokenService tokenService) {
        this.wecomClient = wecomClient;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.tokenService = tokenService;
    }

    /**
     * 企微扫码登录
     */
    @Transactional
    public LoginOutput wecomLogin(WecomLoginInput input) {
        WecomClient.WecomUserIdentity identity = wecomClient.getUserInfoByCode(input.getCode());
        if (identity == null || identity.getUserId() == null || identity.getUserId().isEmpty()) {
            throw BusinessException.of(ErrorCode.WECOM_AUTH_FAILED, "无法获取企微用户身份");
        }

        String wecomUserId = identity.getUserId();
        log.info("企微登录: wecomUserId={}", wecomUserId);

        User user = userRepository.findByWecomUserid(wecomUserId);

        if (user == null) {
            user = autoCreateUser(wecomUserId);
        }

        if (user.getAccountStatus() != null && user.getAccountStatus() == 2) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        List<String> roleCodes = userRepository.findRoleCodes(user.getId());
        user.setRoleCodes(roleCodes);

        return buildLoginOutput(user);
    }

    /**
     * 刷新Token
     */
    public LoginOutput refreshToken(RefreshTokenInput input) {
        Long userId = tokenService.validateRefreshToken(input.getRefreshToken());
        if (userId == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED, "RefreshToken无效或已过期");
        }

        User user = userRepository.findById(userId);
        if (user == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }
        if (user.getAccountStatus() != null && user.getAccountStatus() == 2) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        List<String> roleCodes = userRepository.findRoleCodes(user.getId());
        user.setRoleCodes(roleCodes);

        return buildLoginOutput(user);
    }

    /**
     * 临时登录（仅 dev-login.enabled=true 时可用）
     * 支持：admin/admin2026 或 手机号/admin123
     * 接口编号：API000402
     */
    @Transactional
    public LoginOutput devLogin(DevLoginInput input) {
        if (!devLoginEnabled) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "当前环境不允许使用测试登录");
        }

        // 管理员账号登录
        if (DEV_USERNAME.equals(input.getUsername())) {
            if (!DEV_PASSWORD.equals(input.getPassword())) {
                throw BusinessException.of(ErrorCode.UNAUTHORIZED, "账号或密码错误");
            }
            return loginAsDevAdmin();
        }

        // 手机号登录
        if (PHONE_LOGIN_PASSWORD.equals(input.getPassword())) {
            return loginByPhone(input.getUsername());
        }

        throw BusinessException.of(ErrorCode.UNAUTHORIZED, "账号或密码错误");
    }

    /**
     * admin测试账号登录
     */
    private LoginOutput loginAsDevAdmin() {
        User user = userRepository.findByWecomUserid("DEV_ADMIN");
        if (user == null) {
            user = new User();
            user.setName("测试管理员");
            user.setWecomUserid("DEV_ADMIN");
            user.setAccountStatus(1);
            user = userRepository.save(user);
            userRepository.assignRole(user.getId(), 1L);
            log.info("自动创建测试管理员账号: id={}", user.getId());
        }

        if (user.getAccountStatus() != null && user.getAccountStatus() == 2) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        if (!Integer.valueOf(1).equals(user.getAccountStatus())) {
            log.warn("测试管理员账号状态异常(status={})，自动恢复为激活状态", user.getAccountStatus());
            user.setAccountStatus(1);
            user = userRepository.save(user);
        }

        List<String> roleCodes = userRepository.findRoleCodes(user.getId());
        user.setRoleCodes(roleCodes);
        return buildLoginOutput(user);
    }

    /**
     * 手机号临时登录
     */
    private LoginOutput loginByPhone(String phone) {
        User user = userRepository.findByPhone(phone);
        if (user == null) {
            throw BusinessException.of(ErrorCode.DATA_NOT_FOUND, "手机号未注册，请先在系统中创建账号");
        }

        if (user.getAccountStatus() != null && user.getAccountStatus() == 2) {
            throw BusinessException.of(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        List<String> roleCodes = userRepository.findRoleCodes(user.getId());
        user.setRoleCodes(roleCodes);
        log.info("手机号临时登录成功: userId={}, name={}, phone={}", user.getId(), user.getName(), phone);
        return buildLoginOutput(user);
    }

    /**
     * 首次登录自动创建用户
     */
    private User autoCreateUser(String wecomUserId) {
        log.info("首次登录，自动创建用户: wecomUserId={}", wecomUserId);
        WecomClient.WecomUserDetail detail = wecomClient.getUserDetail(wecomUserId);

        User user = new User();
        user.setName(detail.getName());
        user.setPhone(detail.getMobile());
        user.setEmail(detail.getEmail());
        user.setPosition(detail.getPosition());
        user.setAvatarUrl(detail.getAvatar());
        user.setWecomUserid(wecomUserId);
        user.setAccountStatus(detail.getStatus() != null && detail.getStatus() == 1 ? 1 : 4);

        if (detail.getMainDepartment() != null) {
            Department dept = departmentRepository.findByWecomDeptId(detail.getMainDepartment());
            if (dept != null) {
                user.setDepartmentId(dept.getId());
            }
        }

        user = userRepository.save(user);

        userRepository.assignRole(user.getId(), 4L);
        log.info("自动创建用户成功: id={}, name={}", user.getId(), user.getName());

        return user;
    }

    private LoginOutput buildLoginOutput(User user) {
        String accessToken = tokenService.createAccessToken(user.getId(), user.getName(), user.getRoleCodes());
        String refreshToken = tokenService.createRefreshToken(user.getId());
        long expiresIn = tokenService.getAccessTokenExpireSeconds();

        String deptName = null;
        if (user.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(user.getDepartmentId());
            if (dept != null) {
                deptName = dept.getName();
            }
        }

        LoginOutput output = new LoginOutput();
        output.setAccessToken(accessToken);
        output.setRefreshToken(refreshToken);
        output.setExpiresIn(expiresIn);

        LoginOutput.UserInfo userInfo = new LoginOutput.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setName(user.getName());
        userInfo.setAvatar(user.getAvatarUrl());
        userInfo.setDepartment(deptName);
        userInfo.setRoles(user.getRoleCodes());
        output.setUserInfo(userInfo);

        return output;
    }
}
