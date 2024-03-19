package com.threlease.base.utils.jsonwebtoken;

import com.threlease.base.functions.auth.AuthAccount;
import com.threlease.base.functions.auth.AuthService;
import com.threlease.base.utils.Hash;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthAccount authAccount;

    @Override
    public UserDetails loadUserByUsername(String subject) throws UsernameNotFoundException {
        if (!Objects.equals(
                new Hash().base64_encode(
                        authAccount.id + ":" + authAccount.password + authAccount.secret + ":ROLE_ADMIN"
                ), subject)
        ) {
            throw new UsernameNotFoundException("올바르지 않은 세션 입니다.");
        }

        return new CustomUserDetails(authAccount.id + ":" + authAccount.password + ":" + "ROLE_ADMIN");
    }
}
