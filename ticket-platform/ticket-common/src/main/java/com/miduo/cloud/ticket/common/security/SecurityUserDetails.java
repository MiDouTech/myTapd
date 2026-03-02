package com.miduo.cloud.ticket.common.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security 用户详情
 * 存放登录用户的上下文信息
 */
@Getter
public class SecurityUserDetails implements UserDetails {

    private final Long userId;
    private final String name;
    private final String wecomUserid;
    private final List<String> roleCodes;
    private final Collection<? extends GrantedAuthority> authorities;

    public SecurityUserDetails(Long userId, String name, String wecomUserid,
                               List<String> roleCodes,
                               Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.name = name;
        this.wecomUserid = wecomUserid;
        this.roleCodes = roleCodes;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return name;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
