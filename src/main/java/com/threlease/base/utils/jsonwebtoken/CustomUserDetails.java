package com.threlease.base.utils.jsonwebtoken;

import com.threlease.base.functions.auth.Roles;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {
    private String auth;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(auth.split(":")[2]));
    }

    @Override
    public String getPassword() {
        return auth.split(":")[1];
    }

    @Override
    public String getUsername() {
        return auth.split(":")[0];
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

