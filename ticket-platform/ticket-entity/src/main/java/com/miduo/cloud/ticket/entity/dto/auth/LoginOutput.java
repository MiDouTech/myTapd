package com.miduo.cloud.ticket.entity.dto.auth;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录响应
 */
@Data
public class LoginOutput implements Serializable {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private UserInfo userInfo;

    @Data
    public static class UserInfo implements Serializable {
        private Long id;
        private String name;
        private String avatar;
        private String department;
        private List<String> roles;
    }
}
