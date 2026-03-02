package com.miduo.cloud.ticket.application.auth;

import com.miduo.cloud.ticket.application.common.BaseApplicationService;
import com.miduo.cloud.ticket.common.enums.ErrorCode;
import com.miduo.cloud.ticket.common.exception.BusinessException;
import com.miduo.cloud.ticket.domain.user.model.Department;
import com.miduo.cloud.ticket.domain.user.model.User;
import com.miduo.cloud.ticket.domain.user.repository.DepartmentRepository;
import com.miduo.cloud.ticket.domain.user.repository.UserRepository;
import com.miduo.cloud.ticket.entity.dto.auth.LoginOutput;
import com.miduo.cloud.ticket.entity.dto.auth.RefreshTokenInput;
import com.miduo.cloud.ticket.entity.dto.auth.WecomLoginInput;
import com.miduo.cloud.ticket.infrastructure.external.wework.WecomClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 认证应用服务
 * 处理企微登录、Token刷新等认证业务
 */
@Service
public class AuthApplicationService extends BaseApplicationService {

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
