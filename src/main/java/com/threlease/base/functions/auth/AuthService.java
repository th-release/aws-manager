package com.threlease.base.functions.auth;

import com.threlease.base.entites.AuthEntity;
import com.threlease.base.repositories.AuthRepository;
import com.threlease.base.utils.jsonwebtoken.JwtProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.stereotype.Service;

import java.util.Optional;
@Service
public class AuthService {
    private final AuthRepository authRepository;
    private final JwtProvider jwtProvider;

    public AuthService(AuthRepository authRepository, JwtProvider jwtProvider) {
        this.authRepository = authRepository;
        this.jwtProvider = jwtProvider;
    }

    public void authSave(AuthEntity data) {
        authRepository.save(data);
    }

    public void authRemove(AuthEntity data) {
        authRepository.delete(data);
    }

    public Optional<AuthEntity> findOneByUuid(String uuid) {
        return authRepository.findOneByUUID(uuid);
    }

    public Optional<AuthEntity> findOneByUsername(String username) {
        return authRepository.findOneByUsername(username);
    }

    public String sign(String input) {
        return jwtProvider.sign(input);
    }

    public Optional<Jws<Claims>> verify(String token) {
        return jwtProvider.verify(token);
    }
    public boolean validateToken(String token) {
        return jwtProvider.validateToken(token);
    }
}
