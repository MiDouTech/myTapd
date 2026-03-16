package com.miduo.cloud.ticket.controller.auth;

import com.miduo.cloud.ticket.application.auth.TokenService;
import com.miduo.cloud.ticket.common.dto.common.ApiResult;
import com.miduo.cloud.ticket.domain.user.model.User;
import com.miduo.cloud.ticket.domain.user.repository.UserRepository;
import com.miduo.cloud.ticket.entity.dto.auth.LoginOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 测试环境专用登录接口
 * 接口编号：API000402
 * 仅在 spring.profiles.active=test 时生效，生产环境不会加载此 Controller
 */
@Profile("test")
@Tag(name = "测试专用", description = "测试环境直接生成Token，无需企微授权")
@RestController
@RequestMapping("/api/auth/test")
public class TestAuthController {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    public TestAuthController(UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    /**
     * 查询所有可用用户列表（用于选择测试账号）
     * 接口编号：API000402
     */
    @Operation(summary = "[TEST] 查询用户列表", description = "仅测试环境可用，返回所有用户供选择")
    @GetMapping("/users")
    public ApiResult<List<UserItem>> listUsers() {
        List<User> users = userRepository.findAll();
        List<UserItem> items = users.stream().map(u -> {
            UserItem item = new UserItem();
            item.setId(u.getId());
            item.setName(u.getName());
            item.setWecomUserId(u.getWecomUserid());
            return item;
        }).collect(Collectors.toList());
        return ApiResult.success(items);
    }

    /**
     * 直接为指定用户生成 Token（跳过企微授权）
     * 接口编号：API000403
     */
    @Operation(summary = "[TEST] 直接生成Token", description = "仅测试环境可用，传入userId直接获取Token")
    @PostMapping("/token/{userId}")
    public ApiResult<LoginOutput> generateToken(@PathVariable Long userId) {
        User user = userRepository.findById(userId);
        if (user == null) {
            return ApiResult.error(404, "用户不存在，请先调用 /api/auth/test/users 查询可用用户ID");
        }

        List<String> roleCodes = userRepository.findRoleCodes(userId);

        String accessToken = tokenService.createAccessToken(user.getId(), user.getName(), roleCodes);
        String refreshToken = tokenService.createRefreshToken(user.getId());

        LoginOutput output = new LoginOutput();
        output.setAccessToken(accessToken);
        output.setRefreshToken(refreshToken);
        output.setExpiresIn(tokenService.getAccessTokenExpireSeconds());

        LoginOutput.UserInfo userInfo = new LoginOutput.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setName(user.getName());
        userInfo.setAvatar(user.getAvatarUrl());
        userInfo.setRoles(roleCodes);
        output.setUserInfo(userInfo);

        return ApiResult.success(output);
    }

    public static class UserItem {
        private Long id;
        private String name;
        private String wecomUserId;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getWecomUserId() { return wecomUserId; }
        public void setWecomUserId(String wecomUserId) { this.wecomUserId = wecomUserId; }
    }
}
